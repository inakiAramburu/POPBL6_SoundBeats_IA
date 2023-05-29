package edu.mondragon.soundbeats.RabbitMQ;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

public class Wav {

    public static void main(String[] args) throws LineUnavailableException, IOException, UnsupportedAudioFileException {
        
        Path path = Paths.get("C:/Users/lamaa/OneDrive/Escritorio/SoundBeats/file_example_WAV_1MG.wav");
        //Path path = Paths.get("C:/Users/lamaa/OneDrive/Escritorio/Aunlabelledtest__201101152256.wav");
        Sonido sonido = new Sonido(path);

        String audio = new String(sonido.sonidoAbytes());
        String formato = String.valueOf(sonido.getAudioFormat());
        String length = String.valueOf(sonido.getAudioLength());

        String clave =  audio + "SEPARACION" + formato + "SEPARACION" + length;
        String valores[] = clave.split("SEPARACION");

        sonido.bytesAsonido(valores[0].getBytes(), valores[1], Long.valueOf(valores[2]));
        System.out.println(sonido.getAudioLength());
        sonido.play();
    }
}
