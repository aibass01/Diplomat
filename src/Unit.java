import java.util.ArrayList;
import java.util.Arrays;

public abstract class Unit {
    private static Map map = Map.getInstance();
    private Player owner;
    public Player getOwner() { return owner; }
    protected Territory location;
    protected final Territory PREVIOUS_LOCATION;
    public Territory getLocation() {
        return location;
    }
    public Territory getPREVIOUS_LOCATION() { return PREVIOUS_LOCATION; }
    public void setLocation(Territory t) {
        this.location = t;
        if(t != null) t.setOccupyingUnit(this);
    }

    protected Unit(Territory location, Player owner) {
        this.location = location;
        this.PREVIOUS_LOCATION = location;
        this.owner = owner;
        Map map = Map.getInstance();
        map.getTerritory(location.getName()).setOccupyingUnit(this);
    }

    public boolean equals(Unit other) {
        return ((this.location == null && other.getLocation() == null) || this.location.equals(other.getLocation())) && this.getClass().equals(other.getClass());
    }

    public boolean canMoveTo(Territory target) {
        return Arrays.asList(location.getBorders1()).contains(target);
    }
}
class Army extends Unit {
    public Army(Territory location, Player owner) {
        super(location, owner);
    }
    public String toString() {
        return "A " + location;
    }
}
class Fleet extends Unit {
    public Fleet(Territory location, Player owner) {
        super(location, owner);
    }
    public String toString() {
        return "F " + location;
    }
    @Override
    public boolean canMoveTo(Territory target) {
        return super.canMoveTo(target) || Arrays.asList(location.getBorders2()).contains(target);
    }
}
