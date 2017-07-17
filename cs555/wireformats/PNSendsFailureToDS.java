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
public class PNSendsFailureToDS implements Event {

    private byte type = Protocol.PN_SENDS_FAILURE_TO_DS;
    private String fileName;
    private String info;
    private RoutingEntry peer;

    public PNSendsFailureToDS() {
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public RoutingEntry getPeer() {
        return peer;
    }

    public void setPeer(RoutingEntry peer) {
        this.peer = peer;
    }

    public PNSendsFailureToDS(byte[] data) throws IOException {
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
            this.info = new String(keyByte);
        }

        int randomNodeLength = din.readInt();
        if (randomNodeLength > 0) {
            byte[] randomNodeByte = new byte[randomNodeLength];
            din.readFully(randomNodeByte, 0, randomNodeLength);
            this.peer = new RoutingEntry(randomNodeByte);
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

        if (info == null) {
            dout.writeInt(0);
        } else {
            byte[] keyBytes = this.info.getBytes();
            dout.writeInt(keyBytes.length);
            if (keyBytes.length > 0) {
                dout.write(keyBytes);
            }
        }

        if (peer == null) {
            dout.writeInt(0);
        } else {
            byte[] randomNodeByte = peer.getByte();
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
