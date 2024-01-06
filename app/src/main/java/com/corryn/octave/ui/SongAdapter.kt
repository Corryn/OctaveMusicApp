package com.corryn.octave.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.corryn.octave.databinding.ListItemSongBinding
import com.corryn.octave.model.Song

class SongAdapter(
    private val onSongClicked: (Song, Int) -> Unit,
    private val onPlayClicked: (Song, List<Song>) -> Unit,
    private val onAddClicked: (Song, List<Song>) -> Unit
): ListAdapter<Song, SongAdapter.SongViewHolder>(SongDiffer()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ListItemSongBinding.inflate(inflater, parent, false)

        return SongViewHolder(binding, onSongClicked, onPlayClicked, onAddClicked)
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item, currentList)
    }

    inner class SongViewHolder(
        private val binding: ListItemSongBinding,
        private val onSongClicked: (Song, Int) -> Unit,
        private val onPlayClicked: (Song, List<Song>) -> Unit,
        private val onAddClicked: (Song, List<Song>) -> Unit
    ): RecyclerView.ViewHolder(binding.root) {

        fun bind(song: Song, activeList: List<Song>) = with(binding) {
            root.isActivated = adapterPosition % 2 == 1

            songTitle.text = song.title
            songArtist.text = song.artist

            root.setOnClickListener {
                val animation: Animation = AlphaAnimation(0.3f, 1.0f)
                animation.duration = 500
                it.startAnimation(animation)

                onSongClicked(song, adapterPosition)
            }

            menuPlay.setOnClickListener {
                onPlayClicked(song, activeList)
                root.performClick() // To trigger album art update
            }

            menuAdd.setOnClickListener {
                onAddClicked(song, activeList)
                root.performClick() // To trigger album art update
            }
        }

    }

    private class SongDiffer: DiffUtil.ItemCallback<Song>() {

        override fun areItemsTheSame(oldItem: Song, newItem: Song): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Song, newItem: Song): Boolean {
            return oldItem.title == newItem.title &&
                    oldItem.artist == newItem.artist &&
                    oldItem.albumId == newItem.albumId
        }

    }

}