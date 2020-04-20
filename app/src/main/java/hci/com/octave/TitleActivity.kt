package hci.com.octave

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.provider.MediaStore
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.util.*

class TitleActivity : AppCompatActivity() {

    private var handler: Handler? = null
    private var delayedStart: Runnable? = null
    private var MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 4242 // Unique app-defined constant

    private var idColumn = 0
    private var titleColumn = 0
    private var artistColumn = 0
    private var albumIdColumn = 0
    private var dataColumn = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Player.exists()) {
            onClickOctave()
            return
        }

        setContentView(R.layout.activity_title)

        val logo = findViewById<ImageView>(R.id.logo)
        logo.setOnClickListener { onClickOctave() }

        askForExternalStoragePermission()

        Player.preparePlayer()
        Player.setActive()
        createSongList()

        handler = Handler()
        delayedStart = Runnable {
            onClickOctave()
        }
        handler?.postDelayed(delayedStart, 1500)
    }

    override fun onStop() {
        super.onStop()

        handler?.removeCallbacks(delayedStart)
    }

    private fun askForExternalStoragePermission() {
        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE)
        }
    }

    private fun createSongList() {
        if (isExternalStorageReadable) {
            val musicResolver = contentResolver
            val musicUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            val musicCursor = musicResolver.query(musicUri, null, null, null, null)

            val allSongs = Player.getSongList()
            val artists = Player.getArtistList()
            val songsByArtist = Player.getByArtistList()

            if (musicCursor != null && musicCursor.moveToFirst()) {
                // Get song data indices
                idColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media._ID)
                titleColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.TITLE)
                artistColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)
                albumIdColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)
                dataColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.DATA)

                // Add songs to list
                do {
                    processSong(musicCursor, allSongs, artists, songsByArtist)
                } while (musicCursor.moveToNext())

                artists.sortWith(Comparator { text1, text2 -> text1.compareTo(text2, ignoreCase = true) })

                musicCursor.close()
            }
        } else {
            Toast.makeText(applicationContext,
                    "Media files were not available for access.  " +
                            "Application may behave incorrectly.", Toast.LENGTH_LONG).show()
        }
    }

    private fun processSong(musicCursor: Cursor, allSongs: MutableList<Song>, artists: MutableList<String>, songsByArtist: HashMap<String, ArrayList<Song>>) {
        val thisId = musicCursor.getLong(idColumn)
        val thisAlbumId = musicCursor.getLong(albumIdColumn)
        val thisTitle = musicCursor.getString(titleColumn)
        val thisArtist = musicCursor.getString(artistColumn)
        val thisData = musicCursor.getString(dataColumn)

        val song = Song(thisId, thisAlbumId, thisTitle, thisArtist, thisData)
        allSongs.add(song) // Add to main song list

        if (!songsByArtist.containsKey(thisArtist)) { // Compile hashmap of songs sorted by artist
            songsByArtist[thisArtist] = ArrayList()
            artists.add(thisArtist)
        }

        songsByArtist[thisArtist]?.add(song)
    }

    private val isExternalStorageReadable: Boolean
        get() {
            val state = Environment.getExternalStorageState()
            return Environment.MEDIA_MOUNTED == state || Environment.MEDIA_MOUNTED_READ_ONLY == state
        }

    private fun onClickOctave() {
        val intent = Intent(this, PlayerActivity::class.java)
        startActivity(intent)
        overridePendingTransition(R.anim.slideinfrombottom, R.anim.slideouttotop)
        finish()
    }

}