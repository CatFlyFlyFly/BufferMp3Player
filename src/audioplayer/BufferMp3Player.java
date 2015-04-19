package audioplayer;



/*
 *  SimpleAudioPlayer.java
 *
 *  This file is part of jsresources.org
 */

/*
 * Copyright (c) 1999 - 2001 by Matthias Pfisterer
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * - Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */

/*
|<---            this code is formatted to fit into 80 columns             --->|
*/

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;
import org.apache.commons.io.IOUtils;

public class BufferMp3Player
{
    private static final Logger LOGGER = Logger.getLogger(
                                                    BufferMp3Player.class
                                                                   .getName());
    
    private static final int    EXTERNAL_BUFFER_SIZE = 128000;
    private static final boolean bInterpretFilenameAsUrl = false;
    private static final boolean bForceConversion = false;
    private static final boolean bBigEndian = false;
    private static final int nSampleSizeInBits = 16;
    
    private AudioFormat mp3Format;
    private SourceDataLine line;
    private FloatControl gainControl;
    
    private byte[] AudioBytesArray;
    
    private PlayFileTask pft;
    private Thread pfThread;
    
    private int offset;
    private int len;

    public BufferMp3Player() {
        File initFile = new File("res/init_mp3_format.mp3");
        AudioInputStream    audioInputStream = null;
        try {
            audioInputStream = AudioSystem.getAudioInputStream(initFile);
        } catch (UnsupportedAudioFileException ex) {
            Logger.getLogger(BufferMp3Player.class.getName())
                  .log(Level.SEVERE, "Unsupport Audio File. Really ?", ex);
        } catch (IOException ex) {
            Logger.getLogger(BufferMp3Player.class.getName())
                  .log(Level.SEVERE, null, ex);
        }
        AudioFormat audioFormat = audioInputStream.getFormat();
        mp3Format = new AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED,
            audioFormat.getSampleRate(),
            nSampleSizeInBits,
            audioFormat.getChannels(),
            audioFormat.getChannels() * (nSampleSizeInBits / 8),
            audioFormat.getSampleRate(),
            bBigEndian);
        line = getSourceDataLine(null, mp3Format, AudioSystem.NOT_SPECIFIED);
        gainControl = (FloatControl) line.getControl
                                        (FloatControl.Type.MASTER_GAIN);
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(double percent) {
        offset = (int) (percent / 100 * len) ;
        offset -= (offset % EXTERNAL_BUFFER_SIZE);
    }

    public int getLen() {
        return len;
    }
    
    public byte[] initAudio(File file) throws IOException, UnsupportedAudioFileException
    {
        AudioInputStream    audioInputStream = null;
        try {
            audioInputStream = AudioSystem.getAudioInputStream(file);
        } catch (UnsupportedAudioFileException ex) {
            Logger.getLogger(BufferMp3Player.class.getName())
                  .log(Level.SEVERE, "Unsupport Audio File. Really ?", ex);
        } catch (IOException ex) {
            Logger.getLogger(BufferMp3Player.class.getName())
                  .log(Level.SEVERE, "Something wrong with : " 
                                     + file.getAbsolutePath(), ex);
        }
//        AudioFormat audioFormat = audioInputStream.getFormat();
//        mp3Format = new AudioFormat(
//            AudioFormat.Encoding.PCM_SIGNED,
//            audioFormat.getSampleRate(),
//            nSampleSizeInBits,
//            audioFormat.getChannels(),
//            audioFormat.getChannels() * (nSampleSizeInBits / 8),
//            audioFormat.getSampleRate(),
//            bBigEndian);
//        line = getSourceDataLine(null, mp3Format, AudioSystem.NOT_SPECIFIED);
//        gainControl = (FloatControl) line.getControl
//                                        (FloatControl.Type.MASTER_GAIN);
        audioInputStream = AudioSystem.getAudioInputStream(mp3Format , audioInputStream);

        // NOTED THAT I THINK USING ByteArray SUCKS
        
        /*
           THIS LINE IS FUCKING SLOW
        */
        
        byte[] bufferArray = null;
        
        try {
            bufferArray = IOUtils.toByteArray(audioInputStream);
        } catch (IOException ex) {
            Logger.getLogger(BufferMp3Player.class.getName())
                  .log(Level.SEVERE, null, ex);
        }    
        /*
           THIS LINE IS FUCKING SLOW
        */
        
        return bufferArray;
    }
    
    public void startFile(byte[] byteArray) {
        AudioBytesArray = byteArray;
        offset = 0; 
        len = AudioBytesArray.length;
        pfThread = new Thread(new PlayFileTask());
        line.start();
    }
    
    public static float toAmplitude(float decibel) {
        return (float) Math.pow(10.0, decibel/20.0);
    }
    
    public static float toDecibel(float amplitude) {
        return (float) (20.0 * Math.log10(amplitude));
    }
    
    public void volumnControl(int percent) {
        float range = toAmplitude(gainControl.getMaximum()) 
                      - toAmplitude(gainControl.getMinimum());
        float value = toAmplitude(gainControl.getMinimum()) 
                      + range * percent / 100;
        gainControl.setValue(toDecibel(value));
    }
    
    public void playFile() {
        pft = new PlayFileTask();
        line.start();
        new Thread(pft).start();
//        pfThread = new Thread(new PlayFileTask());
//        pfThread.start();
    }
    
    public void pauseFile() {
        pft.pause();
//        pfThread.interrupt();
    }
    
    private class PlayFileTask implements Runnable {
        
        private volatile boolean isRunning;
        private int bufferSize;
        public PlayFileTask() {
            isRunning = true;
            bufferSize = EXTERNAL_BUFFER_SIZE;
        }

        /*
         * NOTED THAT I WANT TO USE INTERRUPTED METHOD. I DONT KNOW WHY 
         * IT DIDNT WORK. SAVED IT FOR LATER.
        */
        
        /*
           NOTED THAT I THINK USING ByteArray SUCKS
        */
        
        @Override
        public void run()  {
//            while (!Thread.currentThread().isInterrupted() && (offset != len)){
            while (isRunning && (offset != len)) {
                if (offset + EXTERNAL_BUFFER_SIZE > len) {
                    bufferSize = len - offset;
                } 
                line.write(AudioBytesArray, offset, bufferSize);
                offset += bufferSize;
            }
            
            line.drain();
            
//            if (offset == len) {    
//                line.close();
//            }
        }
        
        public void pause() {
            isRunning = false;
            offset -= bufferSize;
        }
    }

    private SourceDataLine getSourceDataLine(String strMixerName,
                            AudioFormat audioFormat,
                            int nBufferSize) {
        SourceDataLine  line = null;
        DataLine.Info   info = new DataLine.Info(SourceDataLine.class,
                             audioFormat, nBufferSize);
        try {
            if (strMixerName != null) {
                Mixer.Info  mixerInfo = AudioCommon.getMixerInfo(strMixerName);
                if (mixerInfo == null)
                {
                    out("AudioPlayer: mixer not found: " + strMixerName);
                    System.exit(1);
                }
                Mixer   mixer = AudioSystem.getMixer(mixerInfo);
                line = (SourceDataLine) mixer.getLine(info);
            } else {
                line = (SourceDataLine) AudioSystem.getLine(info);
            }

            line.open(audioFormat, nBufferSize);
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return line;
    }
    
    private static void out(String strMessage)
    {
        System.out.println(strMessage);
    }
}



/*** SimpleAudioPlayer.java ***/

