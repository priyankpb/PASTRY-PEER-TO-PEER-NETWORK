package cs555.transport;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;
import cs555.nodes.Node;

/**
 *
 * @author YANK
 */
public class TCPReceiver implements Runnable {

    Socket socket;
    DataInputStream din;
    Node node;

    public TCPReceiver(Socket socket, Node node) {
        this.socket = socket;
        this.node = node;
        try {
            this.din = new DataInputStream(socket.getInputStream());
        } catch (IOException ex) {
//            System.out.println("--exception in output stream--");
        }
    }

    @Override
    public void run() {
//        while (true) {
            int dataLength = 0;
            while (socket != null) {
                try {
//                System.out.println("--data length--" + dataLength);
                    dataLength = din.readInt();
                    byte[] data = new byte[dataLength];
                    din.readFully(data, 0, dataLength);
                    node.onEvent(data, this.socket);
                } catch (IOException se) {
//                    System.out.println("--Error in receiving data--");
//                    se.printStackTrace();
                }
            }
//        return null;
//        }
    }
}
