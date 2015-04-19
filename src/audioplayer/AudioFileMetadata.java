/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package audioplayer;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.audio.mp3.MP3File;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.id3.AbstractID3v2Tag;
import org.jaudiotagger.tag.id3.ID3v1Tag;
import org.jaudiotagger.tag.id3.ID3v24Frames;
import org.jaudiotagger.tag.id3.ID3v24Tag;
import org.jaudiotagger.tag.images.Artwork;

/**
 *
 * @author catfly
 */
public class AudioFileMetadata {
    
    private static final Logger LOGGER = Logger.getLogger(
                                                AudioFileMetadata.class
                                                                 .getName());
    
    private AudioHeader audioHeader;
    private Tag tag;
    
    private File file;
    
    private String strFilename;
    private String artistName;
    private String albumName;
    private String songName;
    private Image albumCover;
    private int trackLength;
    
    private Image IMAGE_NOPIC;
    private static final String IMAGE_NOPIC_FILENAME = "res/no_pic.png";

    public AudioFileMetadata() {
        File imageFile = new File(IMAGE_NOPIC_FILENAME);
        try {
            IMAGE_NOPIC = ImageIO.read(imageFile);
        } catch (IOException ex) {
            Logger.getLogger(AudioFileMetadata.class.getName())
                  .log(Level.SEVERE, "Something wrong with : "
                                     + imageFile.getAbsolutePath(), ex);
            System.exit(-1);
        }
    }
    
    public String getStrFilename() {
        return strFilename;
    }

    public String getArtistName() {
        return artistName;
    }

    public String getAlbumName() {
        return albumName;
    }

    public String getSongName() {
        return songName;
    }

    public Image getAlbumCover() {
        return albumCover;
    }
    
    public int getTrackLength() {
        return trackLength;
    }
    
    public void setStrFilename(String strFilename) {
        this.strFilename = strFilename;
        this.file = new File(getStrFilename());
        setMetadata();
    }
    
    public void setFile(File file) {
        this.file = file;
        this.strFilename = file.getName();
        setMetadata();
    }
    
    private void setMetadata() {
        MP3File f = null;
        try {
            f = (MP3File) AudioFileIO.read(file);
        } catch (IOException e) {
            Logger.getLogger(AudioFileMetadata.class.getName()).log(Level.SEVERE, null, e);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        } 
        
        tag = f.getTag();
        ID3v1Tag         v1Tag  = f.getID3v1Tag();
        AbstractID3v2Tag v2Tag  = f.getID3v2Tag();
        ID3v24Tag        v24Tag = f.getID3v2TagAsv24();
        audioHeader = f.getAudioHeader();
        setArtistName(v2Tag);
        setAlbumName(v2Tag);
        setSongName(v2Tag);
        try {
            setAlbumCover(v2Tag);
        } catch (IOException ex) {
            Logger.getLogger(AudioFileMetadata.class.getName())
                  .log(Level.SEVERE, "Something wrong with album cover.", ex);
        }
        setTrackLength(audioHeader);
    }

    private void setArtistName(AbstractID3v2Tag tag) {
        if (tag != null) {
            String name = tag.getFirst(ID3v24Frames.FRAME_ID_ARTIST);
            if (!name.isEmpty()) {
                this.artistName = name;
            } else {
                this.artistName = "Unknown Artist";
            }
        } else {
            this.artistName = "Unknown Artist";
        } 
    }

    private void setAlbumName(AbstractID3v2Tag tag) {
        if (tag != null) {
            String name = tag.getFirst(ID3v24Frames.FRAME_ID_ALBUM);
            if (!name.isEmpty()) {
                this.albumName = name;
            } else {
                this.albumName = "Unknown Album";
            }
        } else {
            this.albumName = "Unknown Album";
        } 
    }

    private void setSongName(AbstractID3v2Tag tag) {
        if (tag != null) {
            String name = tag.getFirst(ID3v24Frames.FRAME_ID_TITLE);
            if (!name.isEmpty()) {
                this.songName = name;
            } else {
                this.songName = getStrFilename();
            }
        } else {
            this.songName = getStrFilename();
        } 
    }
    
    public void setAlbumCover(AbstractID3v2Tag tag) throws IOException {
        Image artImage = null;
        if (tag != null) {
            Artwork artwork = tag.getFirstArtwork();
            if (artwork != null) {
                artImage = (BufferedImage) artwork.getImage();
            } else {
                artImage = IMAGE_NOPIC;
            }
        } else {
            artImage = IMAGE_NOPIC;
        }
        albumCover = artImage;
    }

    private void setTrackLength(AudioHeader audioHeader) {
        this.trackLength = audioHeader.getTrackLength();
    }
    
}
