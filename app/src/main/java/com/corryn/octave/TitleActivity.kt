package com.corryn.octave

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.provider.MediaStore
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import java.util.*

class TitleActivity : AppCompatActivity() {

    private lateinit var handler: Handler
    private lateinit var delayedStart: Runnable
    private var MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 4242 // Unique app-defined constant

    private val player = Player

    private val storagePermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission(), ::onStoragePermissionRequestResult)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (player.exists()) {
            onClickOctave()
            return
        }

        setContentView(R.layout.activity_title)

        val logo = findViewById<ImageView>(R.id.logo)
        logo.setOnClickListener { onClickOctave() }

        askForExternalStoragePermission()

        player.preparePlayer()
        player.setActive()
        createSongList()

        handler = Handler()
        delayedStart = Runnable {
            onClickOctave()
        }
        handler.postDelayed(delayedStart, 1500)
    }

    override fun onStop() {
        super.onStop()

        handler.removeCallbacks(delayedStart)
    }

    private fun askForExternalStoragePermission() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
            storagePermissionLauncher.launch(permission)
        }
    }

    private fun createSongList() {
        if (isExternalStorageReadable) {
            val musicProjection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Albums.ARTIST,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.DATA
            )

            val musicSelection = MediaStore.Audio.Media.IS_MUSIC + " != 0"

            val allSongs = player.songList
            val artists = player.artistList
            val songsByArtist = player.byArtistList

            contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                musicProjection,
                musicSelection,
                null,
                null
            )?.use { musicCursor ->
                while (musicCursor.moveToNext()) {
                    processSong(musicCursor, allSongs, artists, songsByArtist)
                }
            }

            artists.sortWith { text1: String, text2: String -> text1.compareTo(text2, ignoreCase = true) }
        } else {
            Toast.makeText(
                applicationContext,
                "Media files were not available for access.  " +
                        "Application may behave incorrectly.", Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun onStoragePermissionRequestResult(isGranted: Boolean) {

    }

    private fun processSong(
        musicCursor: Cursor,
        allSongs: MutableList<Song>,
        artists: MutableList<String>,
        songsByArtist: HashMap<String, MutableList<Song>>
    ) {
        val thisId = musicCursor.getLong(0)
        val thisAlbumId = musicCursor.getLong(1)
        val thisArtist = musicCursor.getString(2)
        val thisTitle = musicCursor.getString(3)
        val thisData = musicCursor.getString(4)

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