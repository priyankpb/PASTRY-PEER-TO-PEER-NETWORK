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
public class PNDestinationSendsInfoToPN implements Event {

    private byte type = Protocol.PN_DEST_SENDS_INFO_TO_PN;
    private RoutingEntry me;
    private RoutingEntry leftLeaf;
    private RoutingEntry rightLeaf;
    private RoutingTable tempRT;
    private String trace;
    private int hopCount;

    public PNDestinationSendsInfoToPN() {
    }

    public RoutingEntry getMe() {
        return me;
    }

    public void setMe(RoutingEntry me) {
        this.me = me;
    }

    public RoutingEntry getLeftLeaf() {
        return leftLeaf;
    }

    public void setLeftLeaf(RoutingEntry leftLeaf) {
        this.leftLeaf = leftLeaf;
    }

    public RoutingEntry getRightLeaf() {
        return rightLeaf;
    }

    public void setRightLeaf(RoutingEntry rightLeaf) {
        this.rightLeaf = rightLeaf;
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

    public PNDestinationSendsInfoToPN(byte[] data) throws IOException, Exception {
        ByteArrayInputStream baInputStream = new ByteArrayInputStream(data);
        DataInputStream din = new DataInputStream(new BufferedInputStream(baInputStream));
        this.type = din.readByte();

        //me
        int melen = din.readInt();
        if (melen > 0) {
            byte[] meByte = new byte[melen];
            din.readFully(meByte, 0, melen);
            this.me = new RoutingEntry(meByte);
        }

        //leftleaf
        int leftLeafLength = din.readInt();
        if (leftLeafLength > 0) {
            byte[] leftLeafByte = new byte[leftLeafLength];
            din.readFully(leftLeafByte, 0, leftLeafLength);
            this.leftLeaf = new RoutingEntry(leftLeafByte);
        }

        //rightleaf
        int rightLeafLength = din.readInt();
        if (rightLeafLength > 0) {
            byte[] rightLeafByte = new byte[rightLeafLength];
            din.readFully(rightLeafByte, 0, rightLeafLength);
            this.rightLeaf = new RoutingEntry(rightLeafByte);
        }

        //routingtable
        int nodelen = din.readInt();
        if (nodelen > 0) {
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
        }

        //trace
        int traceLen = din.readInt();
        if (traceLen > 0) {
            byte[] traceByte = new byte[traceLen];
            din.readFully(traceByte, 0, traceLen);
            this.trace = new String(traceByte);
        }

        //hopcount
        this.hopCount = din.readInt();

        baInputStream.close();
        din.close();
    }

    @Override
    public byte[] getByte() throws Exception {
        byte[] marshalledBytes = null;
        ByteArrayOutputStream baOutputStream = new ByteArrayOutputStream();
        DataOutputStream dout = new DataOutputStream(new BufferedOutputStream(baOutputStream));
        dout.write(getType());

        //me
        if (me == null) {
            dout.writeInt(0);
        } else {
            byte[] meBytes = me.getByte();
            int meLength = meBytes.length;
            dout.writeInt(meLength);
            if (meLength > 0) {
                dout.write(meBytes);
            }
        }

        //leftleaf
        if (leftLeaf == null) {
            dout.writeInt(0);
        } else {
            byte[] leftLeafBytes = leftLeaf.getByte();
            int leftLeafLength = leftLeafBytes.length;
            dout.writeInt(leftLeafLength);
            if (leftLeafLength > 0) {
                dout.write(leftLeafBytes);
            }
        }

        //rightleaf
        if (rightLeaf == null) {
            dout.writeInt(0);
        } else {
            byte[] rightLeafBytes = rightLeaf.getByte();
            int rightLeafLength = rightLeafBytes.length;
            dout.writeInt(rightLeafLength);
            if (rightLeafLength > 0) {
                dout.write(rightLeafBytes);
            }
        }

        //routingtable
        if (tempRT == null) {
            dout.writeInt(0);
        } else {
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
        }

        //trace
        if (trace == null) {
            dout.writeInt(0);
        } else {
            byte[] traceBytes = this.trace.getBytes();
            dout.writeInt(traceBytes.length);
            dout.write(traceBytes);
        }

        //hopcount
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
