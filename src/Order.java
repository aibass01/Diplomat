public class Order {
    private boolean is_valid = false;
    private enum Success {
        PASS,
        FAIL,
        UNDECIDED
    }
    private Success is_success = Success.UNDECIDED;
    private Unit unit = null;
    public Order() { is_success = Success.FAIL; }
    public  Order(Unit unit) {
        this.is_valid = true;
        this.unit = unit;
    }
}
class MoveOrder extends Order {
    private final Territory MOVE_TO;

    public MoveOrder(Unit unit, Territory move_to) {
        super(unit);
        this.MOVE_TO = move_to;
    }
}
class SupportOrder extends  Order {
    private final Order ORDER_SUPPORTED;

    public SupportOrder(Unit unit, Order order_supported) {
        super(unit);
        this.ORDER_SUPPORTED = order_supported;
    }
}
class ConvoyOrder extends  Order {
    private final Unit UNIT_TO_CONVOY;

    public ConvoyOrder(Unit unit, Unit unit_to_convoy) {
        super(unit);
        this.UNIT_TO_CONVOY = unit_to_convoy;
    }
}