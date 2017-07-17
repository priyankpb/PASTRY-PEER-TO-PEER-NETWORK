package cs555.wireformats;

import cs555.util.Protocol;
import java.io.IOException;

public class EventFactory {

    private static EventFactory eventFactory = new EventFactory();

    public static EventFactory getInstance() {
        return eventFactory;
    }

    public Event createEvent(byte eventType) throws IOException {
        switch (eventType) {

            case Protocol.DN_SENDS_RANDOMNODE:
                return new DNSendsRandomNode();

            case Protocol.DN_SENDS_RANDOMNODE_TO_DS:
                return new DNSendsRandomNodeToDS();

            case Protocol.DN_SENDS_RANDOMNODE_TO_DS_READ:
                return new DNSendsRandomNodeToDSRead();

            case Protocol.DN_SENDS_REGISTRATION_STATUS:
                return new DNSendsRegistrationStatus();

            case Protocol.DS_REQUESTS_RANDOMNODE:
                return new DSRequestsRandomNode();

            case Protocol.DS_REQUESTS_RANDOMNODE_READ:
                return new DSRequestsRandomNodeRead();

            case Protocol.DS_SENDS_FILE:
                return new DSSendsFile();

            case Protocol.DS_SENDS_KEY:
                return new DSSendsKey();

            case Protocol.DS_SENDS_KEY_READ:
                return new DSSendsKeyRead();

            case Protocol.PN_DEST_SENDS_INFO_TO_PN:
                return new PNDestinationSendsInfoToPN();

            case Protocol.PN_FORWARDS_FILE:
                return new PNForwardsFile();

            case Protocol.PN_FORWARDS_KEY:
                return new PNForwardsKey();

            case Protocol.PN_FORWARDS_KEY_READ:
                return new PNForwardsKeyRead();

            case Protocol.PN_FORWARDS_REQUEST:
                return new PNForwardsRequest();

            case Protocol.PN_NOTIFIES_LEAFNODES:
                return new PNNotifiesLeafNodes();

            case Protocol.PN_NOTIFIES_NODES_IN_ROUTINGTABLE:
                return new PNNotifiesNodesInRoutingtable();

            case Protocol.PN_REPLIES_TO_DS:
                return new PNRepliesToDS();

            case Protocol.PN_SENDS_ACK:
                return new PNSendsACK();

            case Protocol.PN_SENDS_CLOSE_TO_DN:
                return new PNSendsCloseToDN();

            case Protocol.PN_SENDS_CLOSE_TO_LEAFNODES:
                return new PNSendsCloseToLeafNodes();

            case Protocol.PN_SENDS_CLOSE_TO_ROUTINGTABLE:
                return new PNSendsCloseToRoutingTable();

            case Protocol.PN_SENDS_FILE_TO_DS:
                return new PNSendsFileToDS();

            case Protocol.PN_SENDS_JOIN_REQUEST:
                return new PNSendsJoinRequest();

            case Protocol.PN_SENDS_REGISTRATION:
                return new PNSendsRegistration();

            case Protocol.PN_SENDS_REGISTRATION_W_CONFLICT:
                return new PNSendsRegistrationWConflict();

            case Protocol.PN_SENDS_FAILURE_TO_DS:
                return new PNSendsFailureToDS();

            case Protocol.PN_SENDS_FILE_TRANSFER_COMPLETE:
                return new PNSendsFileTransferComplete();

        }

        return null;
    }

}
