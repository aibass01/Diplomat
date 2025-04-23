import java.io.File;
import java.io.FileNotFoundException;

public class Main {
    public static void main(String[] args) {
        System.out.println("Hello world!");
        Map map = Map.getInstance();
        /*
        System.out.println("Printing data from a territory on the map:");
        System.out.println(map.getTerritory("YOR"));
        for(Territory t : map.getTerritory("YOR").getBorders1()) {
            System.out.println(t);
        }
         */
        Player[] players = new Player[] {
                new Player("AUSTRIA"),
                new Player("ENGLAND"),
                new Player("FRANCE"),
                new Player("GERMANY"),
                new Player("ITALY"),
                new Player("RUSSIA"),
                new Player("TURKEY")

        };
        for(Player p : players) {
            try {
                p.loadGameState(new File("gamestates/F1900-0000.gs"));
                System.out.println(p.getNumSupplyPoints());
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
        for(Territory t : map.getAllTerritories()) {
            System.out.println(t.getOwner());
        }
    }
}