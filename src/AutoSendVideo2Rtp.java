import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

/**
 * .
 * <p>
 *
 * @author <a href="mailto:shunshun.lss@alibaba-inc.com">shunshun.lss</a>
 * @version 1.0.0
 * @since 2023-02-14
 */
public class AutoSendVideo2Rtp implements ActionListener {

    private VideoStream video;
    byte[] buf;
    Timer timer;
    static int MJPEG_TYPE = 26;
    static int FRAME_PERIOD = 100;
    static int VIDEO_LENGTH = 500;
    int imagenb = 0;
    DatagramSocket RTPsocket;
    DatagramPacket senddp;

    SocketAddress toAddress;

    public static void main(String[] args)  {
        try{
            System.out.println("start...");
            new AutoSendVideo2Rtp().play(args[0], args[1], Integer.parseInt(args[2]));
        }catch (Exception e){
            System.out.println(e);
        }
    }

    private void play(String fileName, String toIp, int toPort) throws Exception {
        video = new VideoStream(fileName);
        timer = new Timer(FRAME_PERIOD, this);
        timer.setInitialDelay(0);
        timer.setCoalesce(true);

        RTPsocket = new DatagramSocket();

        //allocate memory for the sending buffer
        buf = new byte[15000];

        toAddress = new InetSocketAddress(toIp,toPort);

        timer.start();

        Thread.sleep(1000000);
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        //if the current image nb is less than the length of the video
        if (imagenb < VIDEO_LENGTH)
        {
            //update current imagenb
            imagenb++;

            try {
                //get next frame to send from the video, as well as its size
                int image_length = video.getnextframe(buf);

                //Builds an RTPpacket object containing the frame
                RTPpacket rtp_packet = new RTPpacket(MJPEG_TYPE, imagenb, imagenb*FRAME_PERIOD, buf, image_length);

                //get to total length of the full rtp packet to send
                int packet_length = rtp_packet.getlength();

                //retrieve the packet bitstream and store it in an array of bytes
                byte[] packet_bits = new byte[packet_length];
                rtp_packet.getpacket(packet_bits);

                //send the packet as a DatagramPacket over the UDP socket
                senddp = new DatagramPacket(packet_bits, packet_length, toAddress);
                RTPsocket.send(senddp);

                //System.out.println("Send frame #"+imagenb);
                //print the header bitstream
                rtp_packet.printheader();
            }
            catch(Exception ex)
            {
                System.out.println("Exception caught: "+ex);
                ex.printStackTrace();
                System.exit(0);
            }
        }
        else
        {
            //if we have reached the end of the video file, stop the timer
            timer.stop();
        }
    }
}
