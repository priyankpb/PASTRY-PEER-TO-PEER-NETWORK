/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cs555.wireformats;

import cs555.routing.RoutingEntry;
import cs555.util.Protocol;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 *
 * @author priyankb
 */
public class PNRepliesToDS implements Event {

    private byte type = Protocol.PN_REPLIES_TO_DS;
    private RoutingEntry destPeer;
    private String fileName;
    private String key;
    private String trace;
    private int hopCount;

    public PNRepliesToDS() {
    }

    public RoutingEntry getDestPeer() {
        return destPeer;
    }

    public void setDestPeer(RoutingEntry destPeer) {
        this.destPeer = destPeer;
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

    public PNRepliesToDS(byte[] data) throws IOException {
        ByteArrayInputStream baInputStream = new ByteArrayInputStream(data);
        DataInputStream din = new DataInputStream(new BufferedInputStream(baInputStream));
        this.type = din.readByte();

        int fileNameLen = din.readInt();
        byte[] filenameByte = new byte[fileNameLen];
        din.readFully(filenameByte, 0, fileNameLen);
        this.fileName = new String(filenameByte);

        int keyLen = din.readInt();
        byte[] keyByte = new byte[keyLen];
        din.readFully(keyByte, 0, keyLen);
        this.key = new String(keyByte);

        int randomNodeLength = din.readInt();
        if (randomNodeLength > 0) {
            byte[] randomNodeByte = new byte[randomNodeLength];
            din.readFully(randomNodeByte, 0, randomNodeLength);
            this.destPeer = new RoutingEntry(randomNodeByte);
        }

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

        byte[] fileNameBytes = this.fileName.getBytes();
        dout.writeInt(fileNameBytes.length);
        dout.write(fileNameBytes);

        if (key == null) {
            dout.writeInt(0);
        } else {
            byte[] keyBytes = this.key.getBytes();
            dout.writeInt(keyBytes.length);
            dout.write(keyBytes);
        }

        byte[] randomNodeByte = destPeer.getByte();
        int randomNodeLength = randomNodeByte.length;
        dout.writeInt(randomNodeLength);
        if (randomNodeLength > 0) {
            dout.write(randomNodeByte);
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
