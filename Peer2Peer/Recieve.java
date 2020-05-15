import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Thinesh
 */
public class Recieve implements Runnable {

    private final int mcPort = 55001;
    byte playBuffer[] = new byte[500]; 
    private AudioFormat audioFormat;
    private TargetDataLine targetDataLine;
    private AudioInputStream audioInputStream;
    private SourceDataLine sourceDataLine;

    public AudioFormat getAudioFormat() {
        float sampleRate = 16000.0F;
        int sampleSizeInBits = 16;
        int channels = 2;
        boolean signed = true;
        boolean bigEndian = true;
        return new AudioFormat(sampleRate, sampleSizeInBits, channels, signed, bigEndian);
    }
    
       @Override
        public void run() {

        try {

            // initialize the socket
            DatagramSocket socket = new DatagramSocket(this.mcPort);
            System.out.println("The reciever is ready.");

            // create a new data packet
            DatagramPacket packet = new DatagramPacket(playBuffer, playBuffer.length);
         
         
            audioFormat = getAudioFormat();     //get the audio format

            DataLine.Info dataLineInfo1 = new DataLine.Info(SourceDataLine.class, audioFormat);
            sourceDataLine = (SourceDataLine) AudioSystem.getLine(dataLineInfo1);
            sourceDataLine.open(audioFormat);
            sourceDataLine.start();

            //Setting the maximum volume
            FloatControl control = (FloatControl) sourceDataLine.getControl(FloatControl.Type.MASTER_GAIN);
            control.setValue(control.getMaximum());

      

            while(true) {

                try {

                    // Receive 
                    socket.receive(packet);
                  
                    
                    sourceDataLine.write(packet.getData(), 0, 500); //playing the audio   

                } catch (IOException e) {
                }

            }

        } catch (SocketException e) {
        }  catch (LineUnavailableException ex) {
               Logger.getLogger(Recieve.class.getName()).log(Level.SEVERE, null, ex);
           }
    

    }
    
}
