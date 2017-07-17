/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cs555.wireformats;

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
public class DSSendsKey implements Event {

    private byte type = Protocol.DS_SENDS_KEY;
    private String fileName;
    private String key;

    private String dsIP;
    private int dsPort;

    private String trace;
    private int hopCount;

    public DSSendsKey() {
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

    public String getDsIP() {
        return dsIP;
    }

    public void setDsIP(String dsIP) {
        this.dsIP = dsIP;
    }

    public int getDsPort() {
        return dsPort;
    }

    public void setDsPort(int dsPort) {
        this.dsPort = dsPort;
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

    public DSSendsKey(byte[] data) throws IOException {
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

        int iplength = din.readInt();
        byte[] ipByte = new byte[iplength];
        din.readFully(ipByte, 0, iplength);
        this.dsIP = new String(ipByte);

        this.dsPort = din.readInt();

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
            dout.write(keyBytes);
        }

        byte[] ipBytes = this.dsIP.getBytes();
        dout.writeInt(ipBytes.length);
        dout.write(ipBytes);

        dout.writeInt(dsPort);

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
