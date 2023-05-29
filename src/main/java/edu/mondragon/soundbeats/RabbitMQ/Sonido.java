package edu.mondragon.soundbeats.RabbitMQ;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import javax.sound.sampled.*;
import javax.sound.sampled.AudioFormat.Encoding;


public class Sonido {

    private final CyclicBarrier barrier = new CyclicBarrier(2);
    private AudioInputStream audioIn;

    public Sonido(final Path wavPath){
        try {
            audioIn = AudioSystem.getAudioInputStream(wavPath.toFile());
        } catch (UnsupportedAudioFileException | IOException e) {
            e.printStackTrace();
        }
    }

    public Sonido(byte[] data, String audioFormat, long length){
        bytesAsonido(data, audioFormat, length);
    }

    public Sonido(AudioInputStream audioIn){
        this.audioIn = audioIn;
    }

    public byte[] sonidoAbytes() throws IOException{
        byte[] data = new byte[audioIn.available()];
        audioIn.read(data);

        //System.out.println(audioIn.getFrameLength());
        //System.out.println(audioIn.getFormat());
        return data;
    }

    public long getAudioLength(){
        return audioIn.getFrameLength();
    }

    public String getAudioFormat(){
        AudioFormat formato = audioIn.getFormat();
        String encoding = formato.getEncoding().toString();
        String sampleRate = String.valueOf(formato.getSampleRate());
        String sampleSizeInBits = String.valueOf(formato.getSampleSizeInBits());
        String channels = String.valueOf(formato.getChannels());
        String frameSize = String.valueOf(formato.getFrameSize());
        String bigEndian = String.valueOf(formato.isBigEndian());

        String clave = encoding + "," + sampleRate + "," + sampleSizeInBits + "," + channels + "," + frameSize + "," + bigEndian;
        System.out.println(clave);
        System.out.println(formato);

        return clave;
    }

    public void bytesAsonido(byte[] data, String af, long length){
        InputStream is=(InputStream)(new ByteArrayInputStream(data));

        String valores[] = af.split(",");
        AudioFormat audioFormat = crearFormato(valores[0], Float.valueOf(valores[1]), Integer.valueOf(valores[2]),
        Integer.valueOf(valores[3]), Integer.valueOf(valores[4]), Boolean.valueOf(valores[5]));

        //AudioFormat format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 8000, 16, 2, 4, 8000, false);

        audioIn = new AudioInputStream(is, audioFormat, length);

        /*InputStream oInstream = (InputStream)(new ByteArrayInputStream(data));
        try {
            System.out.println(AudioSystem.getAudioFileFormat(oInstream));
            
            audioIn = AudioSystem.getAudioInputStream(oInstream);
        } catch (UnsupportedAudioFileException | IOException e) {
            e.printStackTrace();
        }*/
    }

    public AudioFormat crearFormato(String encoding, float sampleRate, int sampleSizeInBits, int channels,
                                    int frameSize, boolean bigEndian){
        Encoding e = new Encoding(encoding);
        return new AudioFormat(e, sampleRate, sampleSizeInBits, channels, frameSize, sampleRate, bigEndian);
    }

    public String getSonidoString(){
        try {
            String audio = new String(this.sonidoAbytes());
            String formato = String.valueOf(this.getAudioFormat());
            String length = String.valueOf(this.getAudioLength());

            String clave =  audio + "SEPARACION" + formato + "SEPARACION" + length;

            return clave;
        } catch (IOException e) {
            
            e.printStackTrace();
        }        
        return "0";
    }

    /*********************CODIGO PARA REPRODUCIR EL SONIDO*************************/
    public void play(){
        try (final Clip clip = AudioSystem.getClip()) {
            listenerAudio(clip);
            clip.open(audioIn);
            
            clip.start();
            esperarFinalizar();
        } catch (LineUnavailableException | IOException e) {
            e.printStackTrace();
        }
    }

    private void listenerAudio(final Clip clip) {
        clip.addLineListener(event -> {
            if (event.getType() == LineEvent.Type.STOP) esperarEnBarrera();
        });
    }

    private void esperarEnBarrera() {
        try {
            barrier.await();
        } catch (final InterruptedException ignored) {
        } catch (final BrokenBarrierException e) {
            throw new RuntimeException(e);
        }
    }

    private void esperarFinalizar() {
        esperarEnBarrera();
    }
}
