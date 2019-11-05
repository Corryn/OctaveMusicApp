package hci.com.octave;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public class TitleActivity extends AppCompatActivity {

    Handler handler;
    Runnable myRunnable;
    int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(Player.exists()) {
            onClickOctave();
        }
        else {
            setContentView(R.layout.activity_title);

            MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 4242;

            ImageView logo = findViewById(R.id.logo);
            logo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onClickOctave();
                }
            });

            askForExternalStoragePermission();

            Player.preparePlayer();
            Player.setActive();
            createSongList();
            handler = new Handler();
            myRunnable = new Runnable() {
                @Override
                public void run() {
                    // Do something after 5s = 5000ms
                    onClickOctave();
                }
            };
            handler.postDelayed(myRunnable, 1500);
        }
    }

    protected void onStop() {
        super.onStop();
        handler.removeCallbacks(myRunnable);
    }

    private void askForExternalStoragePermission() {
        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (shouldShowRequestPermissionRationale(
                    Manifest.permission.READ_EXTERNAL_STORAGE)) {
                // Explain to the user why we need to read the contacts
            }

            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);

            // MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE is an
            // app-defined int constant that should be quite unique

            return;
        }
    }

    private void createSongList() {
        if(isExternalStorageReadable()) {
            ContentResolver musicResolver = getContentResolver();
            Uri musicUri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
            Cursor musicCursor = musicResolver.query(musicUri, null, null, null, null);
            Song temp;
            List<Song> all = Player.getSongList();
            List<String> artists = Player.getArtistList();
            HashMap<String, ArrayList<Song>> byA = Player.getByArtistList();

            if(musicCursor!=null && musicCursor.moveToFirst()){
                //get columns
                int titleColumn = musicCursor.getColumnIndex
                        (android.provider.MediaStore.Audio.Media.TITLE);
                int idColumn = musicCursor.getColumnIndex
                        (android.provider.MediaStore.Audio.Media._ID);
                int artistColumn = musicCursor.getColumnIndex
                        (android.provider.MediaStore.Audio.Media.ARTIST);
                int albumIdColumn = musicCursor.getColumnIndex
                        (MediaStore.Audio.Media.ALBUM_ID);
                int dataColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.DATA);
                //add songs to list
                do {
                    long thisId = musicCursor.getLong(idColumn);
                    long thisAlbumId = musicCursor.getLong(albumIdColumn);
                    String thisTitle = musicCursor.getString(titleColumn);
                    String thisArtist = musicCursor.getString(artistColumn);
                    String thisData= musicCursor.getString(dataColumn);
                    temp = new Song(thisId, thisAlbumId, thisTitle, thisArtist, thisData);
                    all.add(temp); // Add to main song list
                    if(!byA.containsKey(thisArtist)) { // Compile hashmap of songs sorted by artist
                        byA.put(thisArtist, new ArrayList<Song>());
                        artists.add(thisArtist);
                    }
                    byA.get(thisArtist).add(temp);
                }
                while (musicCursor.moveToNext());
                Collections.sort(artists, new Comparator<String>()
                {
                    @Override
                    public int compare(String text1, String text2)
                    {
                        return text1.compareToIgnoreCase(text2);
                    }
                });
            }
        }
        else {
            Toast.makeText(getApplicationContext(),
                    "Media files were not available for access.  " +
                            "Application may behave incorrectly.", Toast.LENGTH_LONG).show();
        }
    }

    private boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        return false;
    }

    private void onClickOctave() {
        Intent i = new Intent(this, PlayerActivity.class);
        startActivity(i);
        overridePendingTransition(R.anim.slideinfrombottom, R.anim.slideouttotop);
        finish();
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_title, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
