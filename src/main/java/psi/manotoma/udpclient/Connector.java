package psi.manotoma.udpclient;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
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
    private int connectionNumber;
    private DatagramSocket socket;
    private Map<Integer, Packet> recieveds = new HashMap<Integer, Packet>();

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
        } catch (SocketException ex) {
            LOG.error("An error occured when creating a socket: {}", ex);
        }

    }

    public Packet connect(Packet packet) {
        try {
            this.socket.setSoTimeout(SYN_LOST_TIMEOUT);
        } catch (SocketException ex) {
            LOG.error("An error occured when creating a socket: {}", ex);
        }
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
            } catch (Exception ex) {
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
            this.connectionNumber = packet.getConnectionNum();

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

    public void download() {

        Packet packet = null;
        DatagramPacket datagram = null;
        int ack = 255;

        packet = recievePacket();

        while (!Packet.isFin(packet)) {
            if (packet.isValid(connectionNumber)) {
                recieveds.put((int) packet.getSeq(), packet); // TODO handle overflow
                if (recieveds.containsKey(ack)) {
                    ack += 255;
                }
                // send ack to server now
                packet = new Packet(connectionNumber, (short) 0, (short) ack, Packet.Flag.ZERO, new byte[0]);
                datagram = packet.buildDatagram(addr, port, Packet.Lengths.ACK.length());
                LOG.info("Sending a packet: {}", packet);
                try {
                    socket.send(datagram);

                } catch (IOException ex) {
                    LOG.error("An error occured when downloading: {}", ex);
                }
            }
            packet = recievePacket();
        }

        close(connectionNumber, packet.getAck());

        try {
            FileOutputStream fos = new FileOutputStream("./pic.png", false);
            for (Packet p : recieveds.values()) {
                fos.write(p.getData());
            }
            fos.close();
        } catch (IOException ex) {
            LOG.error("An error occured when writing file: {}", ex);
        }
        recieveds.clear();

    }

    public void upload(String fileName) {
    }
    
    public void send(byte[] data, int length, short seq){
        try {
            Packet packet = new Packet(connectionNumber, seq, (short) 0, Packet.Flag.ZERO, data);
            DatagramPacket datagram = packet.buildDatagram(addr, port, length);
            socket.send(datagram);
        } catch (IOException ex) {
            LOG.error("An error occured when sending packet: {}", ex);
        }
    }

    //////////  Helper methods  //////////
    private void close(int connectionNum, short ack) {

        Packet pack = Packet.createFinPacket(connectionNum, ack);

        DatagramPacket datagram = pack.buildDatagram(addr, port, Packet.Lengths.FIN.length());
        LOG.info("Sending a FIN datagram..");

        try {
            socket.send(datagram);
        } catch (Exception ex) {
            LOG.error("An error occured while sending a packet: {}", ex);
        }
        try {
            socket.receive(datagram);
            pack = new Packet(datagram.getData());
            LOG.info("Received response on FIN: {}", pack);
        } catch (Exception ex) {
            LOG.error("An error occured when recieving datagram: {}", ex);
        }
        if (Packet.isFin(pack)) {
            socket.close();
        }
    }

    //////////  Getters / Setters  //////////
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

    private Packet recievePacket() {
        DatagramPacket datagram = new DatagramPacket(new byte[Packet.Lengths.MAX_PACKET_SIZE.length()], Packet.Lengths.MAX_PACKET_SIZE.length(), addr, port);
        try {
            socket.receive(datagram);
        } catch (IOException ex) {
            LOG.error("An error occured when recieving a packet: {}", ex);
        }
        Packet packet = new Packet(datagram.getData());
        LOG.info("Packet recieved: {}", packet);
        return packet;
    }
}
