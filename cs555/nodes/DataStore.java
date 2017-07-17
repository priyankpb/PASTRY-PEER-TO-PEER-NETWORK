/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cs555.nodes;

import cs555.routing.RoutingEntry;
import cs555.transport.TCPConnection;
import cs555.transport.TCPSender;
import cs555.util.IDManipulation;
import cs555.util.InteractiveCommandParser;
import cs555.util.NodeInfo;
import cs555.util.Protocol;
import cs555.wireformats.DNSendsRandomNodeToDS;
import cs555.wireformats.DNSendsRandomNodeToDSRead;
import cs555.wireformats.DSRequestsRandomNode;
import cs555.wireformats.DSRequestsRandomNodeRead;
import cs555.wireformats.DSSendsFile;
import cs555.wireformats.DSSendsKey;
import cs555.wireformats.DSSendsKeyRead;
import cs555.wireformats.EventFactory;
import cs555.wireformats.PNRepliesToDS;
import cs555.wireformats.PNSendsFailureToDS;
import cs555.wireformats.PNSendsFileToDS;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author priyankb
 */
public class DataStore implements Node {

    private NodeInfo discoveryNode;
    private NodeInfo me;
    private static DataStore dataStore;
    private ServerSocket dsServerSocket;
    private Logger logger = Logger.getLogger(getClass().getName());

    private DataStore(String ip, int port, int myport) {

        this.discoveryNode = new NodeInfo(ip, port);

        try {
            String nickname = InetAddress.getLocalHost().getHostName();
            String nodeIP = InetAddress.getLocalHost().getHostAddress();
            int nodePort = myport;

            me = new NodeInfo(nickname, nodeIP, nodePort);
        } catch (UnknownHostException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
        try {
            this.dsServerSocket = new ServerSocket(myport);
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "DATASTORE CANNOT START ON PORT " + myport);
        }
//        System.out.println("[INFO] " + pnServerSocket.getLocalSocketAddress() + ":" + this.pnServerSocket.getLocalPort());
    }

    public static void main(String[] args) {
//            Logger.getLogger(PeerNode.class.getName()).log(Level.SEVERE, null, ex);

        String hostName = Protocol.HOSTNAME;
        int port = Protocol.PORT;
        int myport = 5000;

        if (args.length == 3) {
            hostName = args[0];
            port = Integer.parseInt(args[1]);
            myport = Integer.parseInt(args[2]);
        }
        dataStore = new DataStore(hostName, port, myport);
        dataStore.start();
    }

    private void start() {
//        sendRegistration();
        InteractiveCommandParser interactiveCommandParser = new InteractiveCommandParser(this);
        Thread commandThread = new Thread(interactiveCommandParser);
        commandThread.start();
//        logger.log(Level.INFO, "DATASTORE STARTED ON : " + me.getIp() + ":" + me.getPort());
        System.out.println("[INFO] DATASTORE STARTED ON : " + me.getIp() + ":" + me.getPort());
        while (true) {
            try {
                Socket dsClient = this.dsServerSocket.accept();
                TCPConnection localTCPConnection = new TCPConnection(dsClient, this);
            } catch (IOException ex) {
                logger.log(Level.SEVERE, "DATASTORE CANNOT ACCEPT CONNECTIONS", ex);
            }
        }
    }

    @Override
    public void onEvent(byte[] data, Socket s) {
        try {
            EventFactory eventFactory = EventFactory.getInstance();
            switch (data[0]) {
                case Protocol.DN_SENDS_RANDOMNODE_TO_DS:
                    DNSendsRandomNodeToDS dnsrntds = new DNSendsRandomNodeToDS(data);
                    String fileName = dnsrntds.getFileName();
                    RoutingEntry randomNode = dnsrntds.getRandomNode();
                    if (randomNode == null) {
//                        logger.log(Level.WARNING, "PASTRY DOES NOT HAVE ANY PEER NODE! " + "\n" + "PLEASE ADD SOME NODES FIRST");
                        System.out.println("[INFO] PASTRY DOES NOT HAVE ANY PEER NODE!");
                        System.out.println("[INFO] PLEASE ADD SOME NODES FIRST");
                        break;
                    }
//                    logger.log(Level.INFO, "RandomNode received from DiscoveryNode : " + randomNode);
                    System.out.println("[INFO] RandomNode received from DiscoveryNode : " + randomNode);
                    String key = dnsrntds.getKey();
                    if (key == null || key.equalsIgnoreCase("")) {
                        key = IDManipulation.filenametoKey(fileName);
                    }
                    System.out.println("[INFO] Storing File with KEY: " + key);
                    DSSendsKey sendsKey = (DSSendsKey) eventFactory.createEvent(Protocol.DS_SENDS_KEY);
                    sendsKey.setFileName(fileName);
                    sendsKey.setKey(key);
                    sendsKey.setDsIP(me.getIp());
                    sendsKey.setDsPort(me.getPort());
                    sendsKey.setTrace("DataStore");
                    sendsKey.setHopCount(0);

                    Socket ssk = new Socket(randomNode.getIp(), randomNode.getPort());
                    new TCPSender(ssk).sendData(sendsKey.getByte());

                    break;

                case Protocol.DN_SENDS_RANDOMNODE_TO_DS_READ:
                    DNSendsRandomNodeToDSRead dnsrntdsr = new DNSendsRandomNodeToDSRead(data);
                    String fileNamer = dnsrntdsr.getFileName();
                    RoutingEntry randomNoder = dnsrntdsr.getRandomNode();
                    if (randomNoder == null) {
//                        logger.log(Level.WARNING, "PASTRY DOES NOT HAVE ANY PEER NODE! " + "\n" + "PLEASE ADD SOME NODES FIRST");
                        System.out.println("[INFO] PASTRY DOES NOT HAVE ANY PEER NODE! " + "\n" + "PLEASE ADD SOME NODES FIRST");
                        break;
                    }
//                    logger.log(Level.INFO, "RandomNode received from DiscoveryNode : " + randomNode);
                    System.out.println("[INFO] RandomNode received from DiscoveryNode : " + randomNoder);
                    String keyr = dnsrntdsr.getKey();
                    if (keyr == null || keyr.equalsIgnoreCase("")) {
                        keyr = IDManipulation.filenametoKey(fileNamer);
                    }

                    DSSendsKeyRead sendsKeyr = (DSSendsKeyRead) eventFactory.createEvent(Protocol.DS_SENDS_KEY_READ);
                    sendsKeyr.setFileName(fileNamer);
                    sendsKeyr.setKey(keyr);
                    sendsKeyr.setDsIP(me.getIp());
                    sendsKeyr.setDsPort(me.getPort());
                    sendsKeyr.setTrace("DataStore");
                    sendsKeyr.setHopCount(0);

                    Socket sskr = new Socket(randomNoder.getIp(), randomNoder.getPort());
                    new TCPSender(sskr).sendData(sendsKeyr.getByte());
                    break;

                case Protocol.PN_REPLIES_TO_DS:

                    PNRepliesToDS repliesToDS = new PNRepliesToDS(data);
                    String fileToStore = repliesToDS.getFileName();
                    String keyToStore = repliesToDS.getKey();
                    RoutingEntry destination = repliesToDS.getDestPeer();
                    String trace = repliesToDS.getTrace();
                    trace = trace + " - DataStore";
                    int hopCount = repliesToDS.getHopCount();
                    hopCount++;
                    System.out.println("[INFO] Destination : " + destination);
                    System.out.println("[INFO] Trace: " + trace);
                    System.out.println("[INFO] HopCount: " + hopCount);
                    File fileToSend = new File(fileToStore);
                    if (fileToSend.exists()) {
                        byte[] fileData = new byte[(int) fileToSend.length()];
                        FileInputStream fin = new FileInputStream(fileToSend);
                        fin.read(fileData);
                        fin.close();

                        DSSendsFile sendFile = (DSSendsFile) eventFactory.createEvent(Protocol.DS_SENDS_FILE);
                        sendFile.setFileName(fileToStore);
                        sendFile.setFileData(fileData);
                        sendFile.setKey(keyToStore);

                        Socket sd = new Socket(destination.getIp(), destination.getPort());
                        new TCPSender(sd).sendData(sendFile.getByte());
                    }
                    break;

                case Protocol.PN_SENDS_FILE_TO_DS:
                    PNSendsFileToDS fileToDS = new PNSendsFileToDS(data);
                    String filename = fileToDS.getFileName();
                    byte[] filedata = fileToDS.getFileData();
                    String tracer = fileToDS.getTrace();
                    tracer = tracer + " - DataStore";
                    int hopCountr = fileToDS.getHopCount();
                    hopCountr++;

                    System.out.println("[INFO] Read Trace: " + tracer);
                    System.out.println("[INFO] HopCount: " + hopCountr);
                    saveFile(filename, filedata);

                    break;

                case Protocol.PN_SENDS_FAILURE_TO_DS:
                    PNSendsFailureToDS pnSendsFailureToDS = new PNSendsFailureToDS(data);
                    String ffileName = pnSendsFailureToDS.getFileName();
                    String finfo = pnSendsFailureToDS.getInfo();
                    RoutingEntry fpeer = pnSendsFailureToDS.getPeer();

                    System.out.println("[ERROR] File: " + ffileName + " " + finfo + " on Peer: " + fpeer.getNodeID());

                    break;

            }
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "IOException while trying to contact", ex);
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Exception while sending data", ex);
        }
    }

    public void storeFile(String fileName, String key) {
        try {
            File fts = new File(fileName);
            if (fts.exists()) {
                EventFactory eventFactory = EventFactory.getInstance();
                DSRequestsRandomNode dsrrn = (DSRequestsRandomNode) eventFactory.createEvent(Protocol.DS_REQUESTS_RANDOMNODE);
                dsrrn.setFileName(fileName);
                dsrrn.setIp(me.getIp());
                dsrrn.setPort(me.getPort());
                dsrrn.setKey(key);
                Socket s = new Socket(discoveryNode.getIp(), discoveryNode.getPort());
                new TCPSender(s).sendData(dsrrn.getByte());
            } else {
//                logger.log(Level.SEVERE, "File not Found!");
                System.out.println("[ERROR] File not Found!");
            }
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "DATASTORE CANNOT CONNECT TO DISCOVERYNODE", ex);
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Exception while sending data", ex);
        }
    }

    public void readFile(String fileName, String key) {
        try {
            EventFactory eventFactory = EventFactory.getInstance();
            DSRequestsRandomNodeRead dsrrnr = (DSRequestsRandomNodeRead) eventFactory.createEvent(Protocol.DS_REQUESTS_RANDOMNODE_READ);
            dsrrnr.setFileName(fileName);
            dsrrnr.setKey(key);
            dsrrnr.setIp(me.getIp());
            dsrrnr.setPort(me.getPort());
            Socket s = new Socket(discoveryNode.getIp(), discoveryNode.getPort());
            new TCPSender(s).sendData(dsrrnr.getByte());
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "DATASTORE CANNOT CONNECT TO DISCOVERYNODE", ex);
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Exception while sending data", ex);
        }
    }

    private void saveFile(String fileName, byte[] fileData) {
        FileSaver fileSaver = new FileSaver(fileName, fileData);
        Thread fileSaverThread = new Thread(fileSaver);
        fileSaverThread.start();
    }

    class FileSaver implements Runnable {

        private String fileName;
        private byte[] fileData;

        private FileSaver(String fileName, byte[] fileData) {
            this.fileName = fileName;
            this.fileData = fileData;
        }

        @Override
        public void run() {
            try {
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
                System.out.println("[INFO] File Received!");
            } catch (IOException ex) {
                logger.log(Level.SEVERE, "ERROR WHILE SAVING FILE", ex);
            }
        }

    }

}
