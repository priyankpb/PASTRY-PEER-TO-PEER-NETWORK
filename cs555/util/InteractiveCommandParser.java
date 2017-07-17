package cs555.util;

import cs555.nodes.DataStore;
import cs555.nodes.DiscoveryNode;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import cs555.nodes.Node;
import cs555.nodes.PeerNode;
import java.io.File;
import java.util.StringTokenizer;

public class InteractiveCommandParser implements Runnable {

    private final Node node;

    public InteractiveCommandParser(Node node) {
        this.node = node;

    }

    @Override
    public void run() {
        Scanner sc = new Scanner(System.in);
        while (true) {
            String command = sc.nextLine();
            try {
                executeCommand(command);
            } catch (UnknownHostException ex) {
                Logger.getLogger(InteractiveCommandParser.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(InteractiveCommandParser.class.getName()).log(Level.SEVERE, null, ex);
            } catch (Exception ex) {
                Logger.getLogger(InteractiveCommandParser.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private void executeCommand(String command) throws UnknownHostException, IOException, Exception {
        if (command.contains("CLOSE") || command.contains("Close") || command.contains("close")) {
            PeerNode pn = (PeerNode) this.node;
            pn.closeNode();
        } else if ((command.startsWith("store")) || (command.startsWith("STORE")) || (command.startsWith("Store"))) {
            DataStore ds = (DataStore) this.node;
            StringTokenizer tokenizedCommand = new StringTokenizer(command);
            StringBuffer buf = new StringBuffer();
            StringBuffer buf2 = new StringBuffer();
            for (int i = 0; tokenizedCommand.hasMoreTokens(); i++) {
                if (i == 0) {
                    tokenizedCommand.nextToken();
                } else if (i == 1) {
                    buf2.append(tokenizedCommand.nextToken());
                    buf2.append(" ");
                } else {
                    buf.append(tokenizedCommand.nextToken());
                    buf.append(" ");
                }
            }
            String temp = buf.toString();
            String fileName = temp.trim();

            String temp1 = buf2.toString();
            String[] t = temp1.split(":");
            String tkey = t[1];
            tkey = tkey.trim();
            String key = null;
            if (!tkey.equalsIgnoreCase("")) {
                if (tkey.length() == 4 && IDManipulation.isHEX(tkey)) {
                    key = tkey;
                }
            }
            File f = new File(fileName);
            if (f.exists()) {
                ds.storeFile(fileName, key);
            } else {
                System.out.println("[ERROR] FILE NOT FOUND!");
            }
        } else if ((command.startsWith("read")) || (command.startsWith("READ")) || (command.startsWith("Read"))) {
            DataStore ds = (DataStore) this.node;
            StringTokenizer tokenizedCommand = new StringTokenizer(command);
            StringBuffer buf = new StringBuffer();
            StringBuffer buf2 = new StringBuffer();
            for (int i = 0; tokenizedCommand.hasMoreTokens(); i++) {
                if (i == 0) {
                    tokenizedCommand.nextToken();
                } else if (i == 1) {
                    buf2.append(tokenizedCommand.nextToken());
                    buf2.append(" ");
                } else {
                    buf.append(tokenizedCommand.nextToken());
                    buf.append(" ");
                }
            }
            String temp = buf.toString();
            String fileName = temp.trim();

            String temp1 = buf2.toString();
            String[] t = temp1.split(":");
            String tkey = t[1];
            tkey = tkey.trim();
            String key = null;
            if (!tkey.equalsIgnoreCase("")) {
                if (tkey.length() == 4 && IDManipulation.isHEX(tkey)) {
                    key = tkey;
                }
            }
            ds.readFile(fileName, key);
        } else if (command.equalsIgnoreCase("list-nodes")) {
//            System.out.println("-listnodes-");
            DiscoveryNode discoveryNode = (DiscoveryNode) this.node;
            discoveryNode.listNodes();
        } else if (command.equalsIgnoreCase("leaf-set")) {
            PeerNode pn = (PeerNode) this.node;
            pn.printLeafset();
        } else if (command.equalsIgnoreCase("routing-table")) {
            PeerNode pn = (PeerNode) this.node;
            pn.printRoutingTable();
        } else if (command.equalsIgnoreCase("list-files")) {
            PeerNode pn = (PeerNode) this.node;
            pn.printFileNames();
        } else {
            System.out.println("[ERROR] Invalid command!");
        }

    }
}
