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
public class PNSendsJoinRequest implements Event {

    private byte type = Protocol.PN_SENDS_JOIN_REQUEST;
    private RoutingEntry pn;
    private String trace;
    private int hopCount;

    public PNSendsJoinRequest() {
    }

    public RoutingEntry getPn() {
        return pn;
    }

    public void setPn(RoutingEntry pn) {
        this.pn = pn;
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

    public PNSendsJoinRequest(byte[] data) throws IOException {
        ByteArrayInputStream baInputStream = new ByteArrayInputStream(data);
        DataInputStream din = new DataInputStream(new BufferedInputStream(baInputStream));
        this.type = din.readByte();

        int pnLength = din.readInt();
        if (pnLength > 0) {
            byte[] pnByte = new byte[pnLength];
            din.readFully(pnByte, 0, pnLength);
            this.pn = new RoutingEntry(pnByte);
        }

        int traceLen = din.readInt();
        if (traceLen > 0) {
            byte[] traceByte = new byte[traceLen];
            din.readFully(traceByte, 0, traceLen);
            this.trace = new String(traceByte);
        }

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

        byte[] pnByte = pn.getByte();
        int pnLength = pnByte.length;
        dout.writeInt(pnLength);
        if (pnLength > 0) {
            dout.write(pnByte);
        }

        if (trace == null) {
            dout.writeInt(0);
        } else {
            byte[] traceBytes = this.trace.getBytes();
            dout.writeInt(traceBytes.length);
            dout.write(traceBytes);
        }
        
        dout.writeInt(this.hopCount);

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
