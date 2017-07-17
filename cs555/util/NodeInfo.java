/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cs555.util;

/**
 *
 * @author priyankb
 */
public class NodeInfo {

    String nickName;
    String ip;
    int port;

    public NodeInfo(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    public NodeInfo(String nickName, String ip, int port) {
        this.nickName = nickName;
        this.ip = ip;
        this.port = port;
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
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
}
