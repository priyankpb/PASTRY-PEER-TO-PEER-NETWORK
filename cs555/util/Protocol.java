/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cs555.util;

/**
 *
 * @author priyankb
 */
public class Protocol {

    public static final int PORT = 11520;
    public static final String HOSTNAME = "coconut";
    public static final int PEER_PORT = 50000;

    public static final String FILE_PREFIX = "/tmp/cs555_priyankb";

    public static final byte DN_SENDS_REGISTRATION_STATUS = 1;
    public static final byte DN_SENDS_RANDOMNODE = 2;
    public static final byte DN_SENDS_RANDOMNODE_TO_DS = 3;
    public static final byte DN_SENDS_RANDOMNODE_TO_DS_READ = 4;

    public static final byte PN_SENDS_REGISTRATION = 50;
    public static final byte PN_SENDS_REGISTRATION_W_CONFLICT = 51;
    public static final byte PN_SENDS_ACK = 52;
    public static final byte PN_SENDS_CLOSE_TO_DN = 53;
    public static final byte PN_SENDS_JOIN_REQUEST = 54;
    public static final byte PN_FORWARDS_REQUEST = 55;
    public static final byte PN_DEST_SENDS_INFO_TO_PN = 56;
    public static final byte PN_NOTIFIES_LEAFNODES = 57;
    public static final byte PN_NOTIFIES_NODES_IN_ROUTINGTABLE = 58;
    public static final byte PN_FORWARDS_KEY = 59;
    public static final byte PN_FORWARDS_KEY_READ = 60;
    public static final byte PN_REPLIES_TO_DS = 61;
    public static final byte PN_FORWARDS_FILE = 62;
    public static final byte PN_SENDS_CLOSE_TO_LEAFNODES = 63;
    public static final byte PN_SENDS_CLOSE_TO_ROUTINGTABLE = 64;
    public static final byte PN_SENDS_FILE_TO_DS = 65;
    public static final byte PN_SENDS_FAILURE_TO_DS = 66;
    public static final byte PN_SENDS_FILE_TRANSFER_COMPLETE = 67;

    public static final byte DS_REQUESTS_RANDOMNODE = 100;
    public static final byte DS_SENDS_KEY = 101;
    public static final byte DS_SENDS_FILE = 102;
    public static final byte DS_REQUESTS_RANDOMNODE_READ = 103;
    public static final byte DS_SENDS_KEY_READ = 104;

    public static final class DIRECTION {

        public static final char RIGHT = 'R';
        public static final char LEFT = 'L';

    }

}
