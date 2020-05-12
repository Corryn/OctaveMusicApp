package hci.com.octave;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class SongAdapter extends ArrayAdapter<Song> {

    private Player player = Player.INSTANCE;
    private List<Song> list;

    public SongAdapter(Context context, int resource,
                       List<Song> items) {
        super(context, resource, items);
        list = items;
    }

    @NotNull
    @Override
    public View getView(final int position, View convertView, @NotNull final ViewGroup parent) {

        View v = convertView;
        final Song song = getItem(position);

        if (v == null) {

            LayoutInflater vi;
            vi = LayoutInflater.from(getContext());
            v = vi.inflate(R.layout.list_item_song, null);

        }

        if (song != null) { // Fill text fields
            TextView songTitle = v.findViewById(R.id.songTitle);
            TextView songArtist = v.findViewById(R.id.songArtist);
            ImageView playButton = v.findViewById(R.id.menuPlay);
            ImageView addButton = v.findViewById(R.id.menuAdd);

            if (songTitle != null) {
                songTitle.setText(song.getTitle());
            }
            if (songArtist != null) {
                songArtist.setText(song.getArtist());
            }
            if (playButton != null) {
                playButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (player.isSearching()) {
                            player.setActiveList(player.getViewedList());
                            player.setSong(player.getSongIndex(song));
                        } else if (!player.playlistIsEmpty()) {
                            List<Song> temp = player.getActiveList();
                            player.setActiveList(list);
                            player.setSong(player.getSongIndex(song));
                            player.setActiveList(temp);
                        } else {
                            player.setActiveList(list);
                            player.setSong(player.getSongIndex(song));
                        }
                        ListView lv = (ListView) parent;
                        player.setPlayClicked(true);
                        lv.performItemClick(
                                lv.getAdapter().getView(position, null, null),
                                position,
                                lv.getAdapter().getItemId(position));
                    }
                });
            }
            if (addButton != null) {
                addButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (player.isSearching()) {
                            player.setActiveList(player.getViewedList());
                        } else {
                            player.setActiveList(list);
                        }
                        player.addToPlaylist(song);
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
