package audioplayer;

import christophedelory.content.Content;
import christophedelory.content.ContentMetadataCenter;
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
import static java.lang.System.out;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;


public class test {

	public static void main(String[] args) throws URISyntaxException, MalformedURLException, IOException, Exception {
		// TODO Auto-generated method stub
            AudioPlayer.main(new String[]{"-D", "love somebody.mp3"});
            
            BufferMp3Player player = new BufferMp3Player();
            File file = new File("getsunova.mp3");
            
            byte[] bufferArray = null;
            
            
            try {
                bufferArray = player.initAudio(file);
            } catch (Exception e) {
		e.printStackTrace();
                System.exit(-1);
            }
            player.startFile(bufferArray);
            player.volumnControl(5);
            player.playFile();
	}

}
