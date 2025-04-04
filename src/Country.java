import java.util.ArrayList;

public class Country {
    private String name;
    private ArrayList<Unit> units;
    private int supplyPoints;
    private ArrayList<Order> currentOrders;
    public ArrayList<Unit> getUnits(){ return this.units; }
    public boolean checkUnit(Unit target) {
        for(Unit i: units) {
            if(i.equals(target)) return true;
        }
        return false;
    }
}
