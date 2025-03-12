public class Order {
    private boolean isValid;
    private boolean isSuccess;
    private Unit unit;
    enum Type {
        HOLD,
        CONVOY,
        MOVE,
        SUPPORT
    }

    private Territory desitnation;
    private Order toSupport;
    private Unit toConvoy;
    public Order(Country leader, String[] args) {
        try {

        } catch(ArrayIndexOutOfBoundsException e) {

        }
    }
}
