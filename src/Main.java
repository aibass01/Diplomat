import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        System.out.println("Hello world!");
        Map map = Map.getInstance();
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
            try {
                p.loadGameState(new File("current/F1900.gs"));
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        }

        compileOrders(players);
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
    }

    public static void compileOrders(Player[] players) {
        for(Player p: players) {
            try {
                p.loadOrders();
                p.loadConvoyOrders();
                p.loadSupportOrders();
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
        ArrayList<Order> holdOrders = Player.getHoldOrders();
        ArrayList<MoveOrder> moveOrders = Player.getMoveOrders();
        ArrayList<SupportOrder> supportOrders = Player.getSupportOrders();
        ArrayList<ConvoyOrder> convoyOrders = Player.getConvoyOrders();
        for(Order o: Player.getAllOrders()) {
            System.out.println(o);
        }
        System.out.println(holdOrders.size());

        // Check validity of all convoy orders
        for(ConvoyOrder co: convoyOrders) {
            Unit u = co.getUnit();
            Territory current = u.getLocation();
            MoveOrder convoying = co.getConvoyedMovement();
            if(!(Arrays.asList(current.getBorders1()).contains(convoying.getMoveTo()) && convoying.getMoveTo().getType() == Territory.Type.COAST)) {
                co.setValid();
                convoying.setValid();
            }
        }
        // Check validity and cut support for move orders
        for(int i = 0; i < moveOrders.size(); i++) {
            MoveOrder mo = moveOrders.get(i);
            Unit u = mo.getUnit();
            Territory current = u.getLocation();
            Territory target = mo.getMoveTo();
            // Checks if the target territory is adjacent to the current territory
            if(!(mo.isValid() == Order.Valid.TRUE || Arrays.asList(current.getBorders1()).contains(target))) {
                if(u instanceof Fleet) {
                    System.out.println("Order invalidated: " + mo);
                    mo.setInvalid();
                    moveOrders.remove(i);
                    i--;
                    continue;
                }
                //If the else clause is reached, u must be an army, so check it's borders2 for the target territory
                else if(!Arrays.asList(current.getBorders2()).contains(target)) {
                    System.out.println("Order invalidated: " + mo);
                    mo.setInvalid();
                    moveOrders.remove(i);
                    i--;
                    continue;
                }
            } else { System.out.println("good order"); mo.setValid(); }
            //Cut support orders being attacked by this unit
            for(SupportOrder so: supportOrders) {
                try { // Executes if orderSupported is a move order
                    MoveOrder orderSupported = (MoveOrder)so.getOrderSupported();
                    // Weird edge case in the rules. If your unit attacks a unit that is supporting an attack on
                    // your attacking unit, you do not cut support
                    if(so.getUnit().getLocation().equals(target) && !(orderSupported.getMoveTo().equals(current))) so.setFail();
                } catch(ClassCastException e) { // Executes if orderSupported is not a move order
                    if(so.getUnit().getLocation().equals(target)) so.setFail();
                }
            }
        }
        // Check validity of all support orders. By this point, all valid UNDECIDED supports will succeed
        // because support cuts have already been evaluated
        for(SupportOrder so: supportOrders) {
            // supportTarget is the place where support is being given
            Territory supportTarget = (so.getOrderSupported() instanceof MoveOrder) ?
                    ((MoveOrder) so.getOrderSupported()).getMoveTo() :
                    so.getOrderSupported().getUnit().getLocation();
            if(!so.getUnit().canMoveTo(supportTarget)) {
                so.setInvalid();
                continue;
            }
            if(so.isValid() != Order.Valid.FALSE && so.isSuccess() != Order.Success.FAIL) {
                so.setPass();
                so.getOrderSupported().addSupport();
            }
        }
        // Now that supports have been allocated, compare the strengths of all units engaged in standoffs
        // Start by dislodging any convoy orders that are out-supported by attacking units
        for(int i = 0; i < moveOrders.size()-1; i++) {
            for (ConvoyOrder convoyOrder : convoyOrders) {
                if (moveOrders.get(i).getMoveTo().equals(convoyOrder.getUnit().getLocation()))
                    Order.resolveStandoff(moveOrders.get(i), convoyOrder);
            }
        }
        // Now that appropriate convoys have been dislodged, resolve standoffs between move and hold orders
        for(int i = 0; i < moveOrders.size()-1; i++) {
            for(int j = i + 1; j < moveOrders.size(); j++) {
                if(moveOrders.get(i).getMoveTo().equals(moveOrders.get(j).getMoveTo())) {
                    Order.resolveStandoff(moveOrders.get(i), moveOrders.get(j));
                }
            }
            for (Order holdOrder : holdOrders) {
                System.out.println("why is this code running");
                if (moveOrders.get(i).getMoveTo().equals(holdOrder.getUnit().getLocation())) {
                    Order.resolveStandoff(moveOrders.get(i), holdOrder);
                }
            }
            // Uncontested units auto succeed
            if(moveOrders.get(i).getMoveTo().getOccupyingUnit() == null) {
                moveOrders.get(i).setPass();
            }
        }
        // Units fill newly vacated spots
        for(MoveOrder mo : moveOrders) {
            if(mo.getMoveTo().getOccupyingUnit() == null) {
                mo.setPass();
            }
        }
        // If any units think they are still occupying a territory but have actually been dislodged from that territory,
        // dislodge those units by setting their location to null
        for(Player p: players) {
            for(Unit u: p.getUnits()) {
                if(u.getLocation() != null && !u.getLocation().getOccupyingUnit().equals(u)) u.setLocation(null);
            }
        }
    }
}