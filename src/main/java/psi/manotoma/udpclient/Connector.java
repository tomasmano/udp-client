package psi.manotoma.udpclient;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static psi.manotoma.udpclient.Packet.isSyn;

/**
 *
 * @author Tomas Mano <tomasmano@gmail.com>
 */
public class Connector {

    private static final Logger LOG = LoggerFactory.getLogger(Connector.class);
    private static final int SYN_LOST_TIMEOUT = 100;
    
    private InetAddress addr;
    private int port;
    private DatagramSocket socket;

    private Connector(InetAddress addr, int port) {
        this.addr = addr;
        this.port = port;
    }

    public static Connector create(String addr, int port) {
        InetAddress a = null;
        try {
            a = InetAddress.getByName(addr);
        } catch (UnknownHostException ex) {
            LOG.error("An error occured when seting up a connector: {}", ex);
            System.exit(-1);
        }
        Connector c = new Connector(a, port);
        c.createSocket();
        return c;
    }

    private void createSocket() {
        try {
            this.socket = new DatagramSocket();
            this.socket.setSoTimeout(SYN_LOST_TIMEOUT);
        } catch (SocketException ex) {
            LOG.error("An error occured when creating a socket: {}", ex);
        }

    }

    public Packet connect(Packet packet) {
        LOG.info("Establishing connection to [{}, {}]..", addr, port);
        LOG.debug("Building packet: {}", packet);
        DatagramPacket dp = packet.buildDatagram(addr, port, Packet.Lengths.SYN.length());
        LOG.debug("Sending datagram packet: {}", dp);
        try {
            socket.send(dp);
        } catch (IOException ex) {
            LOG.error("An error occured when creating a socket: {}", ex);
        }
        boolean synRecieved = false;
        while (!synRecieved) {
            DatagramPacket recieved = new DatagramPacket(new byte[Packet.Lengths.MAX_PACKET_SIZE.length()], Packet.Lengths.MAX_PACKET_SIZE.length());
            try {
                socket.receive(recieved);
            } catch (IOException ex) {
                LOG.error("An error occured when recieving a packet: {}", ex);
                try {
                    this.socket.send(dp);
                } catch (IOException f) {
                    LOG.error("An error occured when resending a packet: {}", ex);
                }
                continue;
            }

            packet = new Packet(recieved.getData());
            LOG.info("Response recieved: [{}]", packet);

            if (isSyn(packet)) {
                try {
                    this.socket.setSoTimeout(0);
                } catch (SocketException ex) {
                    LOG.error("An error occured when setting a timeout: {}", ex);
                }
                return packet;
            }
        }
        return null;
    }

    public void download(Packet pack) {
    }

    public void upload(Packet pack) {
    }

    public InetAddress getAddr() {
        return addr;
    }

    public int getPort() {
        return port;
    }

    @Override
    public String toString() {
        return "Connector{" + "addr=" + addr + ", port=" + port + '}';
    }
}
