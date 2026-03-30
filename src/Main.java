import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Main {
    public static void main(String[] args) {
        Map map = Map.getInstance();
        Map newMap = Map.getNewMap();
        /*
        try {
            Thread.sleep(1000);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
         */
        /*
        System.out.println("Printing data from a territory on the map:");
        System.out.println(map.getTerritory("YOR"));
        for(Territory t : map.getTerritory("YOR").getBorders1()) {
            System.out.println(t);
        }
         */
        Player[] players = new Player[] {
                new Player("AUSTRIA"),
                new Player("ENGLAND"),
                new Player("FRANCE"),
                new Player("GERMANY"),
                new Player("ITALY"),
                new Player("RUSSIA"),
                new Player("TURKEY")

        };
        for(Player p : players) {
            try (Stream<Path> stream = Files.list(Paths.get("./current"))){
                List<Path> files = stream.filter(Files::isRegularFile).toList();
                switch(files.size()) {
                    case 0 -> throw new FileNotFoundException();
                    case 1 -> p.loadGameState(files.get(0).toFile());
                    default -> throw new IllegalStateException("Too many gamestates found");
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        compileOrders(players);
        System.out.println("Current map state:");
        for(Player p: players) {
            System.out.println(p.getNation() + ":");
            for(Unit u: p.getUnits()) {
                if(u.getLocation() == null) {
                    System.out.println((u instanceof Army) ? "A" : "F");
                    System.out.print(" DISLODGED from " + u.getPREVIOUS_LOCATION().getName());
                } else {
                    System.out.println(u);
                }
            }
            System.out.println();
        }
        System.out.println("XXX");
        System.out.flush();

    }

    public static void compileOrders(Player[] players) {
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
        //Iterate through all orders in the queue until all have been solved
        // x = 0,1: all support cuts are finalized
        // x = 2: all uncut supports go through
        // x = 3: dislodgements are finalized
        // x >= 4: convoy orders are finalized
        for(int x = 0, k=0; !orderQueue.isEmpty(); k++) {
            if(k>=orderQueue.size()) {
                k=0;
                x++;
            }
            Order o = orderQueue.remove();
            //System.out.println("Attempting to solve: "+o);
            Unit u = o.getUnit();
            //execute logic based on the type of order
            switch(o) {
                case MoveOrder mo -> {
                    //check order validity:
                    if(!mo.isValidated()) {
                        if(!Arrays.asList(u.getLocation().getBorders1()).contains(mo.getMoveTo())) {
                            if(u instanceof Fleet) {
                                //System.out.println("- Order invalidated: "+mo+". Replacing with hold order.");
                                orderQueue.add(new Order(u)); //Add hold order into queue to replace invalid move order
                                continue;
                            }
                            //If the else clause is reached, u must be an army, so check it's borders2 for the target territory
                            else if(!Arrays.asList(u.getLocation().getBorders2()).contains(mo.getMoveTo())) {
                                if(x>=5) { //Convoys are finished resolving at x=5
                                    //Replaced invalid convoy attempt with hold
                                    orderQueue.add(new Order(u));
                                    //System.out.println("- Order \'teleports\'. Invalidating.");
                                    continue;
                                } else {
                                    //System.out.println("- Order is convoy-only");
                                    mo.setConvoyOnly();
                                    continue;
                                }
                            }
                        } else {
                            //System.out.println("- Valid move order");
                            mo.setValid();
                        }
                    }
                    //Cut support orders
                    if(!mo.getMoveTo().isEmpty()) {
                        for(int i = 0; i<orderQueue.size(); i++, orderQueue.add(orderQueue.remove())) {
                            Order top = orderQueue.peek();
                            if(top.getUnit().getLocation().equals(mo.getMoveTo()) && top instanceof SupportOrder) {
                                //check for rules edge case where a unit cannot cut support to another unit attacking the original unit
                                if(((SupportOrder) top).getOrderSupported() instanceof MoveOrder
                                && ((MoveOrder) ((SupportOrder) top).getOrderSupported()).getMoveTo().equals(u.getLocation())) {
                                    //replace this (mo) order with a hold
                                    orderQueue.add(new Order(u));
                                    //System.out.println("- \'"+mo+"\' is an illegal support cut.");
                                    break;
                                }
                                //Remove cut support order and replace with a hold
                                //System.out.println("- \'"+top+"\' cut by \'"+mo+"\'");
                                orderQueue.add(new Order(orderQueue.remove().getUnit()));
                                break;
                            }
                        }
                    }
                    boolean uncontested = true;
                    List<MoveOrder> collisions = new ArrayList<>();
                    //Iterate through the entire queue looking for collisions
                    for (int i = 0; i < orderQueue.size(); i++, orderQueue.add(orderQueue.remove())) {
                        //Check if there are any other move orders that could collide with this order
                        if ((orderQueue.peek() instanceof MoveOrder && ((MoveOrder) orderQueue.peek()).getMoveTo().equals(mo.getMoveTo()))
                                || (orderQueue.peek().getClass() == Order.class && orderQueue.peek().getUnit().getLocation().equals(mo.getMoveTo()))) {
                            uncontested = false;
                            collisions.add((MoveOrder) orderQueue.peek());
                            //System.out.println("- Collision found: " + orderQueue.peek());
                            //System.out.println("   - "+mo.getStrength()+" str vs. "+orderQueue.peek().getStrength()+" str");
                        }
                    }
                    if(mo.getStrength() == -1) {
                        //Move orders that loose standoffs become holds
                        orderQueue.add(new Order(u));
                        //System.out.println("- \'" + mo + "\' bounces");
                        continue;
                    }
                    //uncontested case
                    if (uncontested) {
                        //order succeeds. Note that the order is not added back to the queue, because it's finished processing
                        //System.out.println("- Uncontested order \'" + mo + "\' succeeds");
                        u.setLocation(newMap.getTerritory(mo.getMoveTo().getName()));
                        u.getLocation().setOccupyingUnit(u);
                        continue;
                    //resolve standoffs
                    } else if (x >= 2 && mo.isValidated()) {
                        boolean lostStandoff = false;
                        for (MoveOrder collision : collisions) {
                            if (mo.getStrength() <= collision.getStrength()) {
                                //Move orders that loose standoffs become holds
                                orderQueue.add(new Order(u));
                                lostStandoff = true;
                                //System.out.println("- \'" + mo + "\' bounces");
                            }
                            if (mo.getStrength() >= collision.getStrength()) {
                                collision.setBounce();
                                //System.out.println("- \'"+collision+"\' will bounce");
                            }
                        }
                        if (!lostStandoff) {
                            u.setLocation(newMap.getTerritory(mo.getMoveTo().getName()));
                            u.getLocation().setOccupyingUnit(u);
                            //System.out.println("- \'" + mo + "\' wins standoff");
                            continue;
                        }
                    } else {
                        //add order back into queue for re-assessment later
                        orderQueue.add(mo);
                        //System.out.println("- Cannot be calculated now, adding back to queue");
                    }
                } case SupportOrder so -> {
                    //Check validity:
                    if(!so.isValidated()) {
                        for(int i = 0; i < orderQueue.size(); i++, orderQueue.add(orderQueue.remove())) {
                           if(orderQueue.peek().equals(so.getOrderSupported())) {
                               so.setSupport(orderQueue.peek());
                               so.setValid();
                               //System.out.println("- Valid support order");
                           }
                        }
                    }
                    //Execute support orders after cuts have been finalized
                    if(x>=2 && so.isValidated()) {
                        so.getOrderSupported().addSupport();
                        u.setLocation(newMap.getTerritory(u.getLocation().getName()));
                        u.getLocation().setOccupyingUnit(u);
                        //Do no add so back to queue, since it's finished executing
                    } else if(x>=2 && !so.isValidated()) {
                        //Replace so with a hold order
                        orderQueue.add(new Order(so.getUnit()));
                        //System.out.println("\'"+so+"\' is invalid");
                    } else {
                        //Add so back to queue for further processing
                        orderQueue.add(so);
                    }
                } case ConvoyOrder co -> {
                    //Check validity
                    if(!co.isValidated()) {
                        //Convoys MUST convoy an army across water
                        if(u.getLocation().getType() != Territory.Type.SEA || !(co.getConvoyedMovement().getUnit() instanceof  Army)) {
                            orderQueue.add(new Order(u));
                            //System.out.println("- Invlaid convoy");
                            continue;
                        }
                        for(int i = 0; i < orderQueue.size(); i++, orderQueue.add(orderQueue.remove())) {
                            if(orderQueue.peek().equals(co.getConvoyedMovement())) {
                                co.setConvoyedMovement((MoveOrder) orderQueue.peek());
                                co.setValid();
                                //System.out.println("- Valid convoy");
                            }
                        }
                    }
                    if(x<=3) { //Wait until x=4 to resolve convoys
                        orderQueue.add(co);
                        continue;
                    } else if(!co.isValidated()) {
                        orderQueue.add(new Order(u));
                        //System.out.println("- \'"+co+"\' is invalid");
                        continue;
                    } else {
                        co.getConvoyedMovement().setValid();
                        u.setLocation(newMap.getTerritory(u.getLocation().getName()));
                        u.getLocation().setOccupyingUnit(u);
                        //System.out.println("- convoy \'"+co+"\' successful!");
                    }
                } default -> { //Hold order logic:
                    if(x<=4) {
                        orderQueue.add(o);
                    } else if(!newMap.getTerritory(u.getLocation().getName()).isEmpty()){
                        u.setLocation(null);
                        //System.out.println("- "+u+" dislodged");
                    } else {
                        u.setLocation(newMap.getTerritory(u.getLocation().getName()));
                        u.getLocation().setOccupyingUnit(u);
                        //System.out.println("- \'"+o+"\' successful");
                    }
                }
            }
        }
    }
}