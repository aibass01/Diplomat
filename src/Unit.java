import java.util.ArrayList;
import java.util.Arrays;

public abstract class Unit {
    private static Map map = Map.getInstance();
    protected Territory location;
    private final Territory PREVIOUS_LOCATION;

    public Territory getLocation() {
        return location;
    }
    public Territory getPREVIOUS_LOCATION() { return PREVIOUS_LOCATION; }

    public void setLocation(Territory t) {
        location.setOccupyingUnit(null);
        this.location = t;
        t.setOccupyingUnit(this);
    }

    protected Unit(Territory location) {
        this.location = location;
        this.PREVIOUS_LOCATION = location;
        Map map = Map.getInstance();
        map.getTerritory(location.getName()).setOccupyingUnit(this);
    }

    public boolean equals(Unit other) {
        return this.getLocation() == other.getLocation();
    }

    public boolean canMoveTo(Territory target) {
        return Arrays.asList(location.getBorders1()).contains(target);
    }
}
class Army extends Unit {
    public Army(Territory location) {
        super(location);
    }
    public String toString() {
        return "A " + location;
    }
}
class Fleet extends Unit {
    public Fleet(Territory location) {
        super(location);
    }
    public String toString() {
        return "F " + location;
    }
    @Override
    public boolean canMoveTo(Territory target) {
        return super.canMoveTo(target) || Arrays.asList(location.getBorders2()).contains(target);
    }
}
