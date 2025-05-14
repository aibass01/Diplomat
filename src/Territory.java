public class Territory {
    public enum Type {
        LAND,
        SEA,
        COAST
    }
    private final String name; //Names should be 3 letters all caps i.e. "PRU" for prussia
    private final Type type;
    private final boolean isSupplyPoint;

    public void setOwner(Player owner) {
        this.owner = owner;
    }

    public Player getOwner() {
        return owner;
    }

    private Player owner = null;

    public Unit getOccupyingUnit() {
        return occupyingUnit;
    }

    public void setOccupyingUnit(Unit occupying_unit) {
        this.occupyingUnit = occupying_unit;
    }

    private Unit occupyingUnit;

    public String getName() {
        return name;
    }

    public Type getType() {
        return type;
    }

    public Territory[] getBorders1() {
        return borders1;
    }

    public Territory[] getBorders2() {
        return borders2;
    }

    // borders1 is all territories that any unit could move to from this territory,
    // borders2 is territories only an Army could move to.
    private Territory[] borders1, borders2;
    public Territory(String name, String type, String is_supply_point) {
        this.name = name;
        this.isSupplyPoint = is_supply_point.equals("Y");
        switch(type) {
            case "L": this.type = Type.LAND; break;
            case "S": this.type = Type.SEA; break;
            case "C": this.type = Type.COAST; break;
            default: this.type = null; break;
        }
    }

    public String toString() {
        return name;
    }

    public void setBorders(Territory[] borders1, Territory[] borders2) {
        if(this.borders1 != null) throw new IllegalArgumentException("Borders cannot be mutated once initialized.");
        this.borders1 = borders1;
        this.borders2 = borders2;
    }
}
