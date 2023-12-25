package com.corryn.octave;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

/**
 * Created by botto_000 on 3/14/2016.
 */
public class ArtistAdapter extends ArrayAdapter<String> {

    public ArtistAdapter(Context context, int resource,
                       List<String> items) {
        super(context, resource, items);
    }

    @Override
    public View getView(final int position, View convertView, final ViewGroup parent) {

        View v = convertView;
        final String artist = getItem(position);
        final int pos = position;

        if (v == null) {

            LayoutInflater vi;
            vi = LayoutInflater.from(getContext());
            v = vi.inflate(R.layout.list_item_artist, null);

        }

        if (artist != null) { // Fill text fields
            TextView artistName = (TextView) v.findViewById(R.id.artistName);

            if (artistName != null) {
                artistName.setText(artist);
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
