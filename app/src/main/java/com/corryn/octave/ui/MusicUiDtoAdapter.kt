package com.corryn.octave.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.corryn.octave.databinding.ListItemMusicBinding
import com.corryn.octave.model.consts.PlayerAction
import com.corryn.octave.model.dto.MusicUiDto

class MusicUiDtoAdapter(private val onItemClicked: (MusicUiDto, PlayerAction) -> Unit) :
    ListAdapter<MusicUiDto, RecyclerView.ViewHolder>(MusicUiDtoDiffer()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        return when (viewType) {
            0 -> {
                val binding = ListItemMusicBinding.inflate(inflater, parent, false)
                SongViewHolder(binding, onItemClicked)
            }

            1 -> {
                val binding = ListItemMusicBinding.inflate(inflater, parent, false)
                ArtistViewHolder(binding, onItemClicked)
            }

            else -> throw IllegalArgumentException("Unrecognized view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)

        when {
            holder is SongViewHolder && item is MusicUiDto.SongUiDto -> holder.bind(item)
            holder is ArtistViewHolder && item is MusicUiDto.ArtistUiDto -> holder.bind(item)
            else -> throw IllegalArgumentException("Incompatible viewholder and data type: ${holder::class.java} and ${item::class.java}")
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is MusicUiDto.SongUiDto -> 0
            is MusicUiDto.ArtistUiDto -> 1
        }
    }

    inner class SongViewHolder(
        private val binding: ListItemMusicBinding,
        private val onItemClicked: (MusicUiDto.SongUiDto, PlayerAction) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(song: MusicUiDto.SongUiDto) = with(binding) {
            title.text = song.songName
            subtitle.apply {
                isVisible = true
                text = song.artistName
            }

            playButton.apply {
                isVisible = true
                setOnClickListener {
                    onItemClicked(song, PlayerAction.PLAY)
                    root.performClick() // To trigger album art update
                }
            }

            addButton.apply {
                isVisible = true
                setOnClickListener {
                    onItemClicked(song, PlayerAction.ADD)
                    root.performClick() // To trigger album art update
                }
            }

            root.apply {
                isActivated = song.activated
                setOnClickListener {
                    val animation: Animation = AlphaAnimation(0.3f, 1.0f)
                    animation.duration = 500
                    it.startAnimation(animation)

                    onItemClicked(song, PlayerAction.TAP)
                }
            }
        }

    }

    inner class ArtistViewHolder(
        private val binding: ListItemMusicBinding,
        private val onItemClicked: (MusicUiDto.ArtistUiDto, PlayerAction) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(artist: MusicUiDto.ArtistUiDto) = with(binding) {
            title.text = artist.name

            root.apply {
                isActivated = artist.activated
                setOnClickListener {
                    onItemClicked(artist, PlayerAction.TAP)
                }
            }
        }

    }

}

private class MusicUiDtoDiffer : DiffUtil.ItemCallback<MusicUiDto>() {
    override fun areItemsTheSame(oldItem: MusicUiDto, newItem: MusicUiDto): Boolean {
        return when {
            oldItem is MusicUiDto.SongUiDto && newItem is MusicUiDto.SongUiDto -> oldItem.id == newItem.id
            oldItem is MusicUiDto.ArtistUiDto && newItem is MusicUiDto.ArtistUiDto -> oldItem.id == newItem.id
            else -> false
        }
    }

    override fun areContentsTheSame(oldItem: MusicUiDto, newItem: MusicUiDto): Boolean {
        return when {
            oldItem.activated != newItem.activated -> false

            oldItem is MusicUiDto.SongUiDto && newItem is MusicUiDto.SongUiDto -> {
                oldItem.songName == newItem.songName && oldItem.artistName == newItem.artistName
            }

            oldItem is MusicUiDto.ArtistUiDto && newItem is MusicUiDto.ArtistUiDto -> oldItem.name == newItem.name

            else -> false
        }
    }
}