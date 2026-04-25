import java.util.Arrays;

public class Order {
    private static final Map map = Map.getInstance();
    private static final Map newMap = Map.getNewMap();
    private boolean validated = false;
    public boolean isValidated() {
        return validated;
    }
    public void setValid() { validated = true; }
    private int support = 0;
    public int getStrength() {
        return support;
    }

    public void addSupport() {
        support++;
    }
    public void setBounce() { support = -1; }
    protected Unit unit = null;
    public Order() { validated = false; }
    public  Order(Unit unit) {
        this.unit = unit;
    }

    public String toString() {
        return unit.toString() + " H";
    }

    public Unit getUnit() {
        return unit;
    }

    public boolean equals(Order other) {
        return unit.equals(other.getUnit()) && this.getClass().equals(other.getClass());
    }

    public static Order stringArrayToOrder(Player p, String[] strs) {
        if(strs[2].equals("B")) { //Build orders create a new unit, so u (below) will return null if it tries to search for them
            Territory loc = newMap.getTerritory(strs[1]);
            return new BuildOrder((strs[0].charAt(0) == 'A') ? new Army(loc, p) : new Fleet(loc, p), loc);
        }
        Unit u = p.getUnit(strs[0].charAt(0), map.getTerritory(strs[1])); //Find order operand
        if(u != null) {
            return switch(strs[2]) { //Construct the correct type of order for the given operator
                case "H" -> new Order(u);
                case "-" -> new MoveOrder(u, map.getTerritory(strs[3]));
                //Support and convoy orders create a placeholder order that they use to compare to other orders later
                case "S" -> new SupportOrder(u, Order.stringArrayToOrder(Arrays.copyOfRange(strs, 3, strs.length)));
                case "C" -> new ConvoyOrder((Fleet) u, (MoveOrder) Order.stringArrayToOrder(Arrays.copyOfRange(strs, 3, strs.length)));
                case ">" -> new RetreatOrder(u, newMap.getTerritory(strs[3]));
                case "D" -> new DisbandOrder(u);
                default -> new Order(u); // default order is hold
            };
        } else return new Order();
    }
    //Overloaded version used to generate PLACEHOLDER orders
    public static Order stringArrayToOrder(String[] strs) {
        Unit u = switch(strs[0].charAt(0)) { //PLACEHOLDER UNIT
            case 'A' -> new Army(Map.getInstance().getTerritory(strs[1]), null);
            case 'F' -> new Fleet(Map.getInstance().getTerritory(strs[1]), null);
            default -> null;
        };
        if(u != null) {
            return switch(strs[2]) { //Only move and hold orders will need placeholders
                case "H" -> new Order(u);
                case "-" -> new MoveOrder(u, map.getTerritory(strs[3]));
                default -> new Order(u); // default order is hold
            };
        } else return new Order();
    }
}
class MoveOrder extends Order {
    private final Territory moveTo;
    //Move orders that seem to 'teleport' across the map require convoys to work.
    private boolean needsConvoy = false;
    public boolean isConvoyOnly() { return needsConvoy; }
    public void setConvoyOnly() { needsConvoy = true; }
    public Territory getMoveTo() {
        return moveTo;
    }
    public MoveOrder(Unit unit, Territory moveTo) {
        super(unit);
        this.moveTo = moveTo;
    }
    @Override
    public String toString() {
        return unit.toString() + " - " + moveTo.toString();
    }
}
class SupportOrder extends  Order {
    //OrderSupported holds a placeholder Order until this SupportOrder has been validated
    private Order orderSupported;
    public Order getOrderSupported() {
        return orderSupported;
    }
    public SupportOrder(Unit unit, Order orderSupported) {
        super(unit);
        this.orderSupported = orderSupported;
    }

    public void setSupport(Order orderSupported) {
        if(isValidated()) throw new SecurityException("Field 'ORDER_SUPPORTED' is immutable once assigned");
        this.orderSupported = orderSupported;
    }

    public String toString() {
        return unit.toString() + " S " + orderSupported.toString();
    }
}
class ConvoyOrder extends  Order {
    private MoveOrder convoyedMovement;
    public MoveOrder getConvoyedMovement() {
        return convoyedMovement;
    }
    public ConvoyOrder(Fleet fleet, MoveOrder convoyedMovement) {
        super(fleet);
        this.convoyedMovement = convoyedMovement;
    }

    public String toString() {
        return unit.toString() + " C " + convoyedMovement.toString();
    }
    public void setConvoyedMovement(MoveOrder convoyedMovement) {
        if(isValidated()) throw new SecurityException("Field 'CONVOYED_MOVEMENT' is immutable once assigned");
        this.convoyedMovement = convoyedMovement;
    }
}
class RetreatOrder extends Order {
    private final Territory retreatTo;
    public Territory getRetreatTo() {
        return retreatTo;
    }
    public RetreatOrder(Unit unit, Territory retreatTo) {
        super(unit);
        this.retreatTo = retreatTo;
    }
    @Override
    public String toString() {
        return unit.toString()+" > "+retreatTo.toString();
    }
}
class DisbandOrder extends Order {
    public DisbandOrder(Unit u) {
        super(u);
    }
    @Override
    public String toString() {
        return unit.toString()+" D";
    }
}
class BuildOrder extends Order {
    private final Territory location;
    public Territory getLocation() {
        return location;
    }
    public BuildOrder(Unit u, Territory location) {
        super(u);
        this.location = location;
    }

    @Override
    public String toString() {
        return unit.toString()+" B";
    }
}