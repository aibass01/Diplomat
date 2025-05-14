import java.util.Arrays;

public class Order {
    private static final Map map = Map.getInstance();
    public static enum Valid {
        TRUE,
        FALSE,
        UNKNOWN;
    }
    private Valid isValid = Valid.UNKNOWN;
    public Valid isValid() {
        return isValid;
    }
    public void setValid() { isValid = Valid.TRUE; }
    public void setInvalid() {
        isValid = Valid.FALSE;
        isSuccess = Success.FAIL;
    }
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
    public int getSupport() {
        return support;
    }

    public void addSupport() {
        support++;
    }
    protected Unit unit = null;
    String[] targetStringArray;

    public Order() { isSuccess = Success.FAIL; isValid = Valid.FALSE; }
    public  Order(Unit unit) {
        this.unit = unit;
    }

    public String toString() {
        return unit.toString();
    }

    public Unit getUnit() {
        return unit;
    }

    public boolean equals(Order other) {
        return unit.equals(other.getUnit());
    }

    public String[] getTargetStringArray() {
        return targetStringArray;
    }

    public static Order stringArrayToOrder(Player p, String[] strs) {
        Unit u = p.getUnit(strs[0].charAt(0), map.getTerritory(strs[1]));
        if(u != null) {
            return switch(strs[2]) {
                case "H" -> new Order(u);
                case "-" -> new MoveOrder(u, map.getTerritory(strs[3]));
                case "S" -> new SupportOrder(u);
                case "C" -> new ConvoyOrder((Fleet) u, Arrays.copyOfRange(strs, 4, 8));
                default -> new Order(u); // default order is hold
            };
        } else return new Order(); // call to 0 arg constructor indicates invalid order
    }
    
    public static void resolveStandoff(Order o1, Order o2) {
        if (o1.getSupport() > o2.getSupport()) {
            System.out.println(o1.getUnit() + " wins!");
            o1.setPass();
            o2.setFail();
            // If o2 is a hold or convoy unit order, dislodge it's unit
            // This causes move orders being convoyed by said dislodged unit to fail
            if(o2.getClass().equals(Order.class)) o2.getUnit().setLocation(null);
            if(o2.getClass().equals(ConvoyOrder.class)) {
                o2.getUnit().setLocation(null);
                ((ConvoyOrder) o2).getConvoyedMovement().setFail();
            }
        } else if(o1.getSupport() < o2.getSupport()) {
            System.out.println(o2.getUnit() + " wins!");
            o2.setPass();
            o1.setFail();
        } else {
            System.out.println("Tie between " + o1.getUnit() + " and " + o2.getUnit());
            o1.setFail();
            o2.setFail();
        }
    }
}
class MoveOrder extends Order {
    private final Territory moveTo;
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
        return super.toString() + " - " + moveTo.toString();
    }
}
class SupportOrder extends  Order {
    private Order orderSupported;
    public Order getOrderSupported() {
        return orderSupported;
    }
    public SupportOrder(Unit unit) {
        super(unit);
    }

    public void setSupport(Order orderSupported) {
        if(this.orderSupported != null) throw new SecurityException("Field 'ORDER_SUPPORTED' is immutable once assigned");
        this.orderSupported = orderSupported;
    }

    public String toString() {
        return super.toString() + " S " + orderSupported.toString();
    }
}
class ConvoyOrder extends  Order {
    private MoveOrder convoyedMovement;
    public MoveOrder getConvoyedMovement() {
        return convoyedMovement;
    }
    public ConvoyOrder(Fleet fleet, String[] targetStringArray) {
        super(fleet);
        this.targetStringArray = targetStringArray;
    }

    public String toString() {
        return super.toString() + " C " + convoyedMovement.toString();
    }
    public void setConvoyedMovement(MoveOrder convoyedMovement) {
        this.convoyedMovement = convoyedMovement;
    }
}