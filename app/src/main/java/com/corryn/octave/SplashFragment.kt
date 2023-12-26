package com.corryn.octave

import android.Manifest
import android.content.pm.PackageManager
import android.database.Cursor
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.navigation.fragment.findNavController
import com.corryn.octave.databinding.FragmentSplashBinding
import com.corryn.octave.model.Song
import com.corryn.octave.ui.base.BaseFragment

// TODO Splash screen instead?
class SplashFragment : BaseFragment<FragmentSplashBinding>() {

    override val viewBindingInflater: (LayoutInflater, ViewGroup?, Boolean) -> FragmentSplashBinding
        get() = FragmentSplashBinding::inflate

    private var handler: Handler? = null
    private var delayedStart: Runnable = Runnable(::onClickOctave)

    private val player = Player

    private val storagePermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission(), ::onStoragePermissionRequestResult)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (player.exists()) {
            onClickOctave()
            return
        }

        binding.logo.setOnClickListener { onClickOctave() }

        val requestedPermission = askForExternalStoragePermission()

        if (requestedPermission.not()) {
            startupInit()

            handler = Handler(Looper.getMainLooper())
            delayedStart = Runnable {
                onClickOctave()
            }
            handler?.postDelayed(delayedStart, autoProgressDelay)
        }
    }

    private fun startupInit() {
        player.preparePlayer()
        player.setActive()
        createSongList()
    }

    override fun onStop() {
        super.onStop()

        handler?.removeCallbacks(delayedStart)
    }

    // Returns a boolean indicating whether permission had to be requested.
    private fun askForExternalStoragePermission(): Boolean {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (activity?.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
            storagePermissionLauncher.launch(permission)
            return true
        }

        return false
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

            activity?.contentResolver?.query(
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
            // TODO Dialog with warning? Means to retry?
            Toast.makeText(requireContext(), "Media files were not available for access. Application may behave incorrectly.", Toast.LENGTH_LONG).show()
        }
    }

    // TODO Behavior for when permission is denied
    private fun onStoragePermissionRequestResult(isGranted: Boolean) {
        startupInit()
        onClickOctave()
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
        findNavController().navigate(SplashFragmentDirections.actionTitleFragmentToPlayerFragment())
    }

    companion object {
        private const val autoProgressDelay: Long = 1500L
    }

}