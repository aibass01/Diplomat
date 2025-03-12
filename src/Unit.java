import java.util.ArrayList;

public abstract class Unit {
    private Territory currentSpace;
    private Territory.Type canMoveTo;

    //All actions a Unit can perform. Each action returns true if it is completed legally.
    public boolean move(Territory newTerritory, ArrayList<Order> supportOrders) {

    }

}
