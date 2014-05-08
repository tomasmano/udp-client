package psi.manotoma.udpclient;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 *
 * @author Tomas Mano <tomasmano@gmail.com>
 */
public class Packet {

    private int connectionNum;
    private short seq;
    private short ack;
    private Flag flag;
    private byte[] data;
    private int dLength;

    public Packet(int connectionNum, short seq, short ack, Flag flag, byte[] data) {
        this.connectionNum = connectionNum;
        this.seq = seq;
        this.ack = ack;
        this.flag = flag;
        this.data = data;
        this.dLength = data.length;
    }

    public Packet(byte[] datagram) {
        connectionNum = ByteBuffer.wrap(Arrays.copyOfRange(datagram, 0, 4)).getInt();
        seq = ByteBuffer.wrap(Arrays.copyOfRange(datagram, 5, 7)).getShort();
        ack = ByteBuffer.wrap(Arrays.copyOfRange(datagram, 7, 9)).getShort();
        flag = Flag.parseValue(ByteBuffer.wrap(Arrays.copyOfRange(datagram, 9, 10)).getInt());

        dLength = datagram.length - Lengths.HEADER.length;
        data = new byte[dLength];
        for (int i = 0; i < dLength; i++) {
            this.data[i] = datagram[Lengths.HEADER.length + i];
        }
    }

    public DatagramPacket buildDatagram(InetAddress address, int port, int packetLength) {
        byte[] buffer = new byte[packetLength];

        // fill first 4 bytes in buffer with the connection number
        byte[] connectionNumberArray = ByteBuffer.allocate(4).putInt(connectionNum).array();
        System.arraycopy(connectionNumberArray, 0, buffer, 0, connectionNumberArray.length);

        // fill next 2 bytes in buffer with the syn number
        byte[] synArray = ByteBuffer.allocate(2).putShort(seq).array();
        System.arraycopy(synArray, 0, buffer, 0, synArray.length);

        // fill next 2 bytes in buffer with the ack number
        byte[] ackArray = ByteBuffer.allocate(2).putShort(ack).array();
        System.arraycopy(ackArray, 0, buffer, 0, ackArray.length);

        // fill last byte of header in buffer with the ack number
        byte[] flagArray = ByteBuffer.allocate(1).put(flag.value).array();
        System.arraycopy(flagArray, 0, buffer, 0, flagArray.length);

        // fill the rest of packet with data
        if (this.data != null) {
            System.arraycopy(this.data, 0, buffer, Lengths.HEADER.length, this.data.length);
        }

        return new DatagramPacket(buffer, packetLength, address, port);
    }

    public enum Flag {

        SYN((byte) 4), FIN((byte) 2), RST((byte) 1);
        private byte value;

        private Flag(byte value) {
            this.value = value;
        }

        public int value() {
            return value;
        }

        public static Flag parseValue(int val) {
            Flag[] flags = Flag.values();
            for (Flag f : flags) {
                if (f.value == val) {
                    return f;
                }
            }
            throw new IllegalArgumentException(String.format("[%s] is not valid paramter. It doesn't match any flag.", val));
        }
    }

    public enum Lengths {

        HEADER(9), SYN(10), ACK(9), FIN(9), MAX_PACKET_SIZE(264);
        private int length;

        private Lengths(int length) {
            this.length = length;
        }

        public int length() {
            return length;
        }
    }

    public static boolean isSyn(Packet packet) {
        return packet.flag == Flag.SYN;
    }

    public static boolean isRst(Packet packet) {
        return packet.flag == Flag.RST;
    }
    
    public static boolean isFin(Packet packet) {
        return packet.flag == Flag.FIN;
    }

    @Override
    public String toString() {
        return "Packet{" + "connectionNum=" + connectionNum + ", seq=" + seq + ", ack=" + ack + ", flag=" + flag + ", dLength=" + dLength + '}';
    }
}
