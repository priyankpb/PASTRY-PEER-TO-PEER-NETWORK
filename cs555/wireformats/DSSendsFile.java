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
public class DSSendsFile implements Event {

    private byte type = Protocol.DS_SENDS_FILE;
    private String fileName;
    private byte[] fileData;
    private String key;

    public DSSendsFile() {
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public byte[] getFileData() {
        return fileData;
    }

    public void setFileData(byte[] fileData) {
        this.fileData = fileData;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public DSSendsFile(byte[] data) throws IOException {
        ByteArrayInputStream baInputStream = new ByteArrayInputStream(data);
        DataInputStream din = new DataInputStream(new BufferedInputStream(baInputStream));
        this.type = din.readByte();

        int fileNameLen = din.readInt();
        byte[] filenameByte = new byte[fileNameLen];
        din.readFully(filenameByte, 0, fileNameLen);
        this.fileName = new String(filenameByte);

        int dataLen = din.readInt();
        if (dataLen > 0) {
            this.fileData = new byte[dataLen];
            din.readFully(this.fileData, 0, dataLen);
        }

        int keyLen = din.readInt();
        byte[] keyByte = new byte[keyLen];
        din.readFully(keyByte, 0, keyLen);
        this.key = new String(keyByte);

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

        if (fileData == null) {
            dout.writeInt(0);
        } else {
            int fileLen = this.fileData.length;
            dout.writeInt(fileLen);
            if (fileLen > 0) {
                dout.write(fileData);
            }
        }

        if (key == null) {
            dout.writeInt(0);
        } else {
            byte[] keyBytes = this.key.getBytes();
            dout.writeInt(keyBytes.length);
            dout.write(keyBytes);
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
