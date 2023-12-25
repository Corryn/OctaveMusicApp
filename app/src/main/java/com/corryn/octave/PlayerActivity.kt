package com.corryn.octave

import android.content.Context
import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.TextView.OnEditorActionListener
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.abs

class PlayerActivity : AppCompatActivity(), OnEditorActionListener {

    private var x1 = 0f
    private var x2 = 0f
    private var y1 = 0f
    private var y2 = 0f

    val player = Player

    var menuOpen = false
    var viewingSongs = false

    var nowPlaying: TextView? = null
    var upNext: TextView? = null
    var artistLabel: TextView? = null
    var searchBar: TextView? = null
    var clearSearch: TextView? = null
    var pause: ImageView? = null
    var previous: ImageView? = null
    var next: ImageView? = null
    var repeat: ImageView? = null
    var shuffle: ImageView? = null
    var downArrow: ImageView? = null
    var mainArt: ImageView? = null
    var menu: RelativeLayout? = null

    var playerMenuList: ListView? = null
    var songAdapter: SongAdapter? = null
    var artistAdapter: ArtistAdapter? = null
    var playerMenuArt: ImageView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        menu = findViewById<View>(R.id.playerMenu) as RelativeLayout
        playerMenuList = findViewById<View>(R.id.playerMenuList) as ListView
        playerMenuArt = findViewById<View>(R.id.playerMenuArt) as ImageView
        artistLabel = findViewById(R.id.artistLabel)
        artistAdapter = ArtistAdapter(this, R.layout.list_item_artist, player.artistList)

        playerMenuList?.adapter = artistAdapter
        playerMenuList?.itemsCanFocus = false

        player.resetItemColor()
        setSongClickListener(false)

        nowPlaying = findViewById(R.id.playerNowPlaying)
        upNext = findViewById(R.id.playlistUpNext)

        setUpMenuButton()
        setUpSearchBar()
        setUpClearButton()
        setUpPauseButton()
        setUpNextButton()
        setUpPreviousButton()
        setUpRepeatButton()
        setUpShuffleButton()

        mainArt = findViewById(R.id.mainart)
        mainArt?.setOnTouchListener { _, _ -> false }

        setSongListLayout(this.resources.configuration.orientation)
        resume()
    }

    private fun setUpSearchBar() {
        searchBar = findViewById(R.id.searchBar)

        searchBar?.setOnEditorActionListener(this)
        searchBar?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                val searchString = s.toString().trim { it <= ' ' }
                playerMenuList?.adapter = player.filterSongs(searchString)
                player.isSearching = searchString != ""
            }

            override fun afterTextChanged(s: Editable) {}
        })
    }

    private fun setUpClearButton() {
        clearSearch = findViewById(R.id.clearSearch)
        clearSearch?.setOnClickListener { v ->
            val animation: Animation = AlphaAnimation(0.3f, 1.0f)
            animation.duration = 500
            v.startAnimation(animation)
            if (searchBar?.text.toString() != "") {
                player.isSearching = false
                searchBar?.text = ""
            }
        }
    }

    private fun setUpMenuButton() {
        downArrow = findViewById(R.id.downarrow)
        downArrow?.setOnClickListener { openMenu() }
    }

    private fun setUpPauseButton() {
        pause = findViewById(R.id.pause)
        pause?.setOnClickListener {
            if (player.getNowPlaying() != null) {
                pause()
            } else if (!player.playlistIsEmpty()) {
                player.nextSong()
                pause?.setImageResource(R.drawable.octavepause)
                setUpNext()
            } else {
                noSongPicked()
            }
        }
    }

    private fun setUpNextButton() {
        next = findViewById(R.id.next)
        next?.setOnClickListener {
            if (player.getNowPlaying() != null) {
                nextSongClick()
            } else {
                noSongPicked()
            }
        }
    }

    private fun setUpPreviousButton() {
        previous = findViewById(R.id.previous)
        previous?.setOnClickListener {
            if (player.getNowPlaying() != null) {
                prevSongClick()
            } else {
                noSongPicked()
            }
        }
    }

    private fun setUpRepeatButton() {
        repeat = findViewById(R.id.repeat)
        repeat?.setOnClickListener {
            val res = player.toggleRepeat()
            if (res) {
                Toast.makeText(applicationContext, "Repeat on", Toast.LENGTH_SHORT).show()
                repeat?.setImageResource(R.drawable.octaverepeatactive)
            } else {
                Toast.makeText(applicationContext, "Repeat off", Toast.LENGTH_SHORT).show()
                repeat?.setImageResource(R.drawable.octaverepeat)
            }
        }
    }

    private fun setUpShuffleButton() {
        shuffle = findViewById(R.id.shuffle)
        shuffle?.setOnClickListener {
            val res = player.toggleShuffle()
            if (res) {
                Toast.makeText(applicationContext, "Shuffle on", Toast.LENGTH_SHORT).show()
                shuffle?.setImageResource(R.drawable.octaveshuffleactive)
            } else {
                Toast.makeText(applicationContext, "Shuffle off", Toast.LENGTH_SHORT).show()
                shuffle?.setImageResource(R.drawable.octaveshuffle)
            }
        }
    }

    private fun noSongPicked() {
        Toast.makeText(applicationContext, "Swipe down and pick a song first!", Toast.LENGTH_SHORT).show()
    }

    public override fun onResume() {
        super.onResume()

        resume()
    }

    private fun resume() {
        player.updateContext(applicationContext)
        updateCompletionListener()

        if (player.getNowPlaying() != null) {
            setNowPlaying()
        }

        if (player.selected != -1) {
            playerMenuList?.setSelection(player.selected)
        }

        updateAlbumArt(player.selectedSong)
    }

    override fun onEditorAction(v: TextView, actionId: Int, event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_ENTER) {
            val `in` = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

            // NOTE: In the author's example, he uses an identifier
            // called searchBar. If setting this code on your EditText
            // then use v.getWindowToken() as a reference to your
            // EditText is passed into this callback as a TextView
            `in`.hideSoftInputFromWindow(v
                    .applicationWindowToken,
                    InputMethodManager.HIDE_NOT_ALWAYS)
            // Must return true here to consume event
            return true
        }
        if (actionId == EditorInfo.IME_ACTION_NEXT || event.keyCode == KeyEvent.KEYCODE_ENTER) {
            return false
        } else {
            val searchString = v.text.toString().trim { it <= ' ' }
            playerMenuList?.adapter = player.filterSongs(searchString)
        }
        return true
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        // Checks the orientation of the screen
        setSongListLayout(newConfig.orientation)
    }

    private fun setSongListLayout(config: Int) {
        if (config == Configuration.ORIENTATION_LANDSCAPE) {
            playerMenuList?.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 0.7f)

            val params = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.3f)
            val resources = applicationContext.resources
            val px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5f, resources.displayMetrics).toInt()

            params.setMargins(px, px, px, px)
            playerMenuArt?.layoutParams = params
            mainArt?.setImageResource(R.drawable.octavesplashlandscape)
        } else if (config == Configuration.ORIENTATION_PORTRAIT) {
            playerMenuList?.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
            playerMenuArt?.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0f)
            mainArt?.setImageResource(R.drawable.octavesplashportrait)
        }
    }

    override fun onBackPressed() {
        if (menuOpen) {
            if (viewingSongs) {
                returnToArtistList()
            } else {
                closeMenu()
            }
        } else {
            super.onBackPressed()
        }
    }

    private fun returnToArtistList() {
        searchBar?.text = ""
        player.isSearching = false

        searchBar?.visibility = View.INVISIBLE
        clearSearch?.visibility = View.INVISIBLE
        artistLabel?.visibility = View.VISIBLE

        player.viewedList = null

        val animation: Animation = AlphaAnimation(0.3f, 1.0f)
        animation.duration = 500
        playerMenuList?.startAnimation(animation)

        playerMenuList?.adapter = artistAdapter
        setSongClickListener(false)
        updateAlbumArt(null)
        viewingSongs = false
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                x1 = event.x
                y1 = event.y
            }
            MotionEvent.ACTION_UP -> {
                x2 = event.x
                y2 = event.y
                val deltaX = x2 - x1
                val deltaY = y2 - y1
                if (abs(deltaY) > MIN_DISTANCE && y1 < y2) {
                    openMenu()
                } else if (abs(deltaY) > MIN_DISTANCE && y1 > y2) {
                    closeMenu()
                } else if (abs(deltaX) > MIN_DISTANCE && x1 < x2) {
                    prevSongClick()
                } else if (abs(deltaX) > MIN_DISTANCE && x1 > x2) {
                    nextSongClick()
                } else {
                    if (!menuOpen) {
                        if (!player.playlistIsEmpty()) {
                            nextSongClick()
                        } else if (player.getNowPlaying() != null) {
                            pause()
                        }
                    }
                }
            }
        }
        return super.onTouchEvent(event)
    }

    private fun pause() {
        if (player.isPaused) {
            player.unpauseSong()
            pause?.setImageResource(R.drawable.octavepause)
        } else {
            player.pauseSong()
            pause?.setImageResource(R.drawable.octaveplay)
        }
    }

    private fun openMenu() {
        if (!menuOpen) {
            menuOpen = true
            menu?.visibility = View.VISIBLE

            val animationSlideIn = AnimationUtils.loadAnimation(this, R.anim.slideinmenu)
            menu?.startAnimation(animationSlideIn)
        }
    }

    private fun closeMenu() {
        if (menuOpen) {
            menuOpen = false

            setNowPlaying()
            setUpNext()

            val animationSlideOut = AnimationUtils.loadAnimation(this, R.anim.slideoutmenu)
            menu?.startAnimation(animationSlideOut)
            menu?.visibility = View.GONE
        }
    }

    private fun updateCompletionListener() {
        player.player.setOnCompletionListener {
            player.nextSong()
            setNowPlaying()
            setUpNext()
        }
    }

    private fun nextSongClick() {
        if (player.getNowPlaying() != null) {
            player.nextSong()
            setNowPlaying()
            setUpNext()
            pause?.setImageResource(R.drawable.octavepause)
        }
    }

    private fun prevSongClick() {
        if (player.getNowPlaying() != null) {
            if (player.playlistIsEmpty()) {
                player.prevSong()
                setNowPlaying()
                pause?.setImageResource(R.drawable.octavepause)
            } else {
                Toast.makeText(this, "Disabled during queue playback.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setNowPlaying() {
        val song = player.getNowPlaying()
        if (song != null) {
            nowPlaying?.text = getString(R.string.now_playing, song.title, song.artist)
        }
    }

    private fun setUpNext() {
        val song = player.playlistNext()
        if (song != null) {
            upNext?.visibility = View.VISIBLE
            upNext?.text = getString(R.string.up_next, song.title, song.artist)
        } else {
            upNext?.visibility = View.GONE
        }
    }

    fun setSongClickListener(set: Boolean) {
        if (set) {
            playerMenuList?.onItemClickListener = OnItemClickListener { parent, view, position, _ ->
                val animation: Animation = AlphaAnimation(0.3f, 1.0f)
                animation.duration = 500
                view.startAnimation(animation)

                if (player.playClicked) {
                    pause?.setImageResource(R.drawable.octavepause)
                    player.playClicked = false
                }

                player.selected = position
                updateAlbumArt(parent.getItemAtPosition(position) as Song?)
            }
        } else {
            playerMenuList?.onItemClickListener = object : OnItemClickListener {
                override fun onItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                    val animation: Animation = AlphaAnimation(0.3f, 1.0f)
                    animation.duration = 500
                    playerMenuList?.startAnimation(animation)

                    val selectedArtist: String = parent.getItemAtPosition(position) as String
                    val artistSongs: List<Song> = player.byArtistList[selectedArtist] ?: return

                    songAdapter = SongAdapter(applicationContext, R.layout.list_item_song, artistSongs)
                    playerMenuList?.adapter = songAdapter

                    setSongClickListener(true)
                    player.viewedList = artistSongs

                    artistLabel?.visibility = View.INVISIBLE
                    searchBar?.visibility = View.VISIBLE
                    clearSearch?.visibility = View.VISIBLE
                    viewingSongs = true
                }
            }
        }
    }

    private fun updateAlbumArt(song: Song?) {
        if (song != null) {
            val albumArt = player.getAlbumArt(contentResolver, song.albumID)
            if (albumArt != null) {
                playerMenuArt?.setImageBitmap(player.getRoundedCornerBitmap(player.getAlbumArt(contentResolver, song.albumID), 50))
            } else {
                val logo = BitmapFactory.decodeResource(applicationContext.resources,
                        R.drawable.octave)
                playerMenuArt?.setImageBitmap(player.getRoundedCornerBitmap(logo, 50))
            }
        } else {
            val logo = BitmapFactory.decodeResource(applicationContext.resources,
                    R.drawable.octave)
            playerMenuArt?.setImageBitmap(player.getRoundedCornerBitmap(logo, 50))
        }
    }

    companion object {
        const val MIN_DISTANCE = 75
    }
}