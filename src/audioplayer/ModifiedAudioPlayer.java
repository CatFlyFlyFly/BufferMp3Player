package audioplayer;



/*
 *  AudioPlayer.java
 *
 *  This file is part of jsresources.org
 */

/*
 * Copyright (c) 1999, 2000 by Matthias Pfisterer
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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.IOException;

import java.net.URL;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;

import gnu.getopt.Getopt;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.IOUtils;

public class ModifiedAudioPlayer
{
    private static boolean  DEBUG = false;

    private static int  DEFAULT_EXTERNAL_BUFFER_SIZE = 128000;
    
    private SourceDataLine line;

    public ModifiedAudioPlayer() {
    
    }

    public void initSong(String[] args)
        throws Exception
    {
        boolean bInterpretFilenameAsUrl = false;
        boolean bForceConversion = false;
        boolean bBigEndian = false;
        int nSampleSizeInBits = 16;


        String  strMixerName = null;

        int nExternalBufferSize = DEFAULT_EXTERNAL_BUFFER_SIZE;

        int nInternalBufferSize = AudioSystem.NOT_SPECIFIED;

        Getopt  g = new Getopt("AudioPlayer", args, "hlufM:e:i:E:S:D");
        int c;
        while ((c = g.getopt()) != -1)
        {
            switch (c)
            {
            case 'h':
                printUsageAndExit();

            case 'l':
                AudioCommon.listMixersAndExit(true);

            case 'u':
                bInterpretFilenameAsUrl = true;
                break;

            case 'f':
                bInterpretFilenameAsUrl = false;
                break;

            case 'M':
                strMixerName = g.getOptarg();
                if (DEBUG) out("AudioPlayer.main(): mixer name: " + strMixerName);
                break;

            case 'e':
                nExternalBufferSize = Integer.parseInt(g.getOptarg());
                break;

            case 'i':
                nInternalBufferSize = Integer.parseInt(g.getOptarg());
                break;

            case 'E':
                String strEndianess = g.getOptarg();
                strEndianess = strEndianess.toLowerCase();
                if (strEndianess.equals("big"))
                {
                    bBigEndian = true;
                }
                else if (strEndianess.equals("little"))
                {
                    bBigEndian = false;
                }
                else
                {
                    printUsageAndExit();
                }
                bForceConversion = true;
                break;

            case 'S':
                nSampleSizeInBits = Integer.parseInt(g.getOptarg());
                bForceConversion = true;
                break;

            case 'D':
                DEBUG = true;
                break;

            case '?':
                printUsageAndExit();

            default:
                out("getopt() returned " + c);
                break;
            }
        }

        String  strFilenameOrUrl = null;
        for (int i = g.getOptind(); i < args.length; i++)
        {
            if (strFilenameOrUrl == null)
            {
                strFilenameOrUrl = args[i];
            }
            else
            {
                printUsageAndExit();
            }
        }
        if (strFilenameOrUrl == null)
        {
            printUsageAndExit();
        }

        AudioInputStream audioInputStream = null;
        if (bInterpretFilenameAsUrl)
        {
            URL url = new URL(strFilenameOrUrl);
            audioInputStream = AudioSystem.getAudioInputStream(url);
        }
        else
        {
            if (strFilenameOrUrl.equals("-"))
            {
                InputStream inputStream = new BufferedInputStream(System.in);
                audioInputStream = AudioSystem.getAudioInputStream(inputStream);
            }
            else
            {
                File file = new File(strFilenameOrUrl);
                audioInputStream = AudioSystem.getAudioInputStream(file);
            }
        }
    
        if (DEBUG) out("AudioPlayer.main(): primary AIS: " + audioInputStream);
        
        AudioFormat audioFormat = audioInputStream.getFormat();
        if (DEBUG) out("AudioPlayer.main(): primary format: " + audioFormat);
        DataLine.Info   info = new DataLine.Info(SourceDataLine.class,
                             audioFormat, nInternalBufferSize);
        boolean bIsSupportedDirectly = AudioSystem.isLineSupported(info);
        
        if (!bIsSupportedDirectly || bForceConversion) {
            AudioFormat sourceFormat = audioFormat;
            AudioFormat targetFormat = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                sourceFormat.getSampleRate(),
                nSampleSizeInBits,
                sourceFormat.getChannels(),
                sourceFormat.getChannels() * (nSampleSizeInBits / 8),
                sourceFormat.getSampleRate(),
                bBigEndian);
            if (DEBUG)
            {
                out("AudioPlayer.main(): source format: " + sourceFormat);
                out("AudioPlayer.main(): target format: " + targetFormat);
            }
            audioInputStream = AudioSystem.getAudioInputStream(targetFormat, audioInputStream);
            audioFormat = audioInputStream.getFormat();
            if (DEBUG) out("AudioPlayer.main(): converted AIS: " + audioInputStream);
            if (DEBUG) out("AudioPlayer.main(): converted format: " + audioFormat);
        }

        line = getSourceDataLine(strMixerName, audioFormat, nInternalBufferSize);
        if (line == null)
        {
            out("AudioPlayer: cannot get SourceDataLine for format " + audioFormat);
            System.exit(1);
        }
        if (DEBUG) out("AudioPlayer.main(): line: " + line);
        if (DEBUG) out("AudioPlayer.main(): line format: " + line.getFormat());
        if (DEBUG) out("AudioPlayer.main(): line buffer size: " + line.getBufferSize());
        
        line.start();
        
        /*
         *          REAL WORK START BELOW
         *
         *                   |
         *                   |
         *                  \|/ 
         *                   V
         */

        int nBytesRead = 0;
        byte[]  abData = new byte[nExternalBufferSize];
        if (DEBUG) out("AudioPlayer.main(): starting main loop");
        byte[]  bigData = IOUtils.toByteArray(audioInputStream);
        System.out.println("Length : " + bigData.length);
        int off = (bigData.length - 20000);
        //Thread t = new Thread(new playSongTask());
        //t.start();
        line.write(bigData, off, bigData.length - off);
        
        while (nBytesRead != -1)
        {
            try
            {
                nBytesRead = audioInputStream.read(abData, 0, abData.length);
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
            if (DEBUG) out("AudioPlayer.main(): read from AudioInputStream (bytes): " + nBytesRead);
            if (nBytesRead >= 0)
            {
                int nBytesWritten = line.write(abData, 0, nBytesRead);
                if (DEBUG) out("AudioPlayer.main(): written to SourceDataLine (bytes): " + nBytesWritten);
            }
        }
        
        

        if (DEBUG) out("AudioPlayer.main(): finished main loop");
        if (DEBUG) out("AudioPlayer.main(): before drain");
        line.drain();
        if (DEBUG) out("AudioPlayer.main(): before close");
        line.close();
    }
    
    private class aoeu {
        private SourceDataLine line;
        private AudioInputStream ais;

        public aoeu(SourceDataLine line, AudioInputStream ais) {
            this.line = line;
            this.ais = ais;
        }
        
    }
    
    /*
    private static class playSongTask implements Runnable {
        private volatile boolean    isRunning = true;
        private byte[]              b;
        private SourceDataLine      line;
        
        public playSongTask(byte[] b, SourceDataLine line) {
            this.b = b;
            this.line = line;
        }

        private playSongTask() {
            
        }
        
        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                System.out.println("HELLO ?");
                System.out.println("YEAH");
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException ex) {
                    Logger.getLogger(ModifiedAudioPlayer.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        
        public void cancel() {
            interrupt();
        }
    }
    */

    // TODO: maybe can used by others. AudioLoop?
    // In this case, move to AudioCommon.
    private SourceDataLine getSourceDataLine(String strMixerName,
                            AudioFormat audioFormat,
                            int nBufferSize)
    {
        /*
         *  Asking for a line is a rather tricky thing.
         *  We have to construct an Info object that specifies
         *  the desired properties for the line.
         *  First, we have to say which kind of line we want. The
         *  possibilities are: SourceDataLine (for playback), Clip
         *  (for repeated playback) and TargetDataLine (for
         *   recording).
         *  Here, we want to do normal playback, so we ask for
         *  a SourceDataLine.
         *  Then, we have to pass an AudioFormat object, so that
         *  the Line knows which format the data passed to it
         *  will have.
         *  Furthermore, we can give Java Sound a hint about how
         *  big the internal buffer for the line should be. This
         *  isn't used here, signaling that we
         *  don't care about the exact size. Java Sound will use
         *  some default value for the buffer size.
         */
        SourceDataLine  line = null;
        DataLine.Info   info = new DataLine.Info(SourceDataLine.class,
                             audioFormat, nBufferSize);
        try
        {
            if (strMixerName != null)
            {
                Mixer.Info  mixerInfo = AudioCommon.getMixerInfo(strMixerName);
                if (mixerInfo == null)
                {
                    out("AudioPlayer: mixer not found: " + strMixerName);
                    System.exit(1);
                }
                Mixer   mixer = AudioSystem.getMixer(mixerInfo);
                line = (SourceDataLine) mixer.getLine(info);
            }
            else
            {
                line = (SourceDataLine) AudioSystem.getLine(info);
            }

            /*
             *  The line is there, but it is not yet ready to
             *  receive audio data. We have to open the line.
             */
            line.open(audioFormat, nBufferSize);
        }
        catch (LineUnavailableException e)
        {
            if (DEBUG) e.printStackTrace();
        }
        catch (Exception e)
        {
            if (DEBUG) e.printStackTrace();
        }
        return line;
    }



    private static void printUsageAndExit()
    {
        out("AudioPlayer: usage:");
        out("\tjava AudioPlayer -h");
        out("\tjava AudioPlayer -l");
        out("\tjava AudioPlayer");
        out("\t\t[-M <mixername>]");
        out("\t\t[-e <externalBuffersize>]");
        out("\t\t[-i <internalBuffersize>]");
        out("\t\t[-S <SampleSizeInBits>]");
        out("\t\t[-B (big | little)]");
        out("\t\t[-D]");
        out("\t\t[-u | -f]");
        out("\t\t<soundfileOrUrl>");
        System.exit(1);
    }



    private static void out(String strMessage)
    {
        System.out.println(strMessage);
    }
}



/*** AudioPlayer.java ***/

