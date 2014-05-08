package psi.manotoma.udpclient;

/**
 * Client
 *
 */
public class Bootstrap {

    public final static String ADDR = "baryk.felk.cvut.cz";
    public final static int PORT = 4000;
    public final static byte[] PHOTO_DOWNLOAD = {0x01};
    public final static byte[] FIRMWARE_UPLOAD = {0x02};

    public static void main(String[] args) {
        Connector con = Connector.create(ADDR, PORT);
        Packet packet = new Packet(0, (short) 0, (short) 0, Packet.Flag.SYN, PHOTO_DOWNLOAD);
        con.connect(packet);
    }
}
