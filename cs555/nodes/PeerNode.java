/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cs555.nodes;

import cs555.routing.RoutingEntry;
import cs555.routing.RoutingTable;
import cs555.transport.TCPConnection;
import cs555.transport.TCPSender;
import cs555.util.IDManipulation;
import cs555.util.InteractiveCommandParser;
import cs555.util.NodeInfo;
import cs555.util.Protocol;
import cs555.wireformats.DNSendsRandomNode;
import cs555.wireformats.DNSendsRegistrationStatus;
import cs555.wireformats.DSSendsFile;
import cs555.wireformats.DSSendsKey;
import cs555.wireformats.DSSendsKeyRead;
import cs555.wireformats.EventFactory;
import cs555.wireformats.PNDestinationSendsInfoToPN;
import cs555.wireformats.PNForwardsFile;
import cs555.wireformats.PNForwardsKey;
import cs555.wireformats.PNForwardsKeyRead;
import cs555.wireformats.PNForwardsRequest;
import cs555.wireformats.PNNotifiesLeafNodes;
import cs555.wireformats.PNNotifiesNodesInRoutingtable;
import cs555.wireformats.PNRepliesToDS;
import cs555.wireformats.PNSendsACK;
import cs555.wireformats.PNSendsCloseToDN;
import cs555.wireformats.PNSendsCloseToLeafNodes;
import cs555.wireformats.PNSendsCloseToRoutingTable;
import cs555.wireformats.PNSendsFailureToDS;
import cs555.wireformats.PNSendsFileToDS;
import cs555.wireformats.PNSendsFileTransferComplete;
import cs555.wireformats.PNSendsJoinRequest;
import cs555.wireformats.PNSendsRegistration;
import cs555.wireformats.PNSendsRegistrationWConflict;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author priyankb
 */
public class PeerNode implements Node {

    private NodeInfo discoveryNode;
//    private String nickname;
    private String nodeID;
//    private String nodeIP;
//    private int nodePort;
    private static PeerNode peerNode;
    private ServerSocket pnServerSocket;
    private volatile boolean initialized = false;
    private volatile boolean leftTransferComplete = false;
    private volatile boolean rightTransferComplete = false;
    private Object transferCompleteLock = new Object();
    private RoutingEntry leftLeaf;
    private RoutingEntry rightLeaf;
    private RoutingEntry me;
    private RoutingTable routingTable;
    private List<FileInfo> storedFiles = new LinkedList<>();

    private Logger logger = Logger.getLogger(getClass().getName());

    private PeerNode(String ip, int port, int myport, String myID) {
        myID = myID.toUpperCase();
        this.discoveryNode = new NodeInfo(ip, port);
//        this.nodeID = myID;
//        this.nodePort = myport;
        try {
//            this.nickname = InetAddress.getLocalHost().getHostName();
//            this.nodeIP = InetAddress.getLocalHost().getHostAddress();
            this.me = new RoutingEntry(myID, InetAddress.getLocalHost().getHostName(), InetAddress.getLocalHost().getHostAddress(), myport);
        } catch (UnknownHostException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
        try {
            this.pnServerSocket = new ServerSocket(myport);
        } catch (IOException ex) {
//            logger.log(Level.SEVERE, "PEERNODE CANNOT START ON PORT " + myport);
            System.out.println("[INFO] PEERNODE CANNOT START ON PORT " + myport);
        }
//        System.out.println("[INFO] " + pnServerSocket.getLocalSocketAddress() + ":" + this.pnServerSocket.getLocalPort());
    }

    public static PeerNode getPeerNode() {
        return peerNode;
    }

    public static void main(String[] args) {
//            Logger.getLogger(PeerNode.class.getName()).log(Level.SEVERE, null, ex);

        String hostName = Protocol.HOSTNAME;
        int port = Protocol.PORT;
        int myport = Protocol.PEER_PORT;
        String myID = generateRandomID();

        if (args.length == 3) {
            hostName = args[0];
            port = Integer.parseInt(args[1]);
            myport = Integer.parseInt(args[2]);
        } else if (args.length == 4) {
            hostName = args[0];
            port = Integer.parseInt(args[1]);
            myport = Integer.parseInt(args[2]);
            myID = args[3];
            myID = myID.toUpperCase();
        }
        peerNode = new PeerNode(hostName, port, myport, myID);
        peerNode.start();
    }

    private void start() {
        sendRegistration();
        InteractiveCommandParser interactiveCommandParser = new InteractiveCommandParser(this);
        Thread commandThread = new Thread(interactiveCommandParser);
        commandThread.start();
//        logger.log(Level.INFO, "PEERNODE STARTED ON : " + me.getNickname() + " : " + me.getIp() + ":" + me.getPort());
        System.out.println("[INFO] PEERNODE STARTED ON : " + me.getNickname() + " : " + me.getIp() + ":" + me.getPort());
        while (true) {
            try {
                Socket csClient = this.pnServerSocket.accept();
                TCPConnection localTCPConnection = new TCPConnection(csClient, this);
            } catch (IOException ex) {
                logger.log(Level.SEVERE, "PEERNODE CANNOT ACCEPT CONNECTIONS", ex);
            }
        }
    }

    @Override
    public void onEvent(byte[] data, Socket s) {
        try {
            EventFactory eventFactory = EventFactory.getInstance();
            switch (data[0]) {

                case Protocol.DN_SENDS_REGISTRATION_STATUS:
                    DNSendsRegistrationStatus status = new DNSendsRegistrationStatus(data);
                    if (status.isExits()) {

//                        logger.log(Level.INFO, "Conflict on ID : " + me.getNodeID());
//                        logger.log(Level.INFO, "Generating new ID");
                        System.out.println("[INFO] Conflict on ID : " + me.getNodeID());
                        System.out.println("[INFO] Generating new ID");
                        String newID = generateRandomID();
                        String oldID = me.getNodeID();
                        while (newID.equals(oldID)) {
                            newID = generateRandomID();
                        }
                        me.setNodeID(newID);
                        sendRegistrationWConflict();
                    } else {
//                        me.setNodeID(nodeID);
                        nodeID = me.getNodeID();
                        System.out.println("[INFO] Registration Successfull with ID : " + me.getNodeID() + "\n");
//                        sendACK();
                    }
                    break;

                case Protocol.DN_SENDS_RANDOMNODE:

                    DNSendsRandomNode dnSendsRandomNode = new DNSendsRandomNode(data);
                    RoutingEntry randomNode = dnSendsRandomNode.getRandomNode();

                    if (randomNode != null) {
                        String randomNodeID = randomNode.getNodeID();
//                        logger.log(Level.INFO, "RandomNode Received with ID : " + randomNodeID);
                        System.out.println("[INFO] Random PEER : " + randomNodeID);
                        String randomNodeIP = randomNode.getIp();
                        int randomNodePort = randomNode.getPort();
//                        logger.log(Level.INFO, "Sending Join request to PEER : " + randomNodeID);
//                        System.out.println("[INFO] Sending Join request to PEER : " + randomNodeID);
                        PNSendsJoinRequest joinRequest = (PNSendsJoinRequest) eventFactory.createEvent(Protocol.PN_SENDS_JOIN_REQUEST);
                        joinRequest.setPn(me);
                        joinRequest.setTrace(nodeID);
                        joinRequest.setHopCount(0);
                        Socket sr = new Socket(randomNodeIP, randomNodePort);
                        new TCPSender(sr).sendData(joinRequest.getByte());
                    } else {
                        System.out.println("[INFO] FIRST-NODE");
                        routingTable = new RoutingTable(nodeID);
                        sendACK();
                    }
                    break;

                case Protocol.PN_SENDS_JOIN_REQUEST:

                    PNSendsJoinRequest receivedJoinRequest = new PNSendsJoinRequest(data);
                    RoutingEntry newNode = receivedJoinRequest.getPn();
                    String trace = receivedJoinRequest.getTrace();
                    trace = trace + " - " + nodeID;
                    int hopCount = receivedJoinRequest.getHopCount();
                    hopCount++;
                    String newNodeID = newNode.getNodeID();
                    RoutingTable tempRT = new RoutingTable(newNodeID);
//                    logger.log(Level.INFO, "Received Join request from PEER : " + newNodeID);
                    System.out.println("");
                    System.out.println("[INFO] Join request from PEER : " + newNodeID);
                    populateTempRT(newNodeID, tempRT);
                    RoutingEntry nextNode = lookup(newNodeID);

                    if (nextNode.getNodeID().equalsIgnoreCase(newNodeID)) {
                        int index = IDManipulation.matchPrefix(nextNode.getNodeID(), nodeID);
                        String lookupStringc = nextNode.getNodeID().substring(0, index + 1);
                        synchronized (routingTable) {
                            Map<String, RoutingEntry> row = routingTable.routingtable.get(index);
                            RoutingEntry p = row.get(lookupStringc);
                            if (p.getNodeID().equals(nextNode.getNodeID())) {
                                row.put(lookupStringc, null);
                            }
                        }
                        nextNode = lookup(newNodeID);
                    }

                    if (nextNode.getIp().equals(newNode.getIp()) && nextNode.getPort() == newNode.getPort()) {
                        int index = IDManipulation.matchPrefix(nextNode.getNodeID(), nodeID);
                        String lookupStringc = nextNode.getNodeID().substring(0, index + 1);
                        synchronized (routingTable) {
                            Map<String, RoutingEntry> row = routingTable.routingtable.get(index);
                            RoutingEntry p = row.get(lookupStringc);
                            if (p.getNodeID().equals(nextNode.getNodeID())) {
                                row.put(lookupStringc, null);
                            }
                        }
                        nextNode = lookup(newNodeID);
                    }

                    if (nextNode.getNodeID().equals(nodeID)) {
//                        logger.log(Level.INFO, "Destination Found PEER : " + nodeID);
                        System.out.println("[INFO] Destination Found PEER : " + nodeID);
                        PNDestinationSendsInfoToPN infoToPN = (PNDestinationSendsInfoToPN) eventFactory.createEvent(Protocol.PN_DEST_SENDS_INFO_TO_PN);
                        infoToPN.setMe(me);
                        if (leftLeaf == null) {
                            infoToPN.setLeftLeaf(me);
                        } else {
                            synchronized (leftLeaf) {
                                infoToPN.setLeftLeaf(leftLeaf);
                            }
                        }
                        if (rightLeaf == null) {
                            infoToPN.setLeftLeaf(me);
                        } else {
                            synchronized (rightLeaf) {
                                infoToPN.setRightLeaf(rightLeaf);
                            }
                        }
                        infoToPN.setTempRT(tempRT);
                        infoToPN.setTrace(trace);
                        infoToPN.setHopCount(hopCount);
                        Socket srnn = new Socket(newNode.getIp(), newNode.getPort());
                        new TCPSender(srnn).sendData(infoToPN.getByte());
                    } else {
//                        logger.log(Level.INFO, "Forwarding request to PEER : " + nextNode.getNodeID());
                        System.out.println("[INFO] Next PEER : " + nextNode.getNodeID());
                        PNForwardsRequest forwardsRequest = (PNForwardsRequest) eventFactory.createEvent(Protocol.PN_FORWARDS_REQUEST);
                        forwardsRequest.setRequestingNode(newNode);
                        forwardsRequest.setTempRT(tempRT);
                        forwardsRequest.setTrace(trace);
                        forwardsRequest.setHopCount(hopCount);
                        Socket sfr = new Socket(nextNode.getIp(), nextNode.getPort());
                        new TCPSender(sfr).sendData(forwardsRequest.getByte());
                    }
                    break;

                case Protocol.PN_FORWARDS_REQUEST:

                    PNForwardsRequest pnfr = new PNForwardsRequest(data);

                    RoutingEntry requestingNode = pnfr.getRequestingNode();
                    RoutingTable oldTempRT = pnfr.getTempRT();
                    String forwardsTrace = pnfr.getTrace();
                    forwardsTrace = forwardsTrace + " - " + nodeID;
                    int forwardsHopCount = pnfr.getHopCount();
                    forwardsHopCount++;
                    String requestingNodeID = requestingNode.getNodeID();
//                    logger.log(Level.INFO, "Received Join request for PEER : " + requestingNodeID);
                    System.out.println("[INFO] Join request from PEER : " + requestingNodeID);
                    System.out.println("[INFO] Current HopCount : " + forwardsHopCount);
                    populateTempRT(requestingNodeID, oldTempRT);
                    RoutingEntry nextnNode = lookup(requestingNodeID);

                    if (nextnNode.getNodeID().equalsIgnoreCase(requestingNodeID)) {
                        int index = IDManipulation.matchPrefix(nextnNode.getNodeID(), nodeID);
                        String lookupStringc = nextnNode.getNodeID().substring(0, index + 1);
                        synchronized (routingTable) {
                            Map<String, RoutingEntry> row = routingTable.routingtable.get(index);
                            RoutingEntry p = row.get(lookupStringc);
                            if (p.getNodeID().equals(nextnNode.getNodeID())) {
                                row.put(lookupStringc, null);
                            }
                        }
                        nextnNode = lookup(requestingNodeID);
                    }
                    if (nextnNode.getIp().equals(requestingNode.getIp()) && nextnNode.getPort() == requestingNode.getPort()) {
                        int index = IDManipulation.matchPrefix(nextnNode.getNodeID(), nodeID);
                        String lookupStringc = nextnNode.getNodeID().substring(0, index + 1);
                        synchronized (routingTable) {
                            Map<String, RoutingEntry> row = routingTable.routingtable.get(index);
                            RoutingEntry p = row.get(lookupStringc);
                            if (p.getNodeID().equals(nextnNode.getNodeID())) {
                                row.put(lookupStringc, null);
                            }
                        }
                        nextnNode = lookup(requestingNodeID);
                    }

                    if (nextnNode.getNodeID().equals(nodeID)) {
//                        logger.log(Level.INFO, "Destination Found PEER : " + nodeID);
                        System.out.println("[INFO] Destination Found PEER : " + nodeID);
                        PNDestinationSendsInfoToPN infoToPN = (PNDestinationSendsInfoToPN) eventFactory.createEvent(Protocol.PN_DEST_SENDS_INFO_TO_PN);
                        infoToPN.setMe(me);
                        infoToPN.setTrace(forwardsTrace);
                        infoToPN.setHopCount(forwardsHopCount);
                        synchronized (leftLeaf) {
                            infoToPN.setLeftLeaf(leftLeaf);
                        }
                        synchronized (rightLeaf) {
                            infoToPN.setRightLeaf(rightLeaf);
                        }
                        infoToPN.setTempRT(oldTempRT);
                        Socket srnn = new Socket(requestingNode.getIp(), requestingNode.getPort());
                        new TCPSender(srnn).sendData(infoToPN.getByte());
                    } else {
//                        logger.log(Level.INFO, "Forwarding request to PEER : " + nextnNode.getNodeID());
                        System.out.println("[INFO] Next PEER : " + nextnNode.getNodeID());
                        PNForwardsRequest forwardsRequest = (PNForwardsRequest) eventFactory.createEvent(Protocol.PN_FORWARDS_REQUEST);
                        forwardsRequest.setRequestingNode(requestingNode);
                        forwardsRequest.setTempRT(oldTempRT);
                        forwardsRequest.setTrace(forwardsTrace);
                        forwardsRequest.setHopCount(forwardsHopCount);
                        Socket sfr = new Socket(nextnNode.getIp(), nextnNode.getPort());
                        new TCPSender(sfr).sendData(forwardsRequest.getByte());
                    }

                    break;

                case Protocol.PN_DEST_SENDS_INFO_TO_PN:
                    PNDestinationSendsInfoToPN infoToPN = new PNDestinationSendsInfoToPN(data);

                    //populate routing table
//                    synchronized (routingTable) {
                    routingTable = infoToPN.getTempRT();
//                    }
                    synchronized (routingTable) {
                        for (Map.Entry<Integer, Map<String, RoutingEntry>> entrySet : routingTable.routingtable.entrySet()) {
                            int key = entrySet.getKey();
                            Map<String, RoutingEntry> row = entrySet.getValue();
                            for (Map.Entry<String, RoutingEntry> entrySet1 : row.entrySet()) {
                                String handle = entrySet1.getKey();
                                RoutingEntry re = entrySet1.getValue();
                                if (handle.substring(0, key + 1).equalsIgnoreCase(nodeID.substring(0, key + 1))) {
                                    row.put(handle, null);
                                }
                            }
                        }
                    }
                    RoutingEntry L = infoToPN.getLeftLeaf();
                    RoutingEntry R = infoToPN.getRightLeaf();
                    RoutingEntry Dest = infoToPN.getMe();
                    String finalTrace = infoToPN.getTrace() + " - " + nodeID;
                    int finalHopCount = infoToPN.getHopCount();
                    finalHopCount++;

//                    logger.log(Level.INFO, "Info received from Destination with ID : " + Dest.getNodeID());
                    System.out.println("");
                    System.out.println("[INFO] Info received from Destination with ID : " + Dest.getNodeID());
                    System.out.println("[INFO] HOPCOUNT: " + finalHopCount);
                    System.out.println("[INFO] TRACE: " + finalTrace);

                    //populate leafset
                    if (L != null && R != null && Dest != null) {
//                        IDManipulation.DistanceDirection ddL = IDManipulation.getDistanceAndDirection(nodeID, L.getNodeID());
//                        IDManipulation.DistanceDirection ddR = IDManipulation.getDistanceAndDirection(nodeID, R.getNodeID());
//                        IDManipulation.DistanceDirection ddA = IDManipulation.getDistanceAndDirection(nodeID, Dest.getNodeID());

                        int destLeft = IDManipulation.countDistanceInDirection(Protocol.DIRECTION.LEFT, Dest.getNodeID(), L.getNodeID());
                        int destRight = IDManipulation.countDistanceInDirection(Protocol.DIRECTION.RIGHT, Dest.getNodeID(), R.getNodeID());
                        int destMeLeft = IDManipulation.countDistanceInDirection(Protocol.DIRECTION.LEFT, Dest.getNodeID(), nodeID);
                        int destMeRight = IDManipulation.countDistanceInDirection(Protocol.DIRECTION.RIGHT, Dest.getNodeID(), nodeID);

//                        if (ddA.getDirection() == Protocol.DIRECTION.RIGHT && ddL.getDirection() == Protocol.DIRECTION.LEFT) {
                        if (destMeLeft < destLeft) {
                            leftLeaf = L;
                            rightLeaf = Dest;
//                        } else if (ddA.getDirection() == Protocol.DIRECTION.LEFT && ddR.getDirection() == Protocol.DIRECTION.RIGHT) {
                        } else if (destMeRight < destRight) {
                            leftLeaf = Dest;
                            rightLeaf = R;
                        }

                    } else if (Dest != null) {
                        leftLeaf = Dest;
                        rightLeaf = Dest;
                    }

                    //notifyleafnodes
//                    logger.log(Level.INFO, "Notifying Leaf Nodes");
//                    System.out.println("[INFO] Notifying Leaf Nodes");
                    PNNotifiesLeafNodes notifyRightLeaf = (PNNotifiesLeafNodes) eventFactory.createEvent(Protocol.PN_NOTIFIES_LEAFNODES);
                    notifyRightLeaf.setDirection(Protocol.DIRECTION.LEFT);
                    notifyRightLeaf.setLeafNode(me);
                    synchronized (rightLeaf) {
                        Socket snrl = new Socket(rightLeaf.getIp(), rightLeaf.getPort());
                        new TCPSender(snrl).sendData(notifyRightLeaf.getByte());
                    }

                    PNNotifiesLeafNodes notifyLeftLeaf = (PNNotifiesLeafNodes) eventFactory.createEvent(Protocol.PN_NOTIFIES_LEAFNODES);
                    notifyLeftLeaf.setDirection(Protocol.DIRECTION.RIGHT);
                    notifyLeftLeaf.setLeafNode(me);
                    synchronized (leftLeaf) {
                        Socket snll = new Socket(leftLeaf.getIp(), leftLeaf.getPort());
                        new TCPSender(snll).sendData(notifyLeftLeaf.getByte());
                    }

                    //notify nodes in routingtable
//                    logger.log(Level.INFO, "Notifying Routing Entries");
//                    System.out.println("[INFO] Notifying Routing Entries");
                    PNNotifiesNodesInRoutingtable notifyRoutingtable = (PNNotifiesNodesInRoutingtable) eventFactory.createEvent(Protocol.PN_NOTIFIES_NODES_IN_ROUTINGTABLE);
                    notifyRoutingtable.setNewPeer(me);
                    synchronized (routingTable) {
                        for (Map.Entry<Integer, Map<String, RoutingEntry>> entrySet : routingTable.routingtable.entrySet()) {
                            Integer key = entrySet.getKey();
                            Map<String, RoutingEntry> row = entrySet.getValue();
                            for (Map.Entry<String, RoutingEntry> entrySet1 : row.entrySet()) {
                                String handle = entrySet1.getKey();
                                RoutingEntry peer = entrySet1.getValue();

                                if (peer != null) {
                                    try {
                                        Socket snrt = new Socket(peer.getIp(), peer.getPort());
                                        new TCPSender(snrt).sendData(notifyRoutingtable.getByte());
                                    } catch (Exception ex) {
                                        row.put(handle, null);
                                    }
                                }

                            }
                        }
                    }

                    System.out.println("");
                    printRoutingTable();
                    System.out.println("");
                    printLeafset();
                    System.out.println("");
                    //send ack to DN
//                    sendACK();
                    break;

                case Protocol.PN_NOTIFIES_LEAFNODES:
                    PNNotifiesLeafNodes notifiesLeafNodes = new PNNotifiesLeafNodes(data);

                    switch (notifiesLeafNodes.getDirection()) {
                        case Protocol.DIRECTION.LEFT:
//                            logger.log(Level.INFO, "Updating LeftLeaf to : " + notifiesLeafNodes.getLeafNode().getNodeID());
//                            System.out.println("[INFO] Updating LeftLeaf to : " + notifiesLeafNodes.getLeafNode().getNodeID());
                            if (leftLeaf != null) {
                                synchronized (leftLeaf) {
                                    this.leftLeaf = notifiesLeafNodes.getLeafNode();
                                }
                            } else {
                                this.leftLeaf = notifiesLeafNodes.getLeafNode();
                            }
                            synchronized (storedFiles) {
                                for (Iterator<FileInfo> iterator = storedFiles.iterator(); iterator.hasNext();) {
                                    FileInfo finfo = iterator.next();
                                    String file = finfo.getFileName();
                                    String key = finfo.getKey();
                                    synchronized (leftLeaf) {
                                        String near = IDManipulation.nearerNode(key, leftLeaf.getNodeID(), nodeID);
                                        if (near.equalsIgnoreCase(leftLeaf.getNodeID())) {
                                            PNForwardsFile forwardFile = (PNForwardsFile) eventFactory.createEvent(Protocol.PN_FORWARDS_FILE);
                                            forwardFile.setFileName(file);
                                            forwardFile.setLeafNode(me);
                                            forwardFile.setKey(key);
                                            File fts = new File(Protocol.FILE_PREFIX + file);
                                            if (fts.exists()) {
                                                FileInputStream fin = new FileInputStream(fts);
                                                byte[] fileData = new byte[(int) fts.length()];
                                                fin.read(fileData);
                                                fin.close();

                                                forwardFile.setFileData(fileData);
                                            }
                                            Socket sll = new Socket(leftLeaf.getIp(), leftLeaf.getPort());
                                            new TCPSender(sll).sendData(forwardFile.getByte());
                                            storedFiles.remove(finfo);
                                        }
                                    }
                                }
                            }
                            PNSendsFileTransferComplete transferCompleter = (PNSendsFileTransferComplete) eventFactory.createEvent(Protocol.PN_SENDS_FILE_TRANSFER_COMPLETE);
                            transferCompleter.setDirection(Protocol.DIRECTION.RIGHT);
                            synchronized (leftLeaf) {
                                Socket ftcr = new Socket(leftLeaf.getIp(), leftLeaf.getPort());
                                new TCPSender(ftcr).sendData(transferCompleter.getByte());
                            }
                            break;

                        case Protocol.DIRECTION.RIGHT:
//                            logger.log(Level.INFO, "Updating RightLeaf to : " + notifiesLeafNodes.getLeafNode().getNodeID());
//                            System.out.println("[INFO] Updating RightLeaf to : " + notifiesLeafNodes.getLeafNode().getNodeID());
                            if (rightLeaf != null) {
                                synchronized (rightLeaf) {
                                    this.rightLeaf = notifiesLeafNodes.getLeafNode();
                                }
                            } else {
                                this.rightLeaf = notifiesLeafNodes.getLeafNode();
                            }
                            synchronized (storedFiles) {
                                for (Iterator<FileInfo> iterator = storedFiles.iterator(); iterator.hasNext();) {
                                    FileInfo finfo = iterator.next();
                                    String file = finfo.getFileName();
                                    String key = finfo.getKey();
                                    synchronized (rightLeaf) {
                                        String near = IDManipulation.nearerNode(key, rightLeaf.getNodeID(), nodeID);
                                        if (near.equalsIgnoreCase(rightLeaf.getNodeID())) {
                                            PNForwardsFile forwardFile = (PNForwardsFile) eventFactory.createEvent(Protocol.PN_FORWARDS_FILE);
                                            forwardFile.setFileName(file);
                                            forwardFile.setLeafNode(me);
                                            forwardFile.setKey(key);
                                            File fts = new File(Protocol.FILE_PREFIX + file);
                                            if (fts.exists()) {
                                                FileInputStream fin = new FileInputStream(fts);
                                                byte[] fileData = new byte[(int) fts.length()];
                                                fin.read(fileData);
                                                fin.close();

                                                forwardFile.setFileData(fileData);
                                            }
                                            Socket sll = new Socket(rightLeaf.getIp(), rightLeaf.getPort());
                                            new TCPSender(sll).sendData(forwardFile.getByte());
                                            storedFiles.remove(finfo);
                                        }
                                    }
                                }
                            }
                            PNSendsFileTransferComplete transferCompletel = (PNSendsFileTransferComplete) eventFactory.createEvent(Protocol.PN_SENDS_FILE_TRANSFER_COMPLETE);
                            transferCompletel.setDirection(Protocol.DIRECTION.LEFT);
                            synchronized (rightLeaf) {
                                Socket ftcr = new Socket(rightLeaf.getIp(), rightLeaf.getPort());
                                new TCPSender(ftcr).sendData(transferCompletel.getByte());
                            }
                            break;
                    }
                    System.out.println("[INFO] Leafset updated");
                    printLeafset();
                    RoutingEntry leaf = notifiesLeafNodes.getLeafNode();
                    String leafID = leaf.getNodeID();
                    int commonSequence = IDManipulation.matchPrefix(nodeID, leafID);
                    String lookupHandle = leafID.substring(0, commonSequence + 1);
                    synchronized (routingTable) {
                        Map<String, RoutingEntry> row = routingTable.routingtable.get(commonSequence);
                        RoutingEntry oldPeer = row.get(lookupHandle);
                        if (oldPeer == null) {
                            row.put(lookupHandle, leaf);
                            System.out.println("[INFO] Routing table updated");
                            printRoutingTable();
                        }
                    }

                    break;

                case Protocol.PN_SENDS_FILE_TRANSFER_COMPLETE:
                    PNSendsFileTransferComplete fileTransferComplete = new PNSendsFileTransferComplete(data);
                    synchronized (transferCompleteLock) {
                        switch (fileTransferComplete.getDirection()) {
                            case Protocol.DIRECTION.LEFT:
                                leftTransferComplete = true;
                                break;

                            case Protocol.DIRECTION.RIGHT:
                                rightTransferComplete = true;
                                break;
                        }
                        if (leftTransferComplete && rightTransferComplete) {
                            sendACK();
                        }
                    }

                    break;

                case Protocol.PN_NOTIFIES_NODES_IN_ROUTINGTABLE:
                    PNNotifiesNodesInRoutingtable inRoutingtable = new PNNotifiesNodesInRoutingtable(data);
                    RoutingEntry newPeer = inRoutingtable.getNewPeer();
                    String newPeerID = newPeer.getNodeID();
                    int commonSubSequence = IDManipulation.matchPrefix(nodeID, newPeer.getNodeID());
                    String lookupString = newPeerID.substring(0, commonSubSequence + 1);
                    synchronized (routingTable) {
                        Map<String, RoutingEntry> row = routingTable.routingtable.get(commonSubSequence);
                        RoutingEntry oldPeer = row.get(lookupString);
                        if (oldPeer == null) {
                            row.put(lookupString, newPeer);
                        }
                    }
                    break;

                case Protocol.DS_SENDS_KEY:
                    System.out.println("[INFO] Store request recieved from DataStore");
                    DSSendsKey dssk = new DSSendsKey(data);
                    String filename = dssk.getFileName();
                    String key = dssk.getKey();
                    String dsIP = dssk.getDsIP();
                    int dsPort = dssk.getDsPort();
                    String traceds = dssk.getTrace();
                    traceds = traceds + " - " + nodeID;
                    int hopc = dssk.getHopCount();
                    hopc++;

                    RoutingEntry next = lookup(key);
                    if (next.getNodeID().equalsIgnoreCase(nodeID)) {
                        System.out.println("[INFO] Store request Destination Found");
                        PNRepliesToDS toDS = (PNRepliesToDS) eventFactory.createEvent(Protocol.PN_REPLIES_TO_DS);
                        toDS.setDestPeer(me);
                        toDS.setFileName(filename);
                        toDS.setKey(key);
                        toDS.setTrace(traceds);
                        toDS.setHopCount(hopc);

                        Socket sds = new Socket(dsIP, dsPort);
                        new TCPSender(sds).sendData(toDS.getByte());

                    } else {
                        System.out.println("[INFO] Forwarding Store request to Peer: " + next);
                        PNForwardsKey forwardKey = (PNForwardsKey) eventFactory.createEvent(Protocol.PN_FORWARDS_KEY);
                        forwardKey.setDsIP(dsIP);
                        forwardKey.setDsPort(dsPort);
                        forwardKey.setFileName(filename);
                        forwardKey.setKey(key);
                        forwardKey.setTrace(traceds);
                        forwardKey.setHopCount(hopc);

                        Socket nexts = new Socket(next.getIp(), next.getPort());
                        new TCPSender(nexts).sendData(forwardKey.getByte());
                    }
                    break;

                case Protocol.PN_FORWARDS_KEY:
                    System.out.println("[INFO] Store request recieved");
                    PNForwardsKey forwardsKey = new PNForwardsKey(data);
                    String forwardedFilename = forwardsKey.getFileName();
                    String forwardedKey = forwardsKey.getKey();
                    String forwardedDSIP = forwardsKey.getDsIP();
                    int forwardedDSPort = forwardsKey.getDsPort();
                    String tracedsf = forwardsKey.getTrace();
                    tracedsf = tracedsf + " - " + nodeID;
                    int hopcf = forwardsKey.getHopCount();
                    hopcf++;

                    RoutingEntry nextn = lookup(forwardedKey);

                    if (nextn.getNodeID().equalsIgnoreCase(nodeID)) {
                        System.out.println("[INFO] Store request Destination Found");
                        PNRepliesToDS againtoDS = (PNRepliesToDS) eventFactory.createEvent(Protocol.PN_REPLIES_TO_DS);
                        againtoDS.setDestPeer(me);
                        againtoDS.setFileName(forwardedFilename);
                        againtoDS.setKey(forwardedKey);
                        againtoDS.setTrace(tracedsf);
                        againtoDS.setHopCount(hopcf);

                        Socket asds = new Socket(forwardedDSIP, forwardedDSPort);
                        new TCPSender(asds).sendData(againtoDS.getByte());

                    } else {
                        System.out.println("[INFO] Forwarding Store request to Peer: " + nextn);
                        PNForwardsKey againForwardKey = (PNForwardsKey) eventFactory.createEvent(Protocol.PN_FORWARDS_KEY);
                        againForwardKey.setDsIP(forwardedDSIP);
                        againForwardKey.setDsPort(forwardedDSPort);
                        againForwardKey.setFileName(forwardedFilename);
                        againForwardKey.setKey(forwardedKey);
                        againForwardKey.setTrace(tracedsf);
                        againForwardKey.setHopCount(hopcf);

                        Socket nextns = new Socket(nextn.getIp(), nextn.getPort());
                        new TCPSender(nextns).sendData(againForwardKey.getByte());
                    }
                    break;

                case Protocol.DS_SENDS_KEY_READ:
                    System.out.println("[INFO] Read request recieved from DataStore");
                    DSSendsKeyRead dsskr = new DSSendsKeyRead(data);
                    String filenamer = dsskr.getFileName();
                    String keyr = dsskr.getKey();
                    String dsipr = dsskr.getDsIP();
                    int dsportr = dsskr.getDsPort();
                    String tracer = dsskr.getTrace();
                    tracer = tracer + " - " + nodeID;
                    int hopr = dsskr.getHopCount();
                    hopr++;

                    RoutingEntry nextr = lookup(keyr);
                    if (nextr.getNodeID().equals(nodeID)) {
                        System.out.println("[INFO] Read request Destination Found");
                        sendFile(filenamer, keyr, dsipr, dsportr, tracer, hopr);
                    } else {
                        System.out.println("[INFO] Forwarding Read request to Peer: " + nextr);
                        PNForwardsKeyRead forwardsKeyRead = (PNForwardsKeyRead) eventFactory.createEvent(Protocol.PN_FORWARDS_KEY_READ);
                        forwardsKeyRead.setFileName(filenamer);
                        forwardsKeyRead.setKey(keyr);
                        forwardsKeyRead.setDsIP(dsipr);
                        forwardsKeyRead.setDsPort(dsportr);
                        forwardsKeyRead.setTrace(tracer);
                        forwardsKeyRead.setHopCount(hopr);

                        Socket nextsr = new Socket(nextr.getIp(), nextr.getPort());
                        new TCPSender(nextsr).sendData(forwardsKeyRead.getByte());
                    }

                    break;

                case Protocol.PN_FORWARDS_KEY_READ:
                    PNForwardsKeyRead forwardsKeyRead = new PNForwardsKeyRead(data);
                    String forwardedFilenamer = forwardsKeyRead.getFileName();
                    String forwardedKeyr = forwardsKeyRead.getKey();
                    String forwardedDSIPr = forwardsKeyRead.getDsIP();
                    int forwardedDSPortr = forwardsKeyRead.getDsPort();
                    String forwardedtracer = forwardsKeyRead.getTrace();
                    forwardedtracer = forwardedtracer + " - " + nodeID;
                    int forwardedhopCount = forwardsKeyRead.getHopCount();
                    forwardedhopCount++;

                    RoutingEntry nextrn = lookup(forwardedKeyr);
                    if (nextrn.getNodeID().equals(nodeID)) {
                        System.out.println("[INFO] Read request Destination Found");
                        sendFile(forwardedFilenamer, forwardedKeyr, forwardedDSIPr, forwardedDSPortr, forwardedtracer, forwardedhopCount);
                    } else {
                        System.out.println("[INFO] Forwarding Read request to Peer: " + nextrn);
                        PNForwardsKeyRead againForwardsKeyRead = (PNForwardsKeyRead) eventFactory.createEvent(Protocol.PN_FORWARDS_KEY_READ);
                        againForwardsKeyRead.setFileName(forwardedFilenamer);
                        againForwardsKeyRead.setKey(forwardedKeyr);
                        againForwardsKeyRead.setDsIP(forwardedDSIPr);
                        againForwardsKeyRead.setDsPort(forwardedDSPortr);
                        againForwardsKeyRead.setTrace(forwardedtracer);
                        againForwardsKeyRead.setHopCount(forwardedhopCount);

                        Socket nextnr = new Socket(nextrn.getIp(), nextrn.getPort());
                        new TCPSender(nextnr).sendData(againForwardsKeyRead.getByte());
                    }

                    break;

                case Protocol.DS_SENDS_FILE:
                    System.out.println("[INFO] File received from DataStore");
                    DSSendsFile newfile = new DSSendsFile(data);
                    String fileToStore = newfile.getFileName();
                    byte[] fileData = newfile.getFileData();
                    String fileKey = newfile.getKey();
                    saveFile(fileToStore, fileData, fileKey);

                    break;

                case Protocol.PN_FORWARDS_FILE:
                    PNForwardsFile pnff = new PNForwardsFile(data);
                    String forwardedFile = pnff.getFileName();
                    byte[] forwardedFileData = pnff.getFileData();
                    String forwardedKeyn = pnff.getKey();
                    RoutingEntry peer = pnff.getLeafNode();
//                    logger.log(Level.INFO, "File forwarded from PEER : " + peer.getNodeID());
                    System.out.println("[INFO] File forwarded from PEER : " + peer.getNodeID());
                    saveFile(forwardedFile, forwardedFileData, forwardedKeyn);

                    break;

                case Protocol.PN_SENDS_CLOSE_TO_LEAFNODES:
                    PNSendsCloseToLeafNodes closeToLeafNodes = new PNSendsCloseToLeafNodes(data);

                    switch (closeToLeafNodes.getDirection()) {
                        case Protocol.DIRECTION.LEFT:
//                            logger.log(Level.INFO, "Updating LeftLeaf to : " + closeToLeafNodes.getLeafNode().getNodeID());
                            System.out.println("[INFO] Updating LeftLeaf to : " + closeToLeafNodes.getLeafNode().getNodeID());
                            synchronized (leftLeaf) {
                                this.leftLeaf = closeToLeafNodes.getLeafNode();
                            }
                            break;

                        case Protocol.DIRECTION.RIGHT:
//                            logger.log(Level.INFO, "Updating RightLeaf to : " + closeToLeafNodes.getLeafNode().getNodeID());
                            System.out.println("[INFO] Updating RightLeaf to : " + closeToLeafNodes.getLeafNode().getNodeID());
                            synchronized (rightLeaf) {
                                this.rightLeaf = closeToLeafNodes.getLeafNode();
                            }
                            break;
                    }
                    break;

                case Protocol.PN_SENDS_CLOSE_TO_ROUTINGTABLE:
                    PNSendsCloseToRoutingTable closeToRoutingTable = new PNSendsCloseToRoutingTable(data);
                    RoutingEntry closingPeer = closeToRoutingTable.getPeerClose();
                    int index = IDManipulation.matchPrefix(closingPeer.getNodeID(), nodeID);
                    String lookupStringc = closingPeer.getNodeID().substring(0, index + 1);
                    synchronized (routingTable) {
                        Map<String, RoutingEntry> row = routingTable.routingtable.get(index);
                        RoutingEntry p = row.get(lookupStringc);
                        if (p.getNodeID().equals(closingPeer.getNodeID())) {
                            row.put(lookupStringc, null);
                        }
                    }
                    break;
            }
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "IOException while trying to contact", ex);
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Exception while sending data", ex);
        }
    }

    private void sendRegistration() {
        try {
//            logger.log(Level.INFO, "Sending registration request with ID : " + me.getNodeID());
            System.out.println("[INFO] Sending registration request with ID : " + me.getNodeID());
            EventFactory eventFactory = EventFactory.getInstance();
            PNSendsRegistration registration = (PNSendsRegistration) eventFactory.createEvent(Protocol.PN_SENDS_REGISTRATION);
            Socket sr = new Socket(discoveryNode.getIp(), discoveryNode.getPort());
            registration.setNewPeer(me);
            new TCPSender(sr).sendData(registration.getByte());
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "PEERNODE CANNOT CONNECT TO DISCOVERYNODE", ex);
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Exception while sending data", ex);
        }
    }

    private void sendRegistrationWConflict() {
        try {
//            logger.log(Level.INFO, "Sending registration request with ID : " + me.getNodeID());
            System.out.println("[INFO] Sending registration request with ID : " + me.getNodeID());
            EventFactory eventFactory = EventFactory.getInstance();
            PNSendsRegistrationWConflict registration = (PNSendsRegistrationWConflict) eventFactory.createEvent(Protocol.PN_SENDS_REGISTRATION_W_CONFLICT);
            Socket sr = new Socket(discoveryNode.getIp(), discoveryNode.getPort());
            registration.setNewPeer(me);
            new TCPSender(sr).sendData(registration.getByte());
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "PEERNODE CANNOT CONNECT TO DISCOVERYNODE", ex);
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Exception while sending data", ex);
        }
    }

    private void sendACK() {
        this.initialized = true;
        try {
//            logger.log(Level.INFO, "Sending ACK to DiscoveryNode");
            System.out.println("[INFO] Initialization Complete!\n");
            EventFactory eventFactory = EventFactory.getInstance();
            PNSendsACK sendACK = (PNSendsACK) eventFactory.createEvent(Protocol.PN_SENDS_ACK);
            Socket sr = new Socket(discoveryNode.getIp(), discoveryNode.getPort());
            sendACK.setPn(me);
            new TCPSender(sr).sendData(sendACK.getByte());
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "PEERNODE CANNOT CONNECT TO DISCOVERYNODE", ex);
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Exception while sending data", ex);
        }
    }

    private static String generateRandomID() {
        Long l = System.nanoTime();
        String hex = Long.toHexString(l);
        String randomID = hex.substring(hex.length() - 4);
        randomID = randomID.toUpperCase();
        return randomID;
    }

    private RoutingEntry lookup(String newNodeID) {
        RoutingEntry nextNode = null;
        if (leftLeaf == null || rightLeaf == null) {
            return me;
        }
        String nextNodeID;
//        IDManipulation.DistanceDirection ddleft;
//        IDManipulation.DistanceDirection ddright;
//        IDManipulation.DistanceDirection ddOwn;
        int ownLeft;
        int ownRight;
        int ownNewL;
        int ownNewR;
        synchronized (leftLeaf) {
//            ddleft = IDManipulation.getDistanceAndDirection(newNodeID, leftLeaf.getNodeID());
            ownLeft = IDManipulation.countDistanceInDirection(Protocol.DIRECTION.LEFT, nodeID, leftLeaf.getNodeID());
        }
        synchronized (rightLeaf) {
//            ddright = IDManipulation.getDistanceAndDirection(newNodeID, rightLeaf.getNodeID());
            ownRight = IDManipulation.countDistanceInDirection(Protocol.DIRECTION.RIGHT, nodeID, rightLeaf.getNodeID());
        }
//        ddOwn = IDManipulation.getDistanceAndDirection(newNodeID, nodeID);
        ownNewL = IDManipulation.countDistanceInDirection(Protocol.DIRECTION.LEFT, nodeID, newNodeID);
        ownNewR = IDManipulation.countDistanceInDirection(Protocol.DIRECTION.RIGHT, nodeID, newNodeID);

//        if (ddleft.getDirection() == Protocol.DIRECTION.LEFT && ddOwn.getDirection() == Protocol.DIRECTION.RIGHT) {
        if (ownNewL < ownLeft) {
            synchronized (leftLeaf) {
                nextNodeID = IDManipulation.nearerNode(newNodeID, nodeID, leftLeaf.getNodeID());
            }
            if (nextNodeID.equals(nodeID)) {
                nextNode = this.me;
            } else {
                synchronized (leftLeaf) {
                    nextNode = leftLeaf;
                }
            }
//        } else if (ddright.getDirection() == Protocol.DIRECTION.RIGHT && ddOwn.getDirection() == Protocol.DIRECTION.LEFT) {
        } else if (ownNewR < ownRight) {
            synchronized (rightLeaf) {
                nextNodeID = IDManipulation.nearerNode(newNodeID, nodeID, rightLeaf.getNodeID());
            }
            if (nextNodeID.equals(nodeID)) {
                nextNode = this.me;
            } else {
                synchronized (rightLeaf) {
                    nextNode = rightLeaf;
                }
            }
        } else {
            int index = IDManipulation.matchPrefix(newNodeID, nodeID);
            String lookupString = newNodeID.substring(0, index + 1);
            synchronized (routingTable) {
                Map<String, RoutingEntry> row = routingTable.routingtable.get(index);
                nextNode = row.get(lookupString);

                //check if nextnode is reachable, if no then remove it form routing table
                if (nextNode != null) {
                    try {
                        Socket trySocket = new Socket(nextNode.getIp(), nextNode.getPort());
                    } catch (IOException ex) {
                        row.put(lookupString, null);
                        nextNode = null;
                    }
                }
            }
        }
        if (nextNode == null) {
            nextNode = this.me;
            nextNodeID = nodeID;
            synchronized (routingTable) {
                for (Map.Entry<Integer, Map<String, RoutingEntry>> entrySet : routingTable.routingtable.entrySet()) {
                    Map<String, RoutingEntry> row = entrySet.getValue();

                    for (Map.Entry<String, RoutingEntry> entrySet1 : row.entrySet()) {
                        String handle = entrySet1.getKey();
                        RoutingEntry peer = entrySet1.getValue();
                        if (peer != null) {
                            try {
                                Socket trySocket = new Socket(peer.getIp(), peer.getPort());
                            } catch (IOException ex) {
                                row.put(handle, null);
                                continue;
                            }
                            String peerID = peer.getNodeID();
                            nextNodeID = IDManipulation.nearerNode(newNodeID, nextNodeID, peerID);

                            if (nextNodeID.equals(peerID)) {
                                nextNode = peer;
                            }
                        }
                    }
                }
            }
            synchronized (leftLeaf) {
                nextNodeID = IDManipulation.nearerNode(newNodeID, nextNodeID, leftLeaf.getNodeID());
            }
            synchronized (rightLeaf) {
                nextNodeID = IDManipulation.nearerNode(newNodeID, nextNodeID, rightLeaf.getNodeID());
            }
            synchronized (leftLeaf) {
                if ((nextNodeID.equals(leftLeaf.getNodeID()))) {
                    nextNode = leftLeaf;
                }
            }
            synchronized (rightLeaf) {
                if ((nextNodeID.equals(rightLeaf.getNodeID()))) {
                    nextNode = rightLeaf;
                }
            }
            if (nextNodeID.equals(nodeID)) {
                nextNode = me;
            }
        }
        return nextNode;
    }

    private void populateTempRT(String newNodeID, RoutingTable tempRT) {
        int commonSubSequence = IDManipulation.matchPrefix(nodeID, newNodeID);

        synchronized (routingTable) {
            for (int i = 0; i <= commonSubSequence; i++) {
                Map<String, RoutingEntry> row = routingTable.routingtable.get(i);
                Map<String, RoutingEntry> rowTemp = tempRT.routingtable.get(i);
                for (Map.Entry<String, RoutingEntry> entrySet : row.entrySet()) {
                    String handle = entrySet.getKey();
                    RoutingEntry node = entrySet.getValue();

                    if (node != null) {
                        if (rowTemp.get(handle) == null) {
                            rowTemp.put(handle, node);
                        }
                    }
                }
                String sub = nodeID.substring(0, i + 1);
                if (rowTemp.get(sub) == null) {
                    rowTemp.put(sub, me);
                }
            }
        }
    }

    public void closeNode() {
        try {
            EventFactory eventFactory = EventFactory.getInstance();
//            logger.log(Level.INFO, "Notifying DISCOVERY NODE");
            System.out.println("[INFO] Notifying DISCOVERY NODE");
            PNSendsCloseToDN close = (PNSendsCloseToDN) eventFactory.createEvent(Protocol.PN_SENDS_CLOSE_TO_DN);
            close.setInitialized(initialized);
            close.setId(nodeID);

            Socket sr = new Socket(discoveryNode.getIp(), discoveryNode.getPort());
            new TCPSender(sr).sendData(close.getByte());

            if (initialized) {
                RoutingEntry ll;
                RoutingEntry rl;
                synchronized (leftLeaf) {
                    ll = leftLeaf;
                }
                synchronized (rightLeaf) {
                    rl = rightLeaf;
                }
                if (ll != null && rl != null) {

                    //noderemoval mesages to leafnodes 
//                    logger.log(Level.INFO, "Notifying Leaf Nodes");
                    System.out.println("[INFO] Notifying Leaf Nodes");
                    PNSendsCloseToLeafNodes closeToLeft = (PNSendsCloseToLeafNodes) eventFactory.createEvent(Protocol.PN_SENDS_CLOSE_TO_LEAFNODES);
                    closeToLeft.setDirection(Protocol.DIRECTION.RIGHT);
                    closeToLeft.setLeafNode(rl);
                    Socket cleft = new Socket(ll.getIp(), ll.getPort());
                    new TCPSender(cleft).sendData(closeToLeft.getByte());

                    PNSendsCloseToLeafNodes closeToRight = (PNSendsCloseToLeafNodes) eventFactory.createEvent(Protocol.PN_SENDS_CLOSE_TO_LEAFNODES);
                    closeToRight.setDirection(Protocol.DIRECTION.LEFT);
                    closeToRight.setLeafNode(ll);
                    Socket cright = new Socket(rl.getIp(), rl.getPort());
                    new TCPSender(cright).sendData(closeToRight.getByte());

                    //filetransfer
//                    logger.log(Level.INFO, "Transfering Files");
                    System.out.println("[INFO] Transfering Files");
                    synchronized (storedFiles) {
                        for (Iterator<FileInfo> iterator = storedFiles.iterator(); iterator.hasNext();) {
                            FileInfo finfo = iterator.next();
                            String file = finfo.getFileName();
                            String key = finfo.getKey();

                            RoutingEntry nearNode = null;
//                            String key = IDManipulation.filenametoKey(file);
                            String near = IDManipulation.nearerNode(key, ll.getNodeID(), rl.getNodeID());
                            if (near.equalsIgnoreCase(ll.getNodeID())) {
                                nearNode = ll;
                            } else if (near.equalsIgnoreCase(rl.getNodeID())) {
                                nearNode = rl;
                            }

                            PNForwardsFile forwardFile = (PNForwardsFile) eventFactory.createEvent(Protocol.PN_FORWARDS_FILE);
                            forwardFile.setFileName(file);
                            forwardFile.setLeafNode(me);
                            forwardFile.setKey(key);
                            File fts = new File(Protocol.FILE_PREFIX + file);
                            if (fts.exists()) {
                                FileInputStream fin = new FileInputStream(fts);
                                byte[] fileData = new byte[(int) fts.length()];
                                fin.read(fileData);
                                fin.close();

                                forwardFile.setFileData(fileData);
                            }
                            Socket sll = new Socket(nearNode.getIp(), nearNode.getPort());
                            new TCPSender(sll).sendData(forwardFile.getByte());
                            storedFiles.remove(finfo);

                        }
                    }

                    //noderemoval messages to routing table
//                    logger.log(Level.INFO, "Notifying Routing table");
                    System.out.println("[INFO] Notifying Routing table");
                    PNSendsCloseToRoutingTable closeToRoutingTable = (PNSendsCloseToRoutingTable) eventFactory.createEvent(Protocol.PN_SENDS_CLOSE_TO_ROUTINGTABLE);
                    closeToRoutingTable.setPeerClose(me);

                    synchronized (routingTable) {
                        for (Map.Entry<Integer, Map<String, RoutingEntry>> entrySet : routingTable.routingtable.entrySet()) {
                            Integer key = entrySet.getKey();
                            Map<String, RoutingEntry> row = entrySet.getValue();

                            for (Map.Entry<String, RoutingEntry> entrySet1 : row.entrySet()) {
                                String handle = entrySet1.getKey();
                                RoutingEntry peer = entrySet1.getValue();
                                if (peer != null) {
                                    try {
                                        Socket srt = new Socket(peer.getIp(), peer.getPort());
                                        new TCPSender(srt).sendData(closeToRoutingTable.getByte());
                                    } catch (Exception ex) {

                                    }
                                }
                            }

                        }
                    }
                }
            }

        } catch (IOException ex) {
            logger.log(Level.SEVERE, "PEERNODE CANNOT CONNECT", ex);
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Exception while sending data", ex);
        }
        System.exit(0);
    }

    public void printLeafset() {
        if (leftLeaf != null) {
            synchronized (leftLeaf) {
                System.out.println("[INFO] LEFTLEAF  : " + leftLeaf);
            }
        } else {
            System.out.println("[INFO] LEFTLEAF  : null");
        }
        if (rightLeaf != null) {
            synchronized (rightLeaf) {
                System.out.println("[INFO] RIGHTLEAF : " + rightLeaf);
            }
        } else {
            System.out.println("[INFO] RIGHTLEAF : null");
        }
    }

    public void printRoutingTable() {
        routingTable.printRoutingTable();
    }

    public void printFileNames() {
        System.out.println("[INFO] ---Stored Files---");
        synchronized (storedFiles) {
            for (FileInfo storedFile : storedFiles) {
                System.out.println("[INFO] Key: " + storedFile.getKey() + " - Filename: " + storedFile.getFileName());
            }
        }
    }

    private void sendFailureMessageToDS(String fileName, String info, String dsIP, int dsPort) {
        EventFactory eventFactory = EventFactory.getInstance();
        try {
            PNSendsFailureToDS failureToDS = (PNSendsFailureToDS) eventFactory.createEvent(Protocol.PN_SENDS_FAILURE_TO_DS);
            failureToDS.setFileName(fileName);
            failureToDS.setInfo(info);
            failureToDS.setPeer(me);

            Socket ds = new Socket(dsIP, dsPort);
            new TCPSender(ds).sendData(failureToDS.getByte());

        } catch (IOException ex) {
            Logger.getLogger(PeerNode.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(PeerNode.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    private void sendFile(String filenamer, String key, String dsipr, int dsportr, String trace, int hopCount) {
        FileSender fileSender = new FileSender(filenamer, key, dsipr, dsportr, trace, hopCount);
        Thread fileSenderThread = new Thread(fileSender);
        fileSenderThread.start();
    }

    private void saveFile(String fileName, byte[] fileData, String key) {
        FileSaver fileSaver = new FileSaver(fileName, fileData, key);
        Thread fileSaverThread = new Thread(fileSaver);
        fileSaverThread.start();
    }

    class FileSaver implements Runnable {

        private String fileName;
        private byte[] fileData;
        private String key;

        private FileSaver(String fileName, byte[] fileData, String key) {
            this.fileName = fileName;
            this.fileData = fileData;
            this.key = key;
        }

        @Override
        public void run() {
            try {
                synchronized (storedFiles) {
                    boolean contains = false;
                    for (Iterator<FileInfo> iterator = storedFiles.iterator(); iterator.hasNext();) {
                        FileInfo next = iterator.next();
                        if (next.getFileName().equals(fileName) && next.getKey().equals(key)) {
                            contains = true;
                        }
                    }

                    if (!contains) {
                        storedFiles.add(new FileInfo(fileName, key));
                    }
                }
                String fn = Protocol.FILE_PREFIX + fileName;
                String path = fn.substring(0, fn.lastIndexOf("/") + 1);

                File newFile = new File(path);
                if (!newFile.exists()) {
                    newFile.mkdirs();
                }

                File actualFile = new File(fn);

                FileOutputStream fout = new FileOutputStream(actualFile);
                fout.write(fileData, 0, fileData.length);
                fout.close();

            } catch (IOException ex) {
                logger.log(Level.SEVERE, "ERROR WHILE SAVING FILE", ex);
            }
        }

    }

    class FileSender implements Runnable {

        private String fileName;
        private String key;
        private String dsIP;
        private int dsPort;
        private String trace;
        private int hopCount;
        private byte[] fileData;
        private String notFound = "[ERROR] FILE NOT FOUND!";
        EventFactory eventFactory = EventFactory.getInstance();

        private FileSender(String filename, String key, String dsIP, int dsPort, String trace, int hopCount) {
            this.fileName = filename;
            this.key = key;
            this.dsIP = dsIP;
            this.dsPort = dsPort;
            this.trace = trace;
            this.hopCount = hopCount;
        }

        @Override
        public void run() {
            try {
                synchronized (storedFiles) {
//                    FileInfo fi = new FileInfo(fileName, key);
                    boolean contains = false;
                    for (Iterator<FileInfo> iterator = storedFiles.iterator(); iterator.hasNext();) {
                        FileInfo next = iterator.next();
                        if (next.getFileName().equals(fileName) && next.getKey().equals(key)) {
                            contains = true;
                        }
                    }

                    if (!contains) {
                        System.out.println(notFound);
                        sendFailureMessageToDS(fileName, "FILE NOT FOUND!", dsIP, dsPort);
                    } else {
                        String fn = Protocol.FILE_PREFIX + fileName;

                        File ftr = new File(fn);
                        if (ftr.exists()) {
                            FileInputStream fin = new FileInputStream(ftr);
                            fileData = new byte[(int) ftr.length()];
                            fin.read(fileData);

                            PNSendsFileToDS pnSendsFileToDS = (PNSendsFileToDS) eventFactory.createEvent(Protocol.PN_SENDS_FILE_TO_DS);
                            pnSendsFileToDS.setFileName(fileName);
                            pnSendsFileToDS.setFileData(fileData);
                            pnSendsFileToDS.setTrace(trace);
                            pnSendsFileToDS.setHopCount(hopCount);

                            Socket s = new Socket(dsIP, dsPort);
                            new TCPSender(s).sendData(pnSendsFileToDS.getByte());

                        } else {
                            System.out.println(notFound);
                            sendFailureMessageToDS(fileName, "FILE NOT FOUND!", dsIP, dsPort);
                        }

                    }
                }
            } catch (FileNotFoundException ex) {
                logger.log(Level.SEVERE, null, ex);
                sendFailureMessageToDS(fileName, "FILE NOT FOUND!", dsIP, dsPort);
            } catch (IOException ex) {
                logger.log(Level.SEVERE, null, ex);
            } catch (Exception ex) {
                logger.log(Level.SEVERE, null, ex);
            }
        }

    }

    class FileInfo {

        String fileName;
        String key;

        public FileInfo(String fileName, String key) {
            this.fileName = fileName;
            this.key = key;
        }

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

    }
}
