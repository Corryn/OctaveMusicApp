package hci.com.octave;

/**
 * Created by botto_000 on 3/12/2016.
 */
public class Song {

    private long id;
    private long aid;
    private String title;
    private String artist;
    private String data;

    public Song(long songID, long albumID, String songTitle, String songArtist, String songData) {
        id = songID;
        aid = albumID;
        title = songTitle;
        artist = songArtist;
        data = songData;
    }

    public long getID(){return id;}
    public long getAlbumID(){return aid;}
    public String getTitle(){return title;}
    public String getArtist(){return artist;}
    public String getData(){return data;}
}
