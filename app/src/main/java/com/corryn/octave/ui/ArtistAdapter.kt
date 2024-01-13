package com.corryn.octave.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.corryn.octave.databinding.ListItemArtistBinding
import com.corryn.octave.model.Artist

class ArtistAdapter(private val onArtistClicked: (Long) -> Unit): ListAdapter<Artist, ArtistAdapter.ArtistViewHolder>(ArtistDiffer()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArtistViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ListItemArtistBinding.inflate(inflater, parent, false)

        return ArtistViewHolder(binding, onArtistClicked)
    }

    override fun onBindViewHolder(holder: ArtistViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    inner class ArtistViewHolder(private val binding: ListItemArtistBinding, private val onArtistClicked: (Long) -> Unit): ViewHolder(binding.root) {

        fun bind(artist: Artist) = with(binding) {
            root.isActivated = adapterPosition % 2 == 1

            artistName.text = artist.name

            root.setOnClickListener {
                onArtistClicked(artist.id)
            }
        }

    }

    private class ArtistDiffer: DiffUtil.ItemCallback<Artist>() {

        override fun areItemsTheSame(oldItem: Artist, newItem: Artist): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Artist, newItem: Artist): Boolean {
            return oldItem.name == newItem.name
        }

    }

}
