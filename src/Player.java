import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;

public class Player {
    static Map map = Map.getInstance();
    private final String nation;
    private ArrayList<Unit> units;

    private int supply_points;
    private ArrayList<Order> current_orders;
    public Player(String nation) {
        this.nation = nation;
        this.units = new ArrayList<>();
    }

    public ArrayList<Unit> getUnits(){ return this.units; }
    public Unit getUnit(String name) {
        for(Unit u : units) {
            if(u.getLocation().getName() == name) return u;
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
                    switch(line.charAt(0)) {
                        case 'A':
                            units.add(new Army(map.getTerritory(line.substring(2,5))));
                            break;
                        case 'F':
                            units.add(new Fleet(map.getTerritory(line.substring(2,5))));
                            break;
                        default: throw new IllegalArgumentException("Unit entries must be marked with 'A' for Army of 'F' for Fleet.");
                    }
                }
                break;
            }
        }
        sc.close();
    }
    public boolean checkUnit(Unit target) {
        for(Unit i: units) {
            if(i.equals(target)) return true;
        }
        return false;
    }
}
