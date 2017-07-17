/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cs555.nodes;

import cs555.routing.RoutingEntry;
import cs555.transport.TCPConnection;
import cs555.transport.TCPSender;
import cs555.util.InteractiveCommandParser;
import cs555.util.Protocol;
import cs555.wireformats.DNSendsRandomNode;
import cs555.wireformats.DNSendsRandomNodeToDS;
import cs555.wireformats.DNSendsRandomNodeToDSRead;
import cs555.wireformats.DNSendsRegistrationStatus;
import cs555.wireformats.DSRequestsRandomNode;
import cs555.wireformats.DSRequestsRandomNodeRead;
import cs555.wireformats.EventFactory;
import cs555.wireformats.PNSendsACK;
import cs555.wireformats.PNSendsCloseToDN;
import cs555.wireformats.PNSendsRegistration;
import cs555.wireformats.PNSendsRegistrationWConflict;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author priyankb
 */
public class DiscoveryNode extends Thread implements Node {

    private static DiscoveryNode discoveryNode;
//    public Map<String, TCPConnection> tempCache = new HashMap();
//    public Map<Integer, TCPConnection> connectionCache = new HashMap();
//    public Map<Integer, Integer> listeningPortCache = new HashMap<>();
    public Queue<PNSendsRegistration> registrationQueue = new LinkedList<>();
    public Logger logger = Logger.getLogger(getClass().getName());
    private String nodeIP;
    private int nodePort;
    private Map<String, RoutingEntry> dataStructure = new TreeMap<>();
    private QueueManager queueManager;

    public static DiscoveryNode getDiscoveryNode() {
        return discoveryNode;
    }

    private DiscoveryNode(int port) {
        try {
            this.nodePort = port;
            this.nodeIP = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
    }

    public static void main(String[] args) throws IOException {
        int port = Protocol.PORT;
        if (args.length == 1) {
            port = Integer.parseInt(args[0]);
        }

        discoveryNode = new DiscoveryNode(port);
        discoveryNode.start(port);
    }

    private void start(int port) throws IOException {
        this.queueManager = new QueueManager();
        Thread qm = new Thread(queueManager);
        qm.start();
        InteractiveCommandParser interactiveCommandParser = new InteractiveCommandParser(this);
        Thread commandThread = new Thread(interactiveCommandParser);
        commandThread.start();
        ServerSocket controllerSocket = new ServerSocket(port);
//        logger.log(Level.INFO, "DISCOVERYNODE STARTED ON : " + nodeIP + ":" + nodePort);
        System.out.println("[INFO] DISCOVERYNODE STARTED ON : " + InetAddress.getLocalHost().getHostName() + " : " + nodeIP + ":" + nodePort);
        while (true) {
            Socket s = controllerSocket.accept();
            TCPConnection conn = new TCPConnection(s, this);
//            addtoTempCache(conn);
        }
    }

    @Override
    public void onEvent(byte[] data, Socket s) throws IOException {
        try {
            EventFactory eventFactory = EventFactory.getInstance();
            switch (data[0]) {
                case Protocol.PN_SENDS_REGISTRATION:
                    PNSendsRegistration registration = new PNSendsRegistration(data);
                    synchronized (registrationQueue) {
                        registrationQueue.add(registration);
                    }
                    synchronized (queueManager.initial) {
                        queueManager.initial.notify();
                    }
                    break;

                case Protocol.PN_SENDS_REGISTRATION_W_CONFLICT:

                    PNSendsRegistrationWConflict registrationWConflict = new PNSendsRegistrationWConflict(data);
                    RoutingEntry peerWConflict = registrationWConflict.getNewPeer();
                    String nodeWConflictID = peerWConflict.getNodeID();
                    String nodeWConflictIP = peerWConflict.getIp();
                    int nodeWConflictPort = peerWConflict.getPort();
                    System.out.println("[INFO] CONFLICTED NODE REQUESTED AGAIN W ID : " + nodeWConflictID);
                    boolean exits;
                    synchronized (dataStructure) {
                        exits = dataStructure.containsKey(nodeWConflictID);
                    }
                    if (exits) {
//                        logger.log(Level.INFO, "CONFLICT in PEER ID : " + nodeID);
                        System.out.println("[INFO] CONFLICT in PEER ID : " + nodeWConflictID);
                    }
                    DNSendsRegistrationStatus status;
                    status = (DNSendsRegistrationStatus) eventFactory.createEvent(Protocol.DN_SENDS_REGISTRATION_STATUS);
                    status.setId(nodeWConflictID);

                    status.setExits(exits);

                    Socket soc = new Socket(nodeWConflictIP, nodeWConflictPort);
                    TCPSender sender = new TCPSender(soc);
                    sender.sendData(status.getByte());

                    //Send Random Node
                    if (!exits) {

                        String randomNodeID = getRandomNode();
//                        logger.log(Level.INFO, "Sending Random PEER : " + randomNodeID + " to PEER : " + nodeID);
                        System.out.println("[INFO] Sending Random PEER : " + randomNodeID + " to PEER : " + nodeWConflictID);
                        RoutingEntry randomNodeInfo = null;
                        DNSendsRandomNode sendsRandomNode = (DNSendsRandomNode) eventFactory.createEvent(Protocol.DN_SENDS_RANDOMNODE);
                        if (randomNodeID != null) {
                            synchronized (dataStructure) {
                                randomNodeInfo = dataStructure.get(randomNodeID);
                            }
                            sendsRandomNode.setRandomNode(randomNodeInfo);
                        }

                        Socket soc1 = new Socket(nodeWConflictIP, nodeWConflictPort);
                        TCPSender sender1 = new TCPSender(soc1);
                        sender1.sendData(sendsRandomNode.getByte());

                    }
                    break;

                case Protocol.PN_SENDS_ACK:
                    PNSendsACK ack = new PNSendsACK(data);
                    RoutingEntry newNode = ack.getPn();
                    String newNodeID = newNode.getNodeID();
//                    logger.log(Level.INFO, "ACK received from PEER : " + newNodeID);
                    System.out.println("[INFO] NEW PEER : " + newNode);
                    synchronized (dataStructure) {
                        dataStructure.put(newNodeID, newNode);
                    }
                    synchronized (queueManager.response) {
                        queueManager.response.notify();
                    }
                    break;

                case Protocol.PN_SENDS_CLOSE_TO_DN:
                    PNSendsCloseToDN close = new PNSendsCloseToDN(data);
                    String nodeID = close.getId();
                    boolean flag = close.isInitialized();
//                    logger.log(Level.INFO, "Remove request received from PEER : " + nodeID);
                    System.out.println("[INFO] PEER LEFT: " + nodeID);
                    if (flag) {
                        //remove node
                        synchronized (dataStructure) {
                            dataStructure.remove(nodeID);
                        }
                    } else {
                        synchronized (queueManager.response) {
                            queueManager.response.notify();
                        }
                    }
                    break;

                case Protocol.DS_REQUESTS_RANDOMNODE:
                    DSRequestsRandomNode dsRequestsRandomNode = new DSRequestsRandomNode(data);
                    String randomNodeID = getRandomNode();
//                        logger.log(Level.INFO, "Sending Random PEER : " + randomNodeID + " to PEER : " + nodeID);
                    RoutingEntry randomNodeInfo = null;
                    if (randomNodeID != null) {
                        synchronized (dataStructure) {
                            randomNodeInfo = dataStructure.get(randomNodeID);
                        }
                    }
                    DNSendsRandomNodeToDS toDS = (DNSendsRandomNodeToDS) eventFactory.createEvent(Protocol.DN_SENDS_RANDOMNODE_TO_DS);
                    toDS.setFileName(dsRequestsRandomNode.getFileName());
                    toDS.setKey(dsRequestsRandomNode.getKey());
                    toDS.setRandomNode(randomNodeInfo);

                    Socket dnsrd = new Socket(dsRequestsRandomNode.getIp(), dsRequestsRandomNode.getPort());
                    new TCPSender(dnsrd).sendData(toDS.getByte());

                    break;

                case Protocol.DS_REQUESTS_RANDOMNODE_READ:
                    DSRequestsRandomNodeRead dsRequestsRandomNodeRead = new DSRequestsRandomNodeRead(data);

                    String randomNodeIDread = getRandomNode();
//                        logger.log(Level.INFO, "Sending Random PEER : " + randomNodeID + " to PEER : " + nodeID);
                    RoutingEntry randomNodeInforead = null;
                    if (randomNodeIDread != null) {
                        synchronized (dataStructure) {
                            randomNodeInforead = dataStructure.get(randomNodeIDread);
                        }
                    }
                    DNSendsRandomNodeToDSRead toDSr = (DNSendsRandomNodeToDSRead) eventFactory.createEvent(Protocol.DN_SENDS_RANDOMNODE_TO_DS_READ);
                    toDSr.setFileName(dsRequestsRandomNodeRead.getFileName());
                    toDSr.setKey(dsRequestsRandomNodeRead.getKey());
                    toDSr.setRandomNode(randomNodeInforead);

                    Socket dnsrdr = new Socket(dsRequestsRandomNodeRead.getIp(), dsRequestsRandomNodeRead.getPort());
                    new TCPSender(dnsrdr).sendData(toDSr.getByte());

                    break;

            }

        } catch (IOException ex) {
            logger.log(Level.SEVERE, "IOException while trying to contact", ex);
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Exception while sending data", ex);
        }
    }

//    private void addtoTempCache(TCPConnection connection) {
//        int idbyPort = connection.getSocket().getPort();
//        String idbyIP = connection.getSocket().getInetAddress().getHostAddress();
//        String id = idbyIP + ":" + idbyPort;
////        System.out.println("-ret- " + id);
//        synchronized (tempCache) {
//            this.tempCache.put(id, connection);
//        }
//    }
    private String getRandomNode() {
        Set<String> nodesSet;
        String randomNode = null;
        synchronized (dataStructure) {
            nodesSet = dataStructure.keySet();
        }
        int totalNodes = nodesSet.size();
        if (totalNodes > 0) {
            List<String> nodes = new ArrayList(nodesSet);
            Random random = new Random();
            int randomNodeIndex = random.nextInt(totalNodes);
            randomNode = nodes.get(randomNodeIndex);
        }
        return randomNode;
    }

    public void listNodes() {
        System.out.println("[INFO] ---------------------------------------------------------");
        System.out.println("[INFO] ----------------------LIST OF NODES----------------------");
        int totNodes = 0;
        synchronized (dataStructure) {

            for (Map.Entry<String, RoutingEntry> entrySet : dataStructure.entrySet()) {
                RoutingEntry peer = entrySet.getValue();
                System.out.println("[INFO] " + peer);
                totNodes++;
            }
        }
        System.out.println("[INFO] ---------------------------------------------------------");
        System.out.println("[INFO] Total Nodes: " + totNodes);
        System.out.println("[INFO] ---------------------------------------------------------");
    }

    private class QueueManager implements Runnable {

        Object initial = new Object();
        Object response = new Object();
        EventFactory eventFactory = EventFactory.getInstance();
        Logger logger = Logger.getLogger(getClass().getName());

        private QueueManager() {

        }

        @Override
        public void run() {
            PNSendsRegistration registration;
            while (true) {

                try {
                    synchronized (registrationQueue) {
                        registration = registrationQueue.poll();
                    }
                    if (registration == null) {
                        synchronized (initial) {
                            try {
                                initial.wait();
                            } catch (InterruptedException ex) {
                                logger.log(Level.SEVERE, "Exception while waiting", ex);
                            }
                        }
                        synchronized (registrationQueue) {
                            registration = registrationQueue.poll();
                        }
                    }
                    //Process Registration
                    RoutingEntry newNode = registration.getNewPeer();
                    String nodeID = newNode.getNodeID();
                    String nodeIP = newNode.getIp();
                    int nodePort = newNode.getPort();
//                    logger.log(Level.INFO, "Processing registration request of PEER : " + nodeID);
                    System.out.println("[INFO] Processing registration request of PEER : " + nodeID);
                    boolean exits;
                    synchronized (dataStructure) {
                        exits = dataStructure.containsKey(nodeID);
                    }
                    if (exits) {
//                        logger.log(Level.INFO, "CONFLICT in PEER ID : " + nodeID);
                        System.out.println("[INFO] CONFLICT in PEER ID : " + nodeID);
                    }
                    DNSendsRegistrationStatus status;
                    status = (DNSendsRegistrationStatus) eventFactory.createEvent(Protocol.DN_SENDS_REGISTRATION_STATUS);
                    status.setId(nodeID);

                    status.setExits(exits);

                    Socket soc = new Socket(nodeIP, nodePort);
                    TCPSender sender = new TCPSender(soc);
                    sender.sendData(status.getByte());

                    //Send Random Node
                    if (!exits) {

                        String randomNodeID = getRandomNode();
//                        logger.log(Level.INFO, "Sending Random PEER : " + randomNodeID + " to PEER : " + nodeID);
                        System.out.println("[INFO] Sending Random PEER : " + randomNodeID + " to PEER : " + nodeID);
                        RoutingEntry randomNodeInfo = null;
                        DNSendsRandomNode sendsRandomNode = (DNSendsRandomNode) eventFactory.createEvent(Protocol.DN_SENDS_RANDOMNODE);
                        if (randomNodeID != null) {
                            synchronized (dataStructure) {
                                randomNodeInfo = dataStructure.get(randomNodeID);
                            }
                            sendsRandomNode.setRandomNode(randomNodeInfo);
                        }

                        Socket soc1 = new Socket(nodeIP, nodePort);
                        TCPSender sender1 = new TCPSender(soc1);
                        sender1.sendData(sendsRandomNode.getByte());

                    }
                    synchronized (response) {
                        response.wait();
                    }

                } catch (IOException ex) {
                    logger.log(Level.SEVERE, "IOException while trying to contact PEER", ex);
                } catch (InterruptedException ex) {
                    logger.log(Level.SEVERE, "Exception while waiting", ex);
                } catch (Exception ex) {
                    logger.log(Level.SEVERE, "Exception while sending data to PEER", ex);
                }
            }
        }
    }
}
