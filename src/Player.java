import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

public class Player {
    static Map map = Map.getInstance();
    private final String nation;
    public String getNation() { return nation; }
    private ArrayList<Unit> units;
    private int supply_points;
    private ArrayList<Order> orders;
    private static ArrayList<Order> allOrders = new ArrayList<>();
    public static ArrayList<Order> getAllOrders() {
        return allOrders;
    }
    public static ArrayList<Order> getHoldOrders() {
        ArrayList<Order> result = new ArrayList<>();
        for(Order o: allOrders) {
            //NOTE: hold orders are represented by valid default Orders, not any subclass of Order
            if(!(o instanceof MoveOrder || o instanceof ConvoyOrder || o instanceof SupportOrder)) {
                result.add(o);
            }
        }
        return result;
    }
    public static ArrayList<MoveOrder> getMoveOrders() {
        ArrayList<MoveOrder> result = new ArrayList<>();
        for(Order o: allOrders) {
            //NOTE: hold orders are represented by valid default Orders, not any subclass of Order
            if(o instanceof MoveOrder) {
                System.out.println("found a move order");
                result.add((MoveOrder)o);
            }
        }
        return result;
    }
    public static ArrayList<SupportOrder> getSupportOrders() {
        ArrayList<SupportOrder> result = new ArrayList<>();
        for(Order o: allOrders) {
            //NOTE: hold orders are represented by valid default Orders, not any subclass of Order
            if(o.getClass().equals(ConvoyOrder.class)) result.add((SupportOrder)o);
        }
        return result;
    }
    public static ArrayList<ConvoyOrder> getConvoyOrders() {
        ArrayList<ConvoyOrder> result = new ArrayList<>();
        for(Order o: allOrders) {
            //NOTE: hold orders are represented by valid default Orders, not any subclass of Order
            if(o.getClass().equals(ConvoyOrder.class)) result.add((ConvoyOrder)o);
        }
        return result;
    }

    private static final File ORDERS_DIR = new File("current/orders");
    public Player(String nation) {
        this.nation = nation;
        this.units = new ArrayList<>();
        this.orders = new ArrayList<>();
    }

    public ArrayList<Order> getOrders() {
        return orders;
    }

    public ArrayList<Unit> getUnits(){ return this.units; }
    public Unit getUnit(String name) {
        for(Unit u : units) {
            if(u.getLocation().getName().equals(name)) return u;
        }
        return null;
    }
    public int getNumSupplyPoints() { return supply_points; }

    public void loadGameState(File f) throws FileNotFoundException {
        Scanner sc = new Scanner(f);
        supply_points = 0;
        while(true) {
            if(sc.nextLine().equals(nation)) {
                for(String s : sc.nextLine().split("[,]")) {
                    map.getTerritory(s).setOwner(this);
                    supply_points++;
                }
                while(sc.hasNextLine()) {
                    String line = sc.nextLine();
                    if(line.isEmpty()) break;
                    switch (line.charAt(0)) {
                        case 'A' -> units.add(new Army(map.getTerritory(line.substring(2, 5))));
                        case 'F' -> units.add(new Fleet(map.getTerritory(line.substring(2, 5))));
                        default -> throw new IllegalArgumentException("Unit entries must be marked with 'A' for Army of 'F' for Fleet.");
                    }
                }
                break;
            }
        }
        sc.close();
    }

    public void loadOrders() throws FileNotFoundException {
        if(ORDERS_DIR.exists() && ORDERS_DIR.isDirectory()) {
            File[] files;
            try {
                files = ORDERS_DIR.listFiles((dir, name) -> name.substring(0, name.length()-4).equals(nation));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            switch(files.length) {
                case 0: throw new FileNotFoundException("No orders found for country:" + nation);
                case 1:
                    Scanner sc = new Scanner(files[0]);
                    while(sc.hasNextLine()) {
                        System.out.println("Scanning an order in");
                        String[] line = sc.nextLine().split("[ ]");
                        //Line format: ["A", "PAR", "-", "BUR"]
                        orders.add(Order.stringArrayToOrder(this, Arrays.copyOfRange(line, 0, 4)));
                    }
                    sc.close();
                    allOrders.addAll(orders);
                    break;
                default: throw new IllegalStateException("Too many orders files found.");
            }
        } else throw new FileNotFoundException("Unable to locate directory:" + ORDERS_DIR);
    }
    public void loadConvoyOrders() {
        for(Order order: orders) {
            if(order instanceof ConvoyOrder) {
                Order targetOrder = Order.stringArrayToOrder(this, order.getTargetStringArray());
                for(Order other: allOrders) {
                    if(other.equals(targetOrder)) ((ConvoyOrder) order).setConvoyedMovement((MoveOrder) other);
                }
            }
        }
    }

    public void loadSupportOrders() {
        for(Order order: orders) {
            if(order instanceof SupportOrder) {
                Order targetOrder = Order.stringArrayToOrder(this, order.getTargetStringArray());
                for(Order other: allOrders) {
                    if(other.equals(targetOrder)) ((SupportOrder) order).setSupport(other);
                }
            }
        }
    }
    public Unit getUnit(char unitType, Territory location) {
        // Implements linear search
        for(Unit i: units) {
            if (i.getLocation().equals(location)) {
                return switch (i) {
                    case Army a -> (unitType == 'A') ? a : null;
                    case Fleet f -> (unitType == 'F') ? f : null;
                    default -> throw new IllegalArgumentException("Unexpected unit type:" + unitType);
                };
            }
        }
        return null;
    }
}
