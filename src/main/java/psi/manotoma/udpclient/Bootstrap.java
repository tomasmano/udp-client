package psi.manotoma.udpclient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client
 *
 */
public class Bootstrap {
    
    private static final Logger LOG = LoggerFactory.getLogger(Bootstrap.class);

    public final static String ADDR = "localhost";
    public final static int PORT = 4000;
    public final static byte[] PHOTO_DOWNLOAD = {0x01};
    public final static byte[] FIRMWARE_UPLOAD = {0x02};

    public static void main(String[] args) {
        Connector con = Connector.create(ADDR, PORT);
        Packet packet = Packet.createSynPacket(PHOTO_DOWNLOAD);
        con.connect(packet);
        LOG.info("******** START DOWNLOAD ********");
        con.download();
    }
}
