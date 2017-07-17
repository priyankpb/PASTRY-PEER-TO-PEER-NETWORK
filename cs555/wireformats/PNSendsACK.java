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
public class PNSendsACK implements Event {

    private byte type = Protocol.PN_SENDS_ACK;
    private RoutingEntry pn;

    public PNSendsACK() {
    }

    public RoutingEntry getPn() {
        return pn;
    }

    public void setPn(RoutingEntry pn) {
        this.pn = pn;
    }

    public PNSendsACK(byte[] data) throws IOException {
        ByteArrayInputStream baInputStream = new ByteArrayInputStream(data);
        DataInputStream din = new DataInputStream(new BufferedInputStream(baInputStream));
        this.type = din.readByte();

        int pnLength = din.readInt();
        if (pnLength > 0) {
            byte[] pnByte = new byte[pnLength];
            din.readFully(pnByte, 0, pnLength);
            this.pn = new RoutingEntry(pnByte);
        }

        baInputStream.close();
        din.close();
    }

    @Override
    public byte[] getByte() throws Exception {
        byte[] marshalledBytes = null;
        ByteArrayOutputStream baOutputStream = new ByteArrayOutputStream();
        DataOutputStream dout = new DataOutputStream(new BufferedOutputStream(baOutputStream));
        dout.write(getType());

        if (pn == null) {
            dout.writeInt(0);
        } else {
            byte[] pnByte = pn.getByte();
            int pnLength = pnByte.length;
            dout.writeInt(pnLength);
            if (pnLength > 0) {
                dout.write(pnByte);
            }
        }

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
