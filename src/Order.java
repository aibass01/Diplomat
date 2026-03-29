import java.util.Arrays;

public class Order {
    private static final Map map = Map.getInstance();
    private boolean validated = false;
    public boolean isValidated() {
        return validated;
    }
    public void setValid() { validated = true; }
    public void setPass() { if(isSuccess == Success.UNDECIDED) isSuccess = Success.PASS; }
    public void setFail() {
        if(isSuccess == Success.UNDECIDED) isSuccess = Success.FAIL;
    }
    public enum Success {
        PASS,
        FAIL,
        UNDECIDED
    }

    public Success isSuccess() {
        return isSuccess;
    }

    private Success isSuccess = Success.UNDECIDED;
    private int support = 0;
    public int getStrength() {
        return support;
    }

    public void addSupport() {
        support++;
    }
    public void setBounce() { support = -1; }
    protected Unit unit = null;
    public Order() { isSuccess = Success.FAIL; validated = false; }
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
        Unit u = p.getUnit(strs[0].charAt(0), map.getTerritory(strs[1]));
        if(u != null) {
            return switch(strs[2]) {
                case "H" -> new Order(u);
                case "-" -> new MoveOrder(u, map.getTerritory(strs[3]));
                case "S" -> new SupportOrder(u, Order.stringArrayToOrder(Arrays.copyOfRange(strs, 3, strs.length)));
                case "C" -> new ConvoyOrder((Fleet) u, (MoveOrder) Order.stringArrayToOrder(Arrays.copyOfRange(strs,3, strs.length)));
                default -> new Order(u); // default order is hold
            };
        } else return new Order(); // call to 0 arg constructor indicates invalid order
    }
    //Overloaded version used to generate PLACEHOLDER orders
    public static Order stringArrayToOrder(String[] strs) {
        Unit u = switch(strs[0].charAt(0)) { //PLACEHOLDER UNIT
            case 'A' -> new Army(Map.getInstance().getTerritory(strs[1]));
            case 'F' -> new Fleet(Map.getInstance().getTerritory(strs[1]));
            default -> null;
        };
        if(u != null) {
            return switch(strs[2]) {
                case "H" -> new Order(u);
                case "-" -> new MoveOrder(u, map.getTerritory(strs[3]));
                default -> new Order(u); // default order is hold
            };
        } else return new Order(); // call to 0 arg constructor indicates invalid order
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
    public void setPass(){
        if(isSuccess() == Success.UNDECIDED) {
            super.setPass();
            unit.setLocation(moveTo);
        }
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