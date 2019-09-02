package hci.com.octave;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by botto_000 on 3/14/2016.
 */
public class SongAdapter extends ArrayAdapter<Song> {

    List<Song> list;

    public SongAdapter(Context context, int resource,
                       List<Song> items) {
        super(context, resource, items);
        list = items;
    }

    @Override
    public View getView(final int position, View convertView, final ViewGroup parent) {

        View v = convertView;
        final Song song = getItem(position);
        final int pos = position;

        if (v == null) {

            LayoutInflater vi;
            vi = LayoutInflater.from(getContext());
            v = vi.inflate(R.layout.list_item_song, null);

        }

        if (song != null) { // Fill text fields
            TextView songTitle = (TextView) v.findViewById(R.id.songTitle);
            TextView songArtist = (TextView) v.findViewById(R.id.songArtist);
            ImageView playButton = (ImageView) v.findViewById(R.id.menuPlay);
            ImageView addButton = (ImageView) v.findViewById(R.id.menuAdd);

            if (songTitle != null) {
                songTitle.setText(song.getTitle());
            }
            if (songArtist != null) {
                songArtist.setText(song.getArtist());
            }
            if(playButton != null) {
                playButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(Player.isSearching()) {
                            Player.setActiveList(Player.getViewedList());
                            Player.setSong(Player.getSongIndex(song));
                        }
                        else if(!Player.playlistIsEmpty()){
                            List<Song> temp = Player.getActiveList();
                            Player.setActiveList(list);
                            Player.setSong(Player.getSongIndex(song));
                            Player.setActiveList(temp);
                        }
                        else {
                            Player.setActiveList(list);
                            Player.setSong(Player.getSongIndex(song));
                        }
                        ListView lv = (ListView) parent;
                        Player.setPlayClicked(true);
                        lv.performItemClick(
                                lv.getAdapter().getView(position, null, null),
                                position,
                                lv.getAdapter().getItemId(position));
                    }
                });
            }
            if(addButton != null) {
                addButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(Player.isSearching()) {
                            Player.setActiveList(Player.getViewedList());
                        }
                        else {
                            Player.setActiveList(list);
                        }
                        Player.addToPlaylist(song);
                        Toast.makeText(getContext(), song.getTitle() + " added to queue!", Toast.LENGTH_SHORT).show();
                        ListView lv = (ListView) parent;
                        lv.performItemClick(
                                lv.getAdapter().getView(position, null, null),
                                position,
                                lv.getAdapter().getItemId(position));
                    }
                });
            }
            if (position % 2 == 1) {
                v.setBackgroundResource(R.drawable.whiteborder);
            } else {
                v.setBackgroundResource(R.drawable.tealborder);
            }
        }

        return v;

    }

}
