import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLOutput;
import java.util.*;
import java.util.stream.Stream;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class Main {
    public static void main(String[] args) {
        boolean verbose = args.length>0 && args[0].equals("-v");
        Map map = Map.getInstance();
        Map newMap = Map.getNewMap();
        File gsFile = null;
        Player[] players = new Player[] {
                new Player("AUSTRIA"),
                new Player("ENGLAND"),
                new Player("FRANCE"),
                new Player("GERMANY"),
                new Player("ITALY"),
                new Player("RUSSIA"),
                new Player("TURKEY")

        };
        // Load game state:
        for(Player p : players) {
            try (Stream<Path> stream = Files.list(Paths.get("./current"))){
                List<Path> files = stream.filter(Files::isRegularFile).toList();
                switch(files.size()) {
                    case 0 -> throw new FileNotFoundException();
                    case 1 -> {
                       gsFile = files.get(0).toFile();
                       p.loadGameState(gsFile);
                    }
                    default -> throw new IllegalStateException("Too many gamestates found");
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        compileOrders(players, verbose);
        printGameState(players);
        System.out.println("\nTime to write your retreat orders!");
        System.out.println("XXX");
        System.out.flush();

        // Wait to receive cue to compile retreat orders
        Scanner sc = new Scanner(System.in);
        while(true) {
            if(sc.hasNextLine() && sc.nextLine().equals("GO")) {
                compileRetreatOrders(players, verbose);
                printGameState(players);
                break;
            }
        }
        if(gsFile.getName().charAt(0) == 'F') { //Spring turn logic
            System.out.println("Spring turn over");
            System.out.println("XXX");
            System.out.flush();
            try (BufferedWriter writer = new BufferedWriter(new FileWriter("current/S"+(Integer.parseInt(gsFile.getName().substring(1,5))+1)+".gs"))){
                for(Player p: players) {
                    writer.write(p.getNation()+"\n");
                    //for(Territory t: newMap.getAllTerritories()) System.out.println((t.getOwner() != null) ? t.getOwner().getNation() : "nill");
                    for(Territory t: map.getTerritoriesOwnedBy(p)) {
                        writer.write(t.getName()+",");
                    }
                    writer.write("\n");
                    for(Unit u: p.getUnits()) {
                        writer.write(u.toString()+"\n");
                    }
                    writer.write("\n");
                }
                Files.move(Paths.get(gsFile.getPath()), Paths.get("archive/", gsFile.getName()), REPLACE_EXISTING);
            } catch(Exception e) {
                System.out.println(e);
            }
        } else { // Fall turn logic
            newMap.updateOwnership();
            for(Player p: players) {
                System.out.println(p.getNation()+": "+p.getNumSupplyPoints(newMap));
                if(p.getNumSupplyPoints(newMap) >= 18) {
                    System.out.println(p.getNation()+" wins!");
                    return;
                }
                if(p.getNumSupplyPoints(newMap) > p.getUnits().size()) {
                    System.out.println(p.getNation()+" can build "+(p.getNumSupplyPoints(newMap)-p.getUnits().size())+" units");
                } else if(p.getNumSupplyPoints(newMap) < p.getUnits().size()) {
                    System.out.println(p.getNation()+" must disband "+(p.getUnits().size()-p.getNumSupplyPoints(newMap))+" units");
                }
            }
            System.out.println("XXX");
            System.out.flush();
            while(true) {
                if(sc.hasNextLine() && sc.nextLine().equals("GO")) {
                    break;
                }
            }
            compileBuildOrders(players, verbose);
            printGameState(players);
            System.out.println("End fall turn");
            System.out.println("XXX");
            System.out.flush();
            // Write game state to file before program close
            try (BufferedWriter writer = new BufferedWriter(new FileWriter("current/F"+(Integer.parseInt(gsFile.getName().substring(1,5)))+".gs"))){
                for(Player p: players) {
                    writer.write(p.getNation()+"\n");
                    for(Territory t: newMap.getTerritoriesOwnedBy(p)) {
                        writer.write(t.getName()+",");
                    }
                    writer.write("\n");
                    for(Unit u: p.getUnits()) {
                        writer.write(u.toString()+"\n");
                    }
                    writer.write("\n");
                }
                Files.move(Paths.get(gsFile.getPath()), Paths.get("archive/", gsFile.getName()), REPLACE_EXISTING);
            } catch(Exception ignored) {
            }
        }
    }

    public static void compileOrders(Player[] players, boolean verbose) {
        Map newMap = Map.getNewMap();
        Queue<Order> orderQueue = new LinkedList<>();
        for(Player p: players) {
            try {
                p.loadOrders();
                orderQueue.addAll(p.getOrders());
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
        // Iterate through all orders in the queue until all have been solved
        // x tracks the number of times the entire queue has been navigated
        // x = 0-1: all support cuts are finalized
        // x = 2: all uncut supports go through
        // x = 3: dislodgements are finalized
        // x >= 4: convoy orders are finalized
        outer:
        for(int x = 0, k=-1; !orderQueue.isEmpty(); k++) {
            if(k>=orderQueue.size()) {
                k=0;
                x++;
                if(verbose) System.out.println("x = "+x+" -----------------------------------------");
            }
            Order o = orderQueue.remove();
            Unit u = o.getUnit();
            if(u.getOwner() == null) {
                continue;
                // Unit being ordered does not exist, so remove order from queue
            }
            if(verbose) System.out.println(o);
            //execute logic based on the type of order
            switch(o) {
                case MoveOrder mo -> {
                    //check order validity:
                    if(!mo.isValidated()) {
                        if(!Arrays.asList(u.getLocation().getBorders1()).contains(mo.getMoveTo())) {
                            if(u instanceof Fleet) {
                                if(verbose) System.out.println("- Order invalidated: "+mo+". Replacing with hold order.");
                                orderQueue.add(new Order(u)); //Add hold order into queue to replace invalid move order
                                continue;
                            }
                            //If the else clause is reached, u must be an army, so check it's borders2 for the target territory
                            else if(!Arrays.asList(u.getLocation().getBorders2()).contains(mo.getMoveTo())) {
                                if(x>=5) { //Convoys are finished resolving at x=5
                                    //Replaced invalid convoy attempt with hold
                                    orderQueue.add(new Order(u));
                                    if(verbose) System.out.println("- Order \'teleports\'. Invalidating.");
                                    continue;
                                } else {
                                    if(verbose) System.out.println("- Order is convoy-only");
                                    mo.setConvoyOnly();
                                    orderQueue.add(mo);
                                    continue;
                                }
                            }
                        } else {
                            if(verbose) System.out.println("- Valid move order");
                            mo.setValid();
                        }
                    }
                    //Cut support orders
                    if(!mo.getMoveTo().isEmpty()) {
                        for(int i = 0; i<orderQueue.size(); i++, orderQueue.add(orderQueue.remove())) {
                            Order top = orderQueue.peek();
                            if(top instanceof SupportOrder && top.getUnit().getLocation().equals(mo.getMoveTo()) ) {
                                //check for rules edge case where a unit cannot cut support to another unit attacking the original unit
                                if(((SupportOrder) top).getOrderSupported() instanceof MoveOrder
                                && ((MoveOrder) ((SupportOrder) top).getOrderSupported()).getMoveTo().equals(u.getLocation())) {
                                    //replace this (mo) order with a hold
                                    orderQueue.add(new Order(u));
                                    if(verbose) System.out.println("- \'"+mo+"\' is an illegal support cut.");
                                    break;
                                }
                                //Remove cut support order and replace with a hold
                                if(verbose) System.out.println("- \'"+top+"\' cut by \'"+mo+"\'");
                                orderQueue.add(new Order(orderQueue.remove().getUnit()));
                                for(;i<orderQueue.size();i++) orderQueue.add(orderQueue.remove());
                                break;
                            }
                        }
                    }
                    boolean uncontested = true;
                    List<Order> collisions = new ArrayList<>();
                    //Iterate through the entire queue looking for collisions
                    for (int i = 0; i < orderQueue.size(); i++, orderQueue.add(orderQueue.remove())) {
                        //Check if there are any other move orders that could collide with this order
                        if ((orderQueue.peek() instanceof MoveOrder && ((MoveOrder) orderQueue.peek()).getMoveTo().equals(mo.getMoveTo()))
                        || (orderQueue.peek().getClass() == Order.class && orderQueue.peek().getUnit().getLocation().equals(mo.getMoveTo()))) {
                            uncontested = false;
                            collisions.add(orderQueue.peek());
                            if(verbose) System.out.println("- Collision found: " + orderQueue.peek());
                            if(verbose) System.out.println("   - "+mo.getStrength()+" str vs. "+orderQueue.peek().getStrength()+" str");
                        }
                        // Check for 'ring around the rosy' case
                        else if(orderQueue.peek() instanceof MoveOrder && orderQueue.peek().getUnit().getLocation().equals(mo.getMoveTo()) && x<=4) {
                            if(x>=3 && ((MoveOrder) orderQueue.peek()).getMoveTo().equals(u.getLocation()) && orderQueue.peek().getStrength() == mo.getStrength()) {
                                if(verbose) System.out.println("- Units cannot trade places without the use of a convoy. Replacing orders with holds.");
                                orderQueue.add(new Order(orderQueue.remove().getUnit()));
                                orderQueue.add(new Order(u));
                                for(; i<orderQueue.size(); i++) orderQueue.add(orderQueue.remove()); //Prevents program from stalling on this collision case
                                continue outer;
                            } else if(mo.getStrength() > orderQueue.peek().getStrength()) {
                                if(verbose) System.out.println("- "+orderQueue.peek()+" dislodged");
                                orderQueue.add(new Order(orderQueue.remove().getUnit()));
                                continue;
                            }
                            //add order back into queue for re-assessment later
                            for(; i<orderQueue.size(); i++) orderQueue.add(orderQueue.remove()); //Prevents program from stalling on this collision case
                            orderQueue.add(mo);
                            if(verbose) System.out.println("- 'Ring around the rosy'. Cannot be calculated now, adding back to queue");
                            continue outer;
                        }
                    }
                    if(mo.getStrength() == -1) {
                        //Move orders that loose standoffs become holds
                        orderQueue.add(new Order(u));
                        if(verbose) System.out.println("- \'" + mo + "\' bounces");
                        continue outer;
                    }
                    //uncontested case
                    if(uncontested && newMap.getTerritory(mo.getMoveTo().getName()).isEmpty()) {
                        //order succeeds. Note that the order is not added back to the queue, because it's finished processing
                        if(verbose) System.out.println("- Uncontested order \'" + mo + "\' succeeds");
                        u.setLocation(newMap.getTerritory(mo.getMoveTo().getName()));
                        u.getLocation().setOccupyingUnit(u);
                        k--;
                        continue;
                    //resolve standoffs
                    } else if (x >= 3 && mo.isValidated()) {
                        boolean lostStandoff = false;
                        for (Order collision : collisions) {
                            if (mo.getStrength() <= collision.getStrength()) {
                                //Move orders that loose standoffs become holds
                                lostStandoff = true;
                                if(verbose) System.out.println("- \'" + mo + "\' bounces");
                            }
                            if (mo.getStrength() >= collision.getStrength()) {
                                collision.setBounce();
                                if(verbose) System.out.println("- \'"+collision+"\' will bounce");
                            }
                        }
                        if (!lostStandoff) {
                            if(verbose) System.out.println("- \'" + mo + "\' wins standoff");
                            u.setLocation(newMap.getTerritory(mo.getMoveTo().getName()));
                            u.getLocation().setOccupyingUnit(u);
                            k--;
                            continue;
                        } else {
                            orderQueue.add(new Order(u));
                        }
                    } else if(x<=10){
                        //add order back into queue for re-assessment later
                        orderQueue.add(mo);
                        if(verbose) System.out.println("- Cannot be calculated now, adding back to queue");
                    } else {
                        orderQueue.add(new Order(u));
                        if(verbose) System.out.println("- "+mo+" must have failed, replacing with hold");
                    }
                }
                case SupportOrder so -> {
                    //Check validity:
                    if(!so.isValidated()) {
                        for(int i = 0; i < orderQueue.size(); i++, orderQueue.add(orderQueue.remove())) {
                           if(orderQueue.peek().equals(so.getOrderSupported())) {
                               so.setSupport(orderQueue.peek());
                               so.setValid();
                               if(verbose) System.out.println("- Valid support order");
                           }
                        }
                    }
                    //Execute support orders after cuts have been finalized
                    if(x>=2 && so.isValidated()) {
                        if(verbose) System.out.println("- Adding support to "+so.getOrderSupported());
                        so.getOrderSupported().addSupport();
                        u.setLocation(newMap.getTerritory(u.getLocation().getName()));
                        u.getLocation().setOccupyingUnit(u);
                        k--;
                        //Do no add so back to queue, since it's finished executing
                    } else if(x>=2 && !so.isValidated()) {
                        //Replace so with a hold order
                        orderQueue.add(new Order(so.getUnit()));
                        if(verbose) System.out.println("\'"+so+"\' is invalid");
                    } else {
                        //Add so back to queue for further processing
                        orderQueue.add(so);
                        if(verbose) System.out.println("- Adding back to queue for more processing");
                    }
                }
                case ConvoyOrder co -> {
                    //Check validity
                    if(!co.isValidated()) {
                        if(verbose) System.out.println("- Looking for "+co.getConvoyedMovement());
                        //Convoys MUST convoy an army across water
                        if(u.getLocation().getType() != Territory.Type.SEA || !(co.getConvoyedMovement().getUnit() instanceof  Army)) {
                            orderQueue.add(new Order(u));
                            if(verbose) System.out.println("- Invalid convoy");
                            continue;
                        }
                        for(int i = 0; i <= orderQueue.size()+2; i++, orderQueue.add(orderQueue.remove())) {
                            if(orderQueue.peek().toString().equals(co.getConvoyedMovement().toString())) {
                                co.setConvoyedMovement((MoveOrder) orderQueue.peek());
                                co.setValid();
                                if(verbose) System.out.println("- Valid convoy");
                            }
                        }
                    }
                    if(x<=3) { //Wait until x=4 to resolve convoys
                        orderQueue.add(co);
                        continue;
                    } else if(!co.isValidated()) {
                        orderQueue.add(new Order(u));
                        if(verbose) System.out.println("- \'"+co+"\' is invalid");
                        continue;
                    } else {
                        co.getConvoyedMovement().setValid();
                        u.setLocation(newMap.getTerritory(u.getLocation().getName()));
                        u.getLocation().setOccupyingUnit(u);
                        k--;
                        if(verbose) System.out.println("- convoy \'"+co+"\' successful!");
                    }
                }
                default -> { //Hold order logic:
                    if(x<=4) {
                        orderQueue.add(o);
                    } else if(!newMap.getTerritory(u.getLocation().getName()).isEmpty()){
                        if(verbose) System.out.println("- "+u+" dislodged");
                        u.setLocation(null);
                    } else {
                        u.setLocation(newMap.getTerritory(u.getLocation().getName()));
                        u.getLocation().setOccupyingUnit(u);
                        k--;
                        if(verbose) System.out.println("- \'"+o+"\' successful");
                        continue outer;
                    }
                }
            } //Execute different code depending on the type of order dequeued
        }
    }
    public static void compileRetreatOrders(Player[] players, boolean verbose) {
        Queue<Order> orderQueue = new LinkedList<>();
        for(Player p: players) {
            try {
                p.loadOrders();
                orderQueue.addAll(p.getOrders());
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
        for(int x = 0, k=0; !orderQueue.isEmpty(); k++) {
            if (k >= orderQueue.size()) {
                k = 0;
                x++;
            }
            Order o = orderQueue.remove();
            //System.out.println("Attempting to solve: "+o);
            Unit u = o.getUnit();
            //execute logic based on the type of order
            switch(o) {
                case RetreatOrder ro -> {
                    if(verbose) System.out.println(ro);
                    // Check order legality
                    if (!Arrays.asList(Map.getNewMap().getTerritory(u.getPREVIOUS_LOCATION().getName()).getBorders1()).contains(ro.getRetreatTo())) {
                        if (u instanceof Fleet) {
                            orderQueue.add(new DisbandOrder(u));
                            if(verbose) System.out.println("- Illegal retreat");
                            continue;
                        }
                        //If the else clause is reached, u must be an army, so check it's borders2 for the target territory
                        else if (!Arrays.asList(Map.getNewMap().getTerritory(u.getPREVIOUS_LOCATION().getName()).getBorders2()).contains(ro.getRetreatTo())) {
                            orderQueue.add(new DisbandOrder(u));
                            if(verbose) System.out.println("- Illegal retreat");
                            continue;
                        }
                    }
                    // Check for retreat order collisions
                    boolean collision = false;
                    for (int i = 0; i < orderQueue.size(); i++, orderQueue.add(orderQueue.remove())) {
                        if (orderQueue.peek() instanceof RetreatOrder && ((RetreatOrder) orderQueue.peek()).getRetreatTo().equals(ro.getRetreatTo())) {
                            if(verbose) System.out.println("- Collision with "+orderQueue.peek());
                            orderQueue.add(new DisbandOrder(orderQueue.remove().getUnit()));
                            collision = true;
                        }
                    }
                    if (!collision && ro.getRetreatTo().isEmpty()) {
                        if(verbose) System.out.println("- Valid retreat");
                        u.setLocation(ro.getRetreatTo());
                        u.getLocation().setOccupyingUnit(u);
                    }
                }
                case DisbandOrder diso -> {
                    if(verbose) System.out.println(diso);
                    disbandUnit(diso.getUnit(), players);
                }
                default -> {}
            }
        }
    }
    public static void compileBuildOrders(Player[] players, boolean verbose) {
        for(Player p: players) {
            try {
                p.loadOrders();
            } catch(FileNotFoundException e) {
                throw new RuntimeException(e);
            }
            for(Order o: p.getOrders()) {
                switch(o) {
                    case BuildOrder bo -> {
                        if(verbose) System.out.println(o);
                        if(p.getNumSupplyPoints(Map.getNewMap()) > p.getUnits().size()
                        && bo.getLocation().isEmpty()
                        && bo.getLocation().isSupplyPoint()) {
                            Unit u = o.getUnit();
                            p.addUnit(u);
                            u.setLocation(((BuildOrder) o).getLocation());
                            u.getLocation().setOccupyingUnit(u);
                        } else System.out.println("Condition failed!");
                    }
                    case DisbandOrder diso -> {
                        if(verbose) System.out.println(diso);
                        if(p.getNumSupplyPoints(Map.getNewMap()) < p.getUnits().size()) {
                            disbandUnit(diso.getUnit(), players);
                        }
                    }
                    default -> {}
                }
            }
        }
    }
    public static void printGameState(Player[] players) {
        System.out.println("Current map state:");
        for(Player p: players) {
            System.out.println(p.getNation() + ":");
            for(Unit u: p.getUnits()) {
                if(u.getLocation() == null) {
                    System.out.print((u instanceof Army) ? "A" : "F");
                    System.out.println(" DISLODGED from " + u.getPREVIOUS_LOCATION().getName());
                } else {
                    System.out.println(u);
                }
            }
            System.out.println();
        }
    }
    public static void disbandUnit(Unit u, Player[] players) {
        for(Player p: players) {
            List<Unit> units = p.getUnits();
            for (int i = 0; i < units.size(); i++) {
                if (u == units.get(i)) {
                    units.remove(i);
                    return;
                }
            }
        }
        throw new IllegalArgumentException("Unit not found: "+u);
    }
}