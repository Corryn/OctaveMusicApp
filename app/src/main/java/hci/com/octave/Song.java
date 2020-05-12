package hci.com.octave;

public class Song {

    private long id;
    private long albumId;
    private String title;
    private String artist;
    private String data;

    public Song(long songID, long aId, String songTitle, String songArtist, String songData) {
        id = songID;
        albumId = aId;
        title = songTitle;
        artist = songArtist;
        data = songData;
    }

    public long getID(){return id;}
    public long getAlbumID(){return albumId;}
    public String getTitle(){return title;}
    public String getArtist(){return artist;}
    public String getData(){return data;}
}
