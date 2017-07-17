package cs555.routing;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class RoutingEntry {

    String nodeID;
    String nickname;
    String ip;
    int port;

    public RoutingEntry(String nodeID, String nickname, String ip, int port) {
        this.nodeID = nodeID;
        this.nickname = nickname;
        this.ip = ip;
        this.port = port;
    }

    public String getNodeID() {
        return nodeID;
    }

    public void setNodeID(String nodeID) {
        this.nodeID = nodeID;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public RoutingEntry(byte[] data) throws IOException {
        ByteArrayInputStream baInputStream = new ByteArrayInputStream(data);
        DataInputStream din = new DataInputStream(new BufferedInputStream(baInputStream));

        int namelength = din.readInt();
        if (namelength > 0) {
            byte[] idByte = new byte[namelength];
            din.readFully(idByte, 0, namelength);
            this.nodeID = new String(idByte);
        }

        int nicknamelength = din.readInt();
        if (nicknamelength > 0) {
            byte[] nicknameByte = new byte[nicknamelength];
            din.readFully(nicknameByte, 0, nicknamelength);
            this.nickname = new String(nicknameByte);
        }

        int iplength = din.readInt();
        if (iplength > 0) {
            byte[] ipByte = new byte[iplength];
            din.readFully(ipByte, 0, iplength);
            this.ip = new String(ipByte);
        }

        this.port = din.readInt();

        baInputStream.close();
        din.close();
    }

    public byte[] getByte() throws Exception {
        byte[] marshalledBytes = null;
        ByteArrayOutputStream baOutputStream = new ByteArrayOutputStream();
        DataOutputStream dout = new DataOutputStream(new BufferedOutputStream(baOutputStream));

        byte[] idBytes = this.nodeID.getBytes();
        dout.writeInt(idBytes.length);
        if (idBytes.length > 0) {
            dout.write(idBytes);
        }

        byte[] nicknameBytes = this.nickname.getBytes();
        dout.writeInt(nicknameBytes.length);
        if (nicknameBytes.length > 0) {
            dout.write(nicknameBytes);
        }

        byte[] ipBytes = this.ip.getBytes();
        dout.writeInt(ipBytes.length);
        if (ipBytes.length > 0) {
            dout.write(ipBytes);
        }

        dout.writeInt(port);

        dout.flush();
        marshalledBytes = baOutputStream.toByteArray();
        baOutputStream.close();
        dout.close();
        return marshalledBytes;
    }

    @Override
    public String toString() {
        String s = this.nodeID + " : " + this.nickname + " : " + this.ip + ":" + this.port;
        return s;
    }

}
