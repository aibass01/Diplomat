import java.util.ArrayList;

public abstract class Unit {
    protected Territory location;
    final Territory.Type[] can_move_to;

    public Territory getLocation() {
        return location;
    }

    protected Unit(Territory.Type[] canMoveTo, Territory location) {
        this.can_move_to = canMoveTo;
        this.location = location;
        Map map = Map.getInstance();
        map.getTerritory(location.getName()).setOccupying_unit(this);
    }

    /*
    public boolean equals() {

    }
    //All actions a Unit can perform. Each action returns true if it is completed legally.
    public boolean move(Territory toTerritory, int supports) {

    }
    */
}
class Army extends Unit {
    public Army(Territory location) {
        super(new Territory.Type[]{Territory.Type.LAND, Territory.Type.COAST}, location);
    }
    public String toString() {
        return "A " + location;
    }
}
class Fleet extends Unit {
    public Fleet(Territory location) {
        super(new Territory.Type[]{Territory.Type.SEA, Territory.Type.COAST}, location);
    }
    public String toString() {
        return "F " + location;
    }
}
