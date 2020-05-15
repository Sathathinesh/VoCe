import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
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

public class MultiServer extends Thread {
    
    MulticastSocket s,ss ;
  final InetAddress mcIPAddress ;
  //final int mcPort;
  boolean stopCapture = false;
  ByteArrayOutputStream byteArrayOutputStream;
  AudioFormat audioFormat;
  TargetDataLine targetDataLine;
  AudioInputStream InputStream;
  SourceDataLine sourceLine;
  byte tempBuffer[] = new byte[10000];
  byte[] receiveData = new byte[10000];
  final int mcPort;
  //int ttl = 1;
    public static void main(String[] args) throws UnknownHostException, IOException {
    
	int mcPort = 12345;
        if (args.length != 1) {
            System.out.println("Not enough arguments ... include Group IP.. ");
            return;
        }

        try {

            Thread cap = new Thread(new MultiServer(mcPort,InetAddress.getByName(args[0])));
            cap.start();
        } catch (Exception e) {
            e.printStackTrace();
        }

  /*  System.out.println("Multicast Receiver running at:"
        + mcSocket.getLocalSocketAddress());*/    
    }

  @Override
    public void run(){
      try {
          this.s = new MulticastSocket(mcPort);
		  this.ss = new MulticastSocket(mcPort);
		  ss.joinGroup(this.mcIPAddress);
		     captureAudio();
  //  s.setLoopbackMode(true);
            runVOIP();
      } catch (IOException ex) {
          Logger.getLogger(MultiServer.class.getName()).log(Level.SEVERE, null, ex);
      }

 

    }
    
    public MultiServer( int mcPort,InetAddress mcIPAddress) {

        this.mcPort=mcPort;
        this.mcIPAddress=mcIPAddress;
    }
        
    private AudioFormat getAudioFormat() {
        float sampleRate = 16000.0F;
        int sampleSizeInBits = 16;
        int channels = 2;
        boolean signed = true;
        boolean bigEndian = true;
        return new AudioFormat(sampleRate, sampleSizeInBits, channels, signed, bigEndian);
    }

    public void captureAudio() {

        try {
    
        AudioFormat  adFormat = getAudioFormat();
        DataLine.Info dataLineInfo = new DataLine.Info(TargetDataLine.class, adFormat);
        targetDataLine = (TargetDataLine) AudioSystem.getLine(dataLineInfo);
        targetDataLine.open(adFormat);
        targetDataLine.start();
            Thread captureThread = new Thread(new CaptureThread());
            captureThread.start();


        } catch (LineUnavailableException e) {
            System.out.println(e);
            System.exit(0);
        }

    }

   
       
class CaptureThread extends Thread {

    byte tempBuffer[] = new byte[10000];
    //int ttl =1;
    @Override
    public void run() {

        byteArrayOutputStream = new ByteArrayOutputStream();
        stopCapture = false;
        try {
         
            while (!stopCapture) {
                int cnt = targetDataLine.read(tempBuffer, 0, tempBuffer.length);
                if (cnt > 0) {
                    DatagramPacket sendPacket = new DatagramPacket(tempBuffer, tempBuffer.length, mcIPAddress, mcPort);
                    ss.send(sendPacket);
                    ss.setLoopbackMode(true);
                  
                   // byteArrayOutputStream.write(tempBuffer, 0, cnt);
                }
            }
            byteArrayOutputStream.close();
        } catch (IOException e) {
            System.out.println("CaptureThread::run()" + e);
            System.exit(0);
        }
    }
}
public void runVOIP() {
    try {
      
        
        while (true) {
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            ss.receive(receivePacket);
            ss.setLoopbackMode(true);
           // System.out.println("RECEIVED: " + receivePacket.getAddress().getHostAddress() + " " + receivePacket.getPort());
            try {
                byte audioData[] = receivePacket.getData();
                InputStream byteInputStream = new ByteArrayInputStream(audioData);
                AudioFormat adFormat = getAudioFormat();
             //   AudioInputStream InputStream;
                InputStream = new AudioInputStream(byteInputStream, adFormat, audioData.length / adFormat.getFrameSize());
                DataLine.Info dataLineInfo = new DataLine.Info(SourceDataLine.class, adFormat);
                sourceLine = (SourceDataLine) AudioSystem.getLine(dataLineInfo);
                sourceLine.open(adFormat);
                sourceLine.start();
                FloatControl control = (FloatControl)sourceLine.getControl(FloatControl.Type.MASTER_GAIN);
                control.setValue(control.getMaximum());
                Thread playThread = new Thread(new PlayThread());
                playThread.start();
            } catch (LineUnavailableException e) {
                System.out.println(e);
                System.exit(0);
            }
        }
    } catch (IOException e) {
    }
}

class PlayThread extends Thread {


    @Override
    public void run() {
        try {
			byteArrayOutputStream = new ByteArrayOutputStream();
            int cnt;
            while ((cnt = InputStream.read(receiveData, 0, receiveData.length)) != -1) {
                if (cnt > 0) {
					byteArrayOutputStream.write(receiveData, 0, cnt);
                    sourceLine.write(receiveData, 0, cnt);
                }
            }
            //  sourceLine.drain();
            // sourceLine.close();
        } catch (IOException e) {
            System.out.println(e);
            System.exit(0);
        }
    }
}
}
