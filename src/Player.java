import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

public class Player {
    private final Map map = Map.getInstance();
    private final String nation;
    public String getNation() { return nation; }
    private ArrayList<Unit> units;
    private ArrayList<Order> orders;
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
    public int getNumSupplyPoints(Map m) { return m.getTerritoriesOwnedBy(this).length; }

    public void loadGameState(File f) throws FileNotFoundException {
        Scanner sc = new Scanner(f);
        while(true) {
            if(sc.nextLine().equals(nation)) { //Find this player's section in the game state file
                for(String s : sc.nextLine().split("[,]")) {
                    map.getTerritory(s).setOwner(this);
                    // Emulates putting tokens on the board to show which territories are owned by which player
                }
                while(sc.hasNextLine()) {
                    String line = sc.nextLine();
                    if(line.isEmpty()) break;
                    switch (line.charAt(0)) { // Create a unit for each unit listed in the game state
                        case 'A' -> units.add(new Army(map.getTerritory(line.substring(2, 5)), this));
                        case 'F' -> units.add(new Fleet(map.getTerritory(line.substring(2, 5)), this));
                        default -> throw new IllegalArgumentException("Unit entries must be marked with 'A' for Army of 'F' for Fleet.");
                    }
                }
                break;
            }
        }
        sc.close();
    }

    public void loadOrders() throws FileNotFoundException {
        orders = new ArrayList<>();
        if(ORDERS_DIR.exists() && ORDERS_DIR.isDirectory()) {
            File[] files;
            try {
                files = ORDERS_DIR.listFiles((dir, name) -> name.substring(0, name.length()-4).equals(nation));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            //There should be exactly 1 file for each country's orders
            switch(files.length) {
                case 0: throw new FileNotFoundException("No orders found for country:" + nation);
                case 1:
                    Scanner sc = new Scanner(files[0]);
                    sc.nextLine(); //Clear player id on first line
                    while(sc.hasNextLine()) {
                        //System.out.print("Scanning an order in: ");
                        String[] line = sc.nextLine().split(" ");
                        //Line format: ["A", "PAR", "-", "BUR"]
                        orders.add(Order.stringArrayToOrder(this, line));
                        //System.out.println(orders.get(orders.size()-1).toString());
                    }
                    sc.close();
                    break;
                default: throw new IllegalStateException("Too many orders files found.");
            }
        } else throw new FileNotFoundException("Unable to locate directory:" + ORDERS_DIR);
    }

    public Unit getUnit(char unitType, Territory location) {
        // Implements linear search
        for(Unit i: units) {
            if(i.getLocation() == null && !i.getPREVIOUS_LOCATION().equals(location)) continue;
            if ((i.getLocation() == null && i.getPREVIOUS_LOCATION().equals(location)) || i.getLocation().equals(location)) {
                return switch (i) {
                    case Army a -> (unitType == 'A') ? a : null;
                    case Fleet f -> (unitType == 'F') ? f : null;
                    default -> throw new IllegalArgumentException("Unexpected unit type:" + unitType);
                };
            }
        }
        return null;
    }

    public void addUnit(Unit u) {
        units.add(u);
    }
}
