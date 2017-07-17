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
public class PNSendsCloseToLeafNodes implements Event {

    private byte type = Protocol.PN_SENDS_CLOSE_TO_LEAFNODES;
    private char direction;
    private RoutingEntry leafNode;

    public PNSendsCloseToLeafNodes() {
    }

    public RoutingEntry getLeafNode() {
        return leafNode;
    }

    public void setLeafNode(RoutingEntry leafNode) {
        this.leafNode = leafNode;
    }

    public char getDirection() {
        return direction;
    }

    public void setDirection(char direction) {
        this.direction = direction;
    }

    public PNSendsCloseToLeafNodes(byte[] data) throws IOException {
        ByteArrayInputStream baInputStream = new ByteArrayInputStream(data);
        DataInputStream din = new DataInputStream(new BufferedInputStream(baInputStream));
        this.type = din.readByte();

        this.direction = din.readChar();

        int randomNodeLength = din.readInt();
        if (randomNodeLength > 0) {
            byte[] randomNodeByte = new byte[randomNodeLength];
            din.readFully(randomNodeByte, 0, randomNodeLength);
            this.leafNode = new RoutingEntry(randomNodeByte);
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

        dout.writeChar(direction);

        byte[] randomNodeByte = leafNode.getByte();
        int randomNodeLength = randomNodeByte.length;
        dout.writeInt(randomNodeLength);
        if (randomNodeLength > 0) {
            dout.write(randomNodeByte);
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
