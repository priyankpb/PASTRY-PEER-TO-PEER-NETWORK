package cs555.routing;

import java.util.Map;
import java.util.TreeMap;

public class RoutingTable {

    public String nodeID;
    public Map<Integer, Map<String, RoutingEntry>> routingtable = new TreeMap<>();

    public RoutingTable(String nodeID) {
        nodeID = nodeID.toUpperCase();
        this.nodeID = nodeID;
        for (int i = 0; i < 4; i++) {
            Map<String, RoutingEntry> row = new TreeMap<>();
            for (int j = 0; j < 16; j++) {
                String handle = "";
                handle = nodeID.substring(0, i) + Integer.toHexString(j).toUpperCase();
                row.put(handle, null);
            }
            routingtable.put(i, row);
        }
    }

    public RoutingTable(String nodeID, Map routingtable) {
        this.nodeID = nodeID;
        this.routingtable = routingtable;
    }

    public void printRoutingTable() {
        System.out.println("[INFO] -----------------------------");
        System.out.println("[INFO] --------ROUTING TABLE--------");
        System.out.println("[INFO] Node ID: " + nodeID);
        System.out.println("[INFO] -----------------------------");
        for (Map.Entry<Integer, Map<String, RoutingEntry>> entrySet : routingtable.entrySet()) {
            Integer key = entrySet.getKey();
            Map<String, RoutingEntry> value = entrySet.getValue();
            System.out.println("[INFO] " + key + " : " + value);
        }
        System.out.println("[INFO] -----------------------------");
    }

}
