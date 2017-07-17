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
public class DNSendsRandomNodeToDSRead implements Event {

    private byte type = Protocol.DN_SENDS_RANDOMNODE_TO_DS_READ;
    private String fileName;
    private String key;
    private RoutingEntry randomNode;

    public DNSendsRandomNodeToDSRead() {
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

    public RoutingEntry getRandomNode() {
        return randomNode;
    }

    public void setRandomNode(RoutingEntry randomNode) {
        this.randomNode = randomNode;
    }

    public DNSendsRandomNodeToDSRead(byte[] data) throws IOException {
        ByteArrayInputStream baInputStream = new ByteArrayInputStream(data);
        DataInputStream din = new DataInputStream(new BufferedInputStream(baInputStream));
        this.type = din.readByte();

        int fileNameLen = din.readInt();
        byte[] filenameByte = new byte[fileNameLen];
        din.readFully(filenameByte, 0, fileNameLen);
        this.fileName = new String(filenameByte);

        int keyLen = din.readInt();
        if (keyLen > 0) {
            byte[] keyByte = new byte[keyLen];
            din.readFully(keyByte, 0, keyLen);
            this.key = new String(keyByte);
        }

        int randomNodeLength = din.readInt();
        if (randomNodeLength > 0) {
            byte[] randomNodeByte = new byte[randomNodeLength];
            din.readFully(randomNodeByte, 0, randomNodeLength);
            this.randomNode = new RoutingEntry(randomNodeByte);
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

        if (fileName == null) {
            dout.writeInt(0);
        } else {
            byte[] fileNameBytes = this.fileName.getBytes();
            dout.writeInt(fileNameBytes.length);
            dout.write(fileNameBytes);
        }

        if (key == null) {
            dout.writeInt(0);
        } else {
            byte[] keyBytes = this.key.getBytes();
            dout.writeInt(keyBytes.length);
            if (keyBytes.length > 0) {
                dout.write(keyBytes);
            }
        }

        if (randomNode == null) {
            dout.writeInt(0);
        } else {
            byte[] randomNodeByte = randomNode.getByte();
            int randomNodeLength = randomNodeByte.length;
            dout.writeInt(randomNodeLength);
            if (randomNodeLength > 0) {
                dout.write(randomNodeByte);
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
