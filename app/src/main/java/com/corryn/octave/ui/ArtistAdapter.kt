package com.corryn.octave.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.corryn.octave.databinding.ListItemArtistBinding

class ArtistAdapter(private val onArtistClicked: (String) -> Unit): ListAdapter<String, ArtistAdapter.ArtistViewHolder>(ArtistDiffer()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArtistViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ListItemArtistBinding.inflate(inflater, parent, false)

        return ArtistViewHolder(binding, onArtistClicked)
    }

    override fun onBindViewHolder(holder: ArtistViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    inner class ArtistViewHolder(private val binding: ListItemArtistBinding, private val onArtistClicked: (String) -> Unit): ViewHolder(binding.root) {

        fun bind(artist: String) = with(binding) {
            root.isActivated = adapterPosition % 2 == 1

            artistName.text = artist

            root.setOnClickListener {
                onArtistClicked(artist)
            }
        }

    }

    private class ArtistDiffer: DiffUtil.ItemCallback<String>() {

        override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: String, newItem: String): Boolean {
            return oldItem == newItem
        }

    }

}
