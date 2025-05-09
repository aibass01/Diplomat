import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.util.ArrayList;

public class Map {
    private static Map single_instance = null;
    private final Territory[] territories;

    // Map() implements singleton architecture
    private Map() throws FileNotFoundException {
        this.territories = populateMapFromTable();
    }
    public static Map getInstance() {
        if(single_instance == null) {
            try {
                single_instance = new Map();
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
        return single_instance;
    }
    private Territory[] populateMapFromTable() throws FileNotFoundException {
        File file = new File("src/Map.txt");
        Scanner sc = new Scanner(file);
        // Each line in the ArrayList represents a territory using an array of length 3,
        // where index 0 is [name, type],
        // index 1 is an array with the adjacent territories a Fleet or Army could move to,
        // and index 2 is an array the adjacent territories only an ARmy could move to.
        ArrayList<String[][]> data = new ArrayList<>();
        while (sc.hasNext()) {
            String[] line = sc.next().split("[\\|]");
            // Columns are of indeterminate size at compile time
            String[][] split_line = new String[3][];
            for(int i = 0; i<3; i++) {
                try{
                    split_line[i] = line[i].split("[,]");
                } catch(ArrayIndexOutOfBoundsException e) {
                    // Some territories have only neighbors that a Fleet could move to,
                    // resulting in a line thatis split into only 2 parts by '|'.
                    // Catching this case and appending an empty String[] ensures that split_line is always of length 3.
                    split_line[i] = new String[0];
                }
            }
            data.add(split_line);
        }
        sc.close();
        // Convert ArrayList<String[3][]> to Territory[]:
        Territory[] result = new Territory[data.size()];
        // Iterate through data, creating an empty Territory in 'result' for each line
        for(int i = 0; i<data.size(); i++) {
            result[i] = new Territory(data.get(i)[0][0], data.get(i)[0][1], data.get(i)[0][2]);
        }
        // Now that all Territories have been declared, initialize their border instance data
        // Territories are all interlinked, so we can't do this in the Territory() constructor
        for(int i = 0; i< data.size(); i++) {
            Territory[] borders1 = new Territory[data.get(i)[1].length];
            Territory[] borders2 = new Territory[data.get(i)[2].length];
            for(int j = 0; j<borders1.length; j++) {
                borders1[j] = getTerritory(data.get(i)[1][j], result);
            }
            for(int j = 0; j<borders2.length; j++) {
                borders2[j] = getTerritory(data.get(i)[2][j], result);
            }
            result[i].setBorders(borders1, borders2);
        }
        return result;
    }

    // Implements binary search. Overloaded for ease of use in populateMapFromTable.
    public Territory getTerritory(String target) {
        Territory[] list = territories;
        int min_index = 0, max_index = list.length-1, i;
        while(max_index>=min_index) {
            i = (max_index+min_index)/2;
            if(list[i].getName().compareTo(target) < 0) min_index = i+1;
            else if(list[i].getName().compareTo(target) > 0) max_index = i-1;
            else return list[i];
        }
        return null;
    }
    public Territory getTerritory(String target, Territory[] searchThrough) {
        Territory[] list = searchThrough;
        int min_index = 0, max_index = list.length-1, i;
        while(max_index>=min_index) {
            i = (max_index+min_index)/2;
            if(list[i].getName().compareTo(target) < 0) {
                min_index = i+1;
            }
            else if(list[i].getName().compareTo(target) > 0) {
                max_index = i - 1;
            }
            else return list[i];
        }
        return null;
    }
    public Territory[] getAllTerritories() {
        return territories;
    }
}
