/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package audioplayer;

import christophedelory.content.Content;
import christophedelory.playlist.AbstractPlaylistComponent;
import christophedelory.playlist.Media;
import christophedelory.playlist.Playlist;
import christophedelory.playlist.Sequence;
import christophedelory.playlist.SpecificPlaylist;
import christophedelory.playlist.SpecificPlaylistFactory;
import christophedelory.playlist.SpecificPlaylistProvider;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author catfly
 */
public class FileUtils {
    
    private static final Logger LOGGER = Logger.getLogger(FileUtils.class
                                                                   .getName());
    
    public static String getURI(File f) {
        return "file://" + f.getAbsolutePath();
    }
    
    public static String getURI(String fileName) {
        File f = new File(fileName);
        return getURI(f);
    }
    
    public static String getType(File f) {
        if (f.isFile()) {
            String[] fNameSplitted = f.getName().split("\\.", 0);
            if(fNameSplitted[fNameSplitted.length-1].equals("mp3")) {
                return "mp3";
            } else if (fNameSplitted[fNameSplitted.length-1].equals("m3u")){
                return "m3u";
            }
        } else if (f.isDirectory()) {
            return "dir";
        }
        return "UNRECOGNISED";
    }
    
    public static File[] toFileArray(File file) throws FileUnrecognisedException
                                                    , M3uFileInvalidException  {
        File[] fileArray = null;
        List<File> fileList = new ArrayList();
        
        if (file.isDirectory()) {
            fileArray = file.listFiles();
            
            for (File f : fileArray) {
                if(getType(f).equals("mp3")) {
                    fileList.add(f.getAbsoluteFile());
                }
            }
            
        } else if (file.isFile()) {
            if (getType(file).equals("m3u")) {
                SpecificPlaylist specificPlaylist = null;
                try {
                    specificPlaylist = SpecificPlaylistFactory.getInstance()
                                                              .readFrom(file);
                } catch (IOException e) {
                    throw new M3uFileInvalidException("Given m3u file is "
                                                       + "broken.");
                }
                Playlist genericPlaylist = specificPlaylist.toPlaylist();
                Sequence sequence = genericPlaylist.getRootSequence();
                AbstractPlaylistComponent[] mediaList 
                        = sequence.getComponents();
                
                for (AbstractPlaylistComponent component : mediaList) {
                    if (component.getClass() == Media.class) {
                        Media media = (Media) component;
                        File f = null;
                        try {
                            f = new File(media.getSource().getURI());
                        } catch (URISyntaxException ex) {
                            Logger.getLogger(FileUtils.class.getName())
                                  .log(Level.SEVERE, "URI Syntax is broken", ex);
                            f = new File("");
                        }
                        if(getType(f).equals("mp3")) {
                            fileList.add(f);
                        }
                    }
                }
            } else if (getType(file).equals("mp3")) {
                fileList.add(file);
            } else {
                throw new FileUnrecognisedException("File given is not any of "
                                                + ": mp3, m3u, dir");
            }
        } else {
            throw new FileUnrecognisedException("File given is not any of "
                                                + ": mp3, m3u, dir"); 
       }
        
        fileArray = fileList.toArray(new File[fileList.size()]);
        return fileArray;
    }
    
    public static void exportToM3U(List<File> fileList, File m3uFile
                                   , boolean isOverwrite) 
                                            throws M3uFileInvalidException
                                                   , IOException {
        try {
            m3uFile.createNewFile();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "File existed.", e);
        }
        
        SpecificPlaylist specificPlaylist = null;
        try {
            specificPlaylist = SpecificPlaylistFactory
                    .getInstance()
                    .readFrom(m3uFile);
        } catch (IOException e) {
            throw new M3uFileInvalidException("Given m3u file is "
                                                       + "broken.");
        }
        
        Playlist genericPlaylist = specificPlaylist.toPlaylist();
        Sequence sequence = genericPlaylist.getRootSequence();
        
        if (isOverwrite) {
            while (sequence.getComponentsNumber() > 0) {
                sequence.removeComponent(0);
            }
        }
        
        for(File f : fileList) {
            Media newMedia = new Media();
            newMedia.setSource(new Content(getURI(f)));
            sequence.addComponent(newMedia); 
        }
        
        SpecificPlaylistProvider provider = SpecificPlaylistFactory
                                                    .getInstance()
                                                    .findProviderById("m3u");
        try {
            SpecificPlaylist newSpecificPlaylist 
                    = provider.toSpecificPlaylist(genericPlaylist);
            FileOutputStream out = new FileOutputStream(m3uFile);

            newSpecificPlaylist.writeTo(out, null);
            out.close();
        } catch (Exception e) {
            throw new IOException("Cannot write m3u file.");
        }
    }
    
    public static void exportToM3U(List<File> fileList, File m3uFile) 
            throws IOException, M3uFileInvalidException {
        exportToM3U(fileList, m3uFile, true);
    }

    public static class FileUnrecognisedException extends Exception {

        public FileUnrecognisedException(String file_unrecognised) {
        }
    }

    public static class M3uFileInvalidException extends Exception {

        public M3uFileInvalidException(String string) {
        }
    }
    
}
