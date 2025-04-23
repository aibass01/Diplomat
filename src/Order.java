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
    private Territory move_to;

    public MoveOrder(Unit unit, Territory move_to) {
        super(unit);
        this.move_to = move_to;
    }
}
class SupportOrder extends  Order {
    private Order order_supported;

    public SupportOrder(Unit unit, Order order_supported) {
        super(unit);
        this.order_supported = order_supported;
    }
}
class ConvoyOrder extends  Order {
    private Unit unit_supported;

    public ConvoyOrder(Unit unit, Unit unit_supported) {
        super(unit);
        this.unit_supported = unit_supported;
    }
}