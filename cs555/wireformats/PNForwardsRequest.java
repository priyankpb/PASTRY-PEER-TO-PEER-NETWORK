/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cs555.wireformats;

import cs555.routing.RoutingEntry;
import cs555.routing.RoutingTable;
import cs555.util.Protocol;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

/**
 *
 * @author priyankb
 */
public class PNForwardsRequest implements Event {

    private byte type = Protocol.PN_FORWARDS_REQUEST;
    private RoutingEntry requestingNode;
    private RoutingTable tempRT;
    private String trace;
    private int hopCount;

    public PNForwardsRequest() {
    }

    public RoutingEntry getRequestingNode() {
        return requestingNode;
    }

    public void setRequestingNode(RoutingEntry requestingNode) {
        this.requestingNode = requestingNode;
    }

    public RoutingTable getTempRT() {
        return tempRT;
    }

    public void setTempRT(RoutingTable tempRT) {
        this.tempRT = tempRT;
    }

    public String getTrace() {
        return trace;
    }

    public void setTrace(String trace) {
        this.trace = trace;
    }

    public int getHopCount() {
        return hopCount;
    }

    public void setHopCount(int hopCount) {
        this.hopCount = hopCount;
    }

    public PNForwardsRequest(byte[] data) throws IOException {
        ByteArrayInputStream baInputStream = new ByteArrayInputStream(data);
        DataInputStream din = new DataInputStream(new BufferedInputStream(baInputStream));
        this.type = din.readByte();

        int requestingNodeLength = din.readInt();
        if (requestingNodeLength > 0) {
            byte[] requestingNodeByte = new byte[requestingNodeLength];
            din.readFully(requestingNodeByte, 0, requestingNodeLength);
            this.requestingNode = new RoutingEntry(requestingNodeByte);
        }

        //routingtable
        int nodelen = din.readInt();
        byte[] nodeByte = new byte[nodelen];
        din.readFully(nodeByte, 0, nodelen);
        String nodeID = new String(nodeByte);
        Map<Integer, Map<String, RoutingEntry>> routingtable = new TreeMap<>();

        int length = din.readInt();
        for (int i = 0; i < length; i++) {
            int key = din.readInt();

            Map<String, RoutingEntry> row = new TreeMap<>();

            int rowLength = din.readInt();
            for (int j = 0; j < rowLength; j++) {
                int handleLength = din.readInt();
                byte[] handleByte = new byte[handleLength];
                din.readFully(handleByte, 0, handleLength);
                String handle = new String(handleByte);
                RoutingEntry re = null;
                int reLength = din.readInt();
                if (reLength > 0) {
                    byte[] reByte = new byte[reLength];
                    din.readFully(reByte, 0, reLength);
                    re = new RoutingEntry(reByte);

                }
                row.put(handle, re);
            }
            routingtable.put(key, row);

        }
        this.tempRT = new RoutingTable(nodeID, routingtable);

        int traceLen = din.readInt();
        if (traceLen > 0) {
            byte[] traceByte = new byte[traceLen];
            din.readFully(traceByte, 0, traceLen);
            this.trace = new String(traceByte);
        }
        
        hopCount = din.readInt();

        baInputStream.close();
        din.close();
    }

    @Override
    public byte[] getByte() throws Exception {
        byte[] marshalledBytes = null;
        ByteArrayOutputStream baOutputStream = new ByteArrayOutputStream();
        DataOutputStream dout = new DataOutputStream(new BufferedOutputStream(baOutputStream));
        dout.write(getType());

        byte[] requestingNodeByte = requestingNode.getByte();
        int requestingNodeLength = requestingNodeByte.length;
        dout.writeInt(requestingNodeLength);
        if (requestingNodeLength > 0) {
            dout.write(requestingNodeByte);
        }

        //routingtable
        byte[] nodeBytes = this.tempRT.nodeID.getBytes();
        dout.writeInt(nodeBytes.length);
        dout.write(nodeBytes);

        Map<Integer, Map<String, RoutingEntry>> routingtable = tempRT.routingtable;
        int length = routingtable.size();
        dout.writeInt(length);

        for (Map.Entry<Integer, Map<String, RoutingEntry>> entrySet : routingtable.entrySet()) {
            int key = entrySet.getKey();
            Map<String, RoutingEntry> row = entrySet.getValue();

            dout.writeInt(key);

            int rowlength = row.size();
            dout.writeInt(rowlength);

            for (Map.Entry<String, RoutingEntry> entrySet1 : row.entrySet()) {
                String handle = entrySet1.getKey();
                RoutingEntry re = entrySet1.getValue();

                byte[] handleBytes = handle.getBytes();
                int handleLength = handleBytes.length;
                dout.writeInt(handleLength);
                if (handleLength > 0) {
                    dout.write(handleBytes);
                }
                if (re == null) {
                    dout.writeInt(0);
                } else {
                    byte[] reBytes = re.getByte();
                    int reLength = reBytes.length;
                    dout.writeInt(reLength);
                    if (reLength > 0) {
                        dout.write(reBytes);
                    }
                }
            }
        }

        if (trace == null) {
            dout.writeInt(0);
        } else {
            byte[] traceBytes = this.trace.getBytes();
            dout.writeInt(traceBytes.length);
            dout.write(traceBytes);
        }
        
        dout.writeInt(hopCount);

        dout.flush();
        marshalledBytes = baOutputStream.toByteArray();
        baOutputStream.close();
        dout.close();
        return marshalledBytes;
    }

    @Override
    public byte getType() {
        return this.type;
    }

}
