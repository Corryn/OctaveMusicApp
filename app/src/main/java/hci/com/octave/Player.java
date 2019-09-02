package hci.com.octave;

import android.app.Application;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/**
 * Created by botto_000 on 3/12/2016.
 */
public class Player extends Application {

    private static List<Song> songList = new ArrayList<>();
    private static List<String> artistList = new ArrayList<>();
    private static HashMap<String, ArrayList<Song>> byArtist = new HashMap<>();
    private static List<Song> activeList;
    private static List<Song> viewedList;
    private static LinkedList<Song> playlist = new LinkedList<>();
    private static MediaPlayer player = new MediaPlayer();
    private static Context context;
    private static int nowPlaying = -1;
    private static Song songNowPlaying;
    private static int selected = -1;
    private static boolean active = false;
    private static boolean repeat = false;
    private static boolean shuffle = false;
    private static boolean playClicked = false;
    private static boolean itemColor = false;
    private static boolean searching = false;

    public static void setActive() {
        active = true;
    }

    public static boolean exists() {
        return active;
    }

    public static List<Song> getSongList() {
        return songList;
    }

    public static MediaPlayer getPlayer() {
        return player;
    }

    public static List<String> getArtistList() {
        return artistList;
    }

    public static void setActiveList(List<Song> l) {
        activeList = l;
    }

    public static List<Song> getActiveList() { return activeList; }

    public static void setViewedList(List<Song> l) { viewedList = l; }

    public static List<Song> getViewedList() { return viewedList; }

    public static HashMap<String, ArrayList<Song>> getByArtistList() { return byArtist; }

    public static boolean isSearching() {
        return searching;
    }

    public static void setSearching(boolean b) {
        searching = b;
    }

    public static void pauseSong() {
        player.pause();
    }

    public static void unpauseSong() {
        player.start();
    }

    public static boolean getItemColor() {
        itemColor = !itemColor;
        return itemColor;
    }

    public static void resetItemColor() {
        itemColor = false;
    }

    public static void setPlayClicked(boolean val) {
        playClicked = val;
    }

    public static boolean getPlayClicked() {
        return playClicked;
    }

    public static boolean isPaused() {
        return !player.isPlaying();
    }

    public static void preparePlayer() {
        player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                nextSong();
            }
        });
    }

    public static void setSelected(int i) {
        selected = i;
    }

    public static int getSelected() {
        return selected;
    }

    public static Song getSelectedSong() {
        if(selected != -1) {
            return songList.get(selected);
        }
        else return null;
    }

    public static void nextSong() {
        if (!playlist.isEmpty()) {
            setSong(removeFromPlaylist());
        }
        else if(shuffle) {
            shuffle();
        }
        else if(nowPlaying + 1 < activeList.size()) {
            setSong(++nowPlaying);
        }
        else {
            nowPlaying = 0;
            setSong(nowPlaying);
        }
    }

    public static void prevSong() {
        if(playlist.isEmpty()) {
            if (shuffle) {
                shuffle();
            } else if (nowPlaying - 1 >= 0) {
                setSong(--nowPlaying);
            } else {
                nowPlaying = activeList.size() - 1;
                setSong(nowPlaying);
            }
        }
    }

    public static boolean playlistIsEmpty() {
        return playlist.isEmpty();
    }

    public static void addToPlaylist(Song s) {
        playlist.add(s);
    }

    public static Song removeFromPlaylist() {
        if(!playlist.isEmpty()) {
            return playlist.remove();
        }
        else {
            return null;
        }
    }

    public static Song playlistNext() {
        if(!playlist.isEmpty()) {
            return playlist.getFirst();
        }
        else return null;
    }

    public static void shuffle() {
        int random;
        Random r = new Random();
        random = r.nextInt(activeList.size());
        setSong(random);
    }

    public static boolean toggleRepeat() {
        repeat = !repeat;
        player.setLooping(repeat);
        return repeat;
    }

    public static boolean toggleShuffle() {
        shuffle = !shuffle;
        return shuffle;
    }

    public static void updateContext(Context c) {
        context = c;
    }

    public static void setSong(int s) {
        Uri uri = null;
        Song temp = null;
        try {
            temp = activeList.get(s);
            uri = Uri.parse("file:///" + temp.getData());
        }
        catch (Exception e) {
            return;
        }
        try {
            player.reset();
            player.setAudioStreamType(AudioManager.STREAM_MUSIC);
            player.setDataSource(context, uri);
            player.prepare();
            player.start();
            if(repeat) { player.setLooping(true); }
            nowPlaying = s;
            songNowPlaying = temp;
            Toast.makeText(context, "Now Playing: " + activeList.get(s).getTitle() + " - " + activeList.get(s).getArtist(), Toast.LENGTH_SHORT).show();
        }
        catch(IOException e) {
            Toast.makeText(context, "File not found!", Toast.LENGTH_SHORT).show();
        }
    }

    public static void setSong(Song s) {
        Uri uri = null;
        try {
            uri = Uri.parse("file:///" + s.getData());
        }
        catch (Exception e) {
            return;
        }
        try {
            player.reset();
            player.setAudioStreamType(AudioManager.STREAM_MUSIC);
            player.setDataSource(context, uri);
            player.prepare();
            player.start();
            if(repeat) { player.setLooping(true); }
            nowPlaying = activeList.indexOf(s);
            songNowPlaying = s;
            Toast.makeText(context, "Now Playing: " + s.getTitle() + " - " + s.getArtist(), Toast.LENGTH_SHORT).show();
        }
        catch(IOException e) {
            Toast.makeText(context, "File not found!", Toast.LENGTH_SHORT).show();
        }
    }

    public static SongAdapter filterSongs(String query) {
        ArrayList<Song> result = new ArrayList<>();
        for(Song s : viewedList) {
            if(s.getTitle().toLowerCase().contains(query.toLowerCase())) {
                result.add(s);
            }
        }
        return new SongAdapter(context, R.layout.list_item_song, result);
    }

    public static Song getNowPlaying() {
        //if(nowPlaying == -1) { return null; }
        return songNowPlaying;
    }

    public static int getSongIndex(Song s) {
        for(int i = 0; i < activeList.size(); i++) {
            if( s.equals(activeList.get(i))) { return i; }
        }
        return -1;
    }

    public static Bitmap getAlbumArt(ContentResolver cr, long albumId) {
        InputStream in = null;
        Uri sArtworkUri = Uri.parse("content://media/external/audio/albumart");
        Uri uri = ContentUris.withAppendedId(sArtworkUri, albumId);
        ContentResolver res = cr;
        try {
            in = res.openInputStream(uri);
        }
        catch(FileNotFoundException e) {
            return null;
        }
        return BitmapFactory.decodeStream(in);
    }

    public static Bitmap getRoundedCornerBitmap(Bitmap bitmap, int pixels) {
        if(bitmap != null) {
            Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap
                    .getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(output);

            final int color = 0xff424242;
            final Paint paint = new Paint();
            final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
            final RectF rectF = new RectF(rect);
            final float roundPx = pixels;

            paint.setAntiAlias(true);
            canvas.drawARGB(0, 0, 0, 0);
            paint.setColor(color);
            canvas.drawRoundRect(rectF, roundPx, roundPx, paint);

            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
            canvas.drawBitmap(bitmap, rect, rect, paint);

            return output;
        }
        return null;
    }

}
