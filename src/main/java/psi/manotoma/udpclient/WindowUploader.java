package psi.manotoma.udpclient;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Tomas Mano <tomasmano@gmail.com>
 */
public class WindowUploader {

    private static final Logger LOG = LoggerFactory.getLogger(WindowUploader.class);
    private File file;
    private FileInputStream fis;
    private static final int WINDOW_BEGIN = 0;
    private static final int WINDOW_END = 2040;
    private byte[] fileData;
    private byte[] datagram;

    public static WindowUploader create(String filename) {
        return new WindowUploader(filename);
    }

    private WindowUploader(String filename) {
        try {
            this.file = new File(filename);
            this.fis = new FileInputStream(file);
            fileData = new byte[(int) file.length()];
            fis.read(fileData);
        } catch (IOException ex) {
            LOG.error("An error occured when creating [{}]: {}", this.getClass(), ex);
        }
    }

    public void send(Connector c) {
        boolean finished = false;
        int lastSize = 0;
        for (int currWin = WINDOW_BEGIN; currWin <= WINDOW_END && currWin < fileData.length; currWin += 255) {
            if (hasMore(currWin)) {
                datagram = new byte[255];
                System.arraycopy(fileData, currWin, datagram, 0, 255);
            } else {
                lastSize = fileData.length - currWin;
                datagram = new byte[lastSize];
                System.arraycopy(fileData, currWin, datagram, 0, lastSize);
                finished = true;
            }
            LOG.debug("Preparing packet for sending..");
            if (!finished) {
                c.send(datagram, Packet.Lengths.MAX_PACKET_SIZE.length(), (short) currWin);
            } else {
                c.send(datagram, lastSize + 9, (short) currWin);
            }
        }
    }

    private boolean hasMore(int currWin) {
        return fileData.length - currWin >= 255;
    }
}
