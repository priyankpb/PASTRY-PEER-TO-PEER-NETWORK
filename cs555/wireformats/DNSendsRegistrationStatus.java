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
public class DNSendsRegistrationStatus implements Event {

    private byte type = Protocol.DN_SENDS_REGISTRATION_STATUS;
    private boolean exits;
    private String id;

    public DNSendsRegistrationStatus() {
    }

    public boolean isExits() {
        return exits;
    }

    public void setExits(boolean exits) {
        this.exits = exits;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public DNSendsRegistrationStatus(byte[] data) throws IOException {
        ByteArrayInputStream baInputStream = new ByteArrayInputStream(data);
        DataInputStream din = new DataInputStream(new BufferedInputStream(baInputStream));
        this.type = din.readByte();

        this.exits = din.readBoolean();

        int namelength = din.readInt();
        byte[] idByte = new byte[namelength];
        din.readFully(idByte, 0, namelength);
        this.id = new String(idByte);

        baInputStream.close();
        din.close();
    }

    @Override
    public byte[] getByte() throws Exception {
        byte[] marshalledBytes = null;
        ByteArrayOutputStream baOutputStream = new ByteArrayOutputStream();
        DataOutputStream dout = new DataOutputStream(new BufferedOutputStream(baOutputStream));
        dout.write(getType());

        dout.writeBoolean(this.exits);

        byte[] idBytes = this.id.getBytes();
        dout.writeInt(idBytes.length);
        dout.write(idBytes);

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
