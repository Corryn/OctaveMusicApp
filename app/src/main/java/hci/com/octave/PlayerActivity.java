package hci.com.octave;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public class PlayerActivity extends AppCompatActivity implements TextView.OnEditorActionListener {

    private float x1,x2,y1,y2;
    static final int MIN_DISTANCE = 75;
    boolean menuOpen = false;
    boolean viewingSongs = false;
    TextView nowPlaying;
    TextView upNext;
    TextView artistLabel;
    TextView clearSearch;
    EditText search;
    ImageView pause;
    ImageView previous;
    ImageView next;
    ImageView repeat;
    ImageView shuffle;
    ImageView downArrow;
    ImageView mainArt;
    RelativeLayout menu;
    ListView playerMenuList;
    SongAdapter songAdapter;
    ArtistAdapter artistAdapter;
    ImageView playerMenuArt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        menu = (RelativeLayout) findViewById(R.id.playerMenu);
        playerMenuList = (ListView) findViewById(R.id.playerMenuList);
        playerMenuArt = (ImageView) findViewById(R.id.playerMenuArt);
        artistAdapter = new ArtistAdapter(this, R.layout.list_item_artist, Player.getArtistList());
        playerMenuList.setAdapter(artistAdapter);
        playerMenuList.setItemsCanFocus(false);
        Player.resetItemColor();

        setSongClickListener(false);

        nowPlaying = (TextView) findViewById(R.id.playerNowPlaying);
        upNext = (TextView) findViewById(R.id.playlistUpNext);

        pause = (ImageView) findViewById(R.id.pause);
        pause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(Player.getNowPlaying() != null) {
                    pause();
                }
                else if(!Player.playlistIsEmpty()) {
                    Player.nextSong();
                    pause.setImageResource(R.drawable.octavepause);
                    setUpNext();
                }
                else {
                    Toast.makeText(getApplicationContext(), "Swipe down and pick a song first!", Toast.LENGTH_LONG).show();
                }
            }
        });

        previous = (ImageView) findViewById(R.id.previous);
        previous.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(Player.getNowPlaying() != null) {
                    prevSongClick();
                }
                else {
                    Toast.makeText(getApplicationContext(), "Swipe down and pick a song first!", Toast.LENGTH_LONG).show();
                }
            }
        });

        repeat = (ImageView) findViewById(R.id.repeat);
        repeat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean res = Player.toggleRepeat();
                if(res) {
                    Toast.makeText(getApplicationContext(), "Repeat on", Toast.LENGTH_SHORT).show();
                    repeat.setImageResource(R.drawable.octaverepeatactive);
                }
                else {
                    Toast.makeText(getApplicationContext(), "Repeat off", Toast.LENGTH_SHORT).show();
                    repeat.setImageResource(R.drawable.octaverepeat);
                }
            }
        });

        shuffle = (ImageView) findViewById(R.id.shuffle);
        shuffle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean res = Player.toggleShuffle();
                if(res) {
                    Toast.makeText(getApplicationContext(), "Shuffle on", Toast.LENGTH_SHORT).show();
                    shuffle.setImageResource(R.drawable.octaveshuffleactive);
                }
                else {
                    Toast.makeText(getApplicationContext(), "Shuffle off", Toast.LENGTH_SHORT).show();
                    shuffle.setImageResource(R.drawable.octaveshuffle);
                }
            }
        });

        downArrow = (ImageView) findViewById(R.id.downarrow);
        downArrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openMenu();
            }
        });

        next = (ImageView) findViewById(R.id.next);
        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(Player.getNowPlaying() != null) {
                    nextSongClick();
                }
                else {
                    Toast.makeText(getApplicationContext(), "Swipe down and pick a song first!", Toast.LENGTH_LONG).show();
                }
            }
        });

        mainArt = (ImageView) findViewById(R.id.mainart);
        mainArt.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return false;
            }
        });

        artistLabel = (TextView) findViewById(R.id.artistLabel);

        search = (EditText) findViewById(R.id.searchBar);

        final EditText searchBar = (EditText) findViewById(R.id.searchBar);
        searchBar.setOnEditorActionListener(this);
        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String searchString = s.toString().trim();
                playerMenuList.setAdapter(Player.filterSongs(searchString));
                if(searchString.equals("")) {
                    Player.setSearching(false);
                }
                else {
                    Player.setSearching(true);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        clearSearch = (TextView) findViewById(R.id.clearSearch);
        clearSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Animation animation1 = new AlphaAnimation(0.3f, 1.0f);
                animation1.setDuration(500);
                v.startAnimation(animation1);
                if(!searchBar.getText().toString().equals("")) {
                    Player.setSearching(false);
                    searchBar.setText("");
                }
            }
        });

        setSongListLayout(this.getResources().getConfiguration().orientation);

        resume();
    }

    @Override
    public void onResume() {
        super.onResume();

        resume();
    }

    public void resume() {
        Player.updateContext(getApplicationContext());
        updateCompletionListener();
        if(Player.getNowPlaying() != null) {
            setNowPlaying();
        }
        if(Player.getSelected() != -1) {
            playerMenuList.setSelection(Player.getSelected());
        }
        updateAlbumArt(Player.getSelectedSong());
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (event != null&& (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
            InputMethodManager in = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

            // NOTE: In the author's example, he uses an identifier
            // called searchBar. If setting this code on your EditText
            // then use v.getWindowToken() as a reference to your
            // EditText is passed into this callback as a TextView

            in.hideSoftInputFromWindow(v
                            .getApplicationWindowToken(),
                    InputMethodManager.HIDE_NOT_ALWAYS);
            // Must return true here to consume event
            return true;

        }
        if (actionId == EditorInfo.IME_ACTION_NEXT || (event != null ? event.getKeyCode() : 66) == KeyEvent.KEYCODE_ENTER) {
            return false;
        } else {
            String searchString = v.getText().toString().trim();
            playerMenuList.setAdapter(Player.filterSongs(searchString));
        }

        return true;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // Checks the orientation of the screen
        setSongListLayout(newConfig.orientation);
    }

    public void setSongListLayout(int config) {
        if (config == Configuration.ORIENTATION_LANDSCAPE) {
            playerMenuList.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 0.7f));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.3f);
            Resources r = getApplicationContext().getResources();
            int px = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5, r.getDisplayMetrics());
            params.setMargins(px,px,px,px);
            playerMenuArt.setLayoutParams(params);
            mainArt.setImageResource(R.drawable.octavesplashlandscape);

        } else if (config == Configuration.ORIENTATION_PORTRAIT){
            playerMenuList.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f));
            playerMenuArt.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0f));
            mainArt.setImageResource(R.drawable.octavesplashportrait);
        }
    }

    @Override
    public void onBackPressed() {
        if(menuOpen) {
            if(viewingSongs) {
                search.setText("");
                Player.setSearching(false);
                search.setVisibility(View.INVISIBLE);
                clearSearch.setVisibility(View.INVISIBLE);
                artistLabel.setVisibility(View.VISIBLE);
                Player.setViewedList(null);
                Animation animation1 = new AlphaAnimation(0.3f, 1.0f);
                animation1.setDuration(500);
                playerMenuList.startAnimation(animation1);
                playerMenuList.setAdapter(artistAdapter);
                setSongClickListener(false);
                updateAlbumArt(null);
                viewingSongs = false;
            }
            else {
                closeMenu();
            }
        }
        else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        switch(event.getAction())
        {
            case MotionEvent.ACTION_DOWN:
                x1 = event.getX();
                y1 = event.getY();
                break;
            case MotionEvent.ACTION_UP:
                x2 = event.getX();
                y2 = event.getY();
                float deltaX = x2 - x1;
                float deltaY = y2 - y1;
                if (Math.abs(deltaY) > MIN_DISTANCE && y1 < y2) {
                    openMenu();
                }
                else if (Math.abs(deltaY) > MIN_DISTANCE && y1 > y2) {
                    closeMenu();
                }
                else if (Math.abs(deltaX) > MIN_DISTANCE && x1 < x2)
                {
                    prevSongClick();
                }
                else if(Math.abs(deltaX) > MIN_DISTANCE && x1 > x2) {
                    nextSongClick();
                }
                else
                {
                    if(!menuOpen) {
                        if(!Player.playlistIsEmpty()) {
                            nextSongClick();
                        }
                        else if(Player.getNowPlaying() != null) {
                            pause();
                        }
                    }
                }
                break;
        }
        return super.onTouchEvent(event);
    }

    public void pause() {
        if (Player.isPaused()) {
            Player.unpauseSong();
            pause.setImageResource(R.drawable.octavepause);
        } else {
            Player.pauseSong();
            pause.setImageResource(R.drawable.octaveplay);
        }
    }

    public void openMenu() {
        if(!menuOpen) {
            menuOpen = true;
            menu.setVisibility(View.VISIBLE);
            Animation animationSlideIn = AnimationUtils.loadAnimation(this, R.anim.slideinmenu);
            menu.startAnimation(animationSlideIn);
        }
    }
    public void closeMenu() {
        if(menuOpen) {
            menuOpen = false;
            setNowPlaying();
            setUpNext();
            Animation animationSlideOut = AnimationUtils.loadAnimation(this, R.anim.slideoutmenu);
            menu.startAnimation(animationSlideOut);
            menu.setVisibility(View.GONE);
        }
    }

    public void updateCompletionListener() {
        Player.getPlayer().setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                Player.nextSong();
                setNowPlaying();
                setUpNext();
            }
        });
    }

    public void nextSongClick() {
        if(Player.getNowPlaying() != null) {
            Player.nextSong();
            setNowPlaying();
            setUpNext();
            pause.setImageResource(R.drawable.octavepause);
        }
    }

    public void prevSongClick() {
        if(Player.getNowPlaying() != null) {
            if(Player.playlistIsEmpty()) {
                Player.prevSong();
                setNowPlaying();
                pause.setImageResource(R.drawable.octavepause);
            }
            else {
                Toast.makeText(this, "Disabled during queue playback.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void setNowPlaying() {
        Song s = Player.getNowPlaying();
        if(s != null) {
            nowPlaying.setText("Now Playing: " + s.getTitle() + " - " + s.getArtist());
        }
    }

    public void setUpNext() {
        Song s = Player.playlistNext();
        if(s != null) {
            upNext.setVisibility(View.VISIBLE);
            upNext.setText("Up Next: " + s.getTitle() + " - " + s.getArtist());
        }
        else {
            upNext.setVisibility(View.GONE);
        }
    }

    public void setSongClickListener(boolean set) {
        if(set) {
            playerMenuList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    Animation animation1 = new AlphaAnimation(0.3f, 1.0f);
                    animation1.setDuration(500);
                    view.startAnimation(animation1);
                    if (Player.getPlayClicked()) {
                        pause.setImageResource(R.drawable.octavepause);
                        Player.setPlayClicked(false);
                    }
                    Player.setSelected(position);
                    updateAlbumArt((Song) parent.getItemAtPosition(position));
                }
            });
        }
        else {
            playerMenuList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    Animation animation1 = new AlphaAnimation(0.3f, 1.0f);
                    animation1.setDuration(500);
                    playerMenuList.startAnimation(animation1);
                    String selectedArtist = (String) parent.getItemAtPosition(position);
                    List<Song> artistSongs = Player.getByArtistList().get(selectedArtist);
                    songAdapter = new SongAdapter(getApplicationContext(), R.layout.list_item_song, artistSongs);
                    playerMenuList.setAdapter(songAdapter);
                    setSongClickListener(true);
                    Player.setViewedList(artistSongs);
                    artistLabel.setVisibility(View.INVISIBLE);
                    search.setVisibility(View.VISIBLE);
                    clearSearch.setVisibility(View.VISIBLE);
                    viewingSongs = true;
                }
            });
        }
    }

    public void updateAlbumArt(Song s) {
        if(s != null) {
            Bitmap albumArt = Player.getAlbumArt(getContentResolver(), s.getAlbumID());
            if(albumArt != null) {
                playerMenuArt.setImageBitmap(Player.getRoundedCornerBitmap
                        (Player.getAlbumArt(getContentResolver(), s.getAlbumID()), 50));
            }
            else {
                Bitmap logo = BitmapFactory.decodeResource(getApplicationContext().getResources(),
                        R.drawable.octave);
                playerMenuArt.setImageBitmap(Player.getRoundedCornerBitmap
                        (logo, 50));
            }
        }
        else {
            Bitmap logo = BitmapFactory.decodeResource(getApplicationContext().getResources(),
                    R.drawable.octave);
            playerMenuArt.setImageBitmap(Player.getRoundedCornerBitmap
                    (logo, 50));
        }
    }



}
