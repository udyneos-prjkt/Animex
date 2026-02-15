package com.udyneos.animex.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.udyneos.animex.R
import com.udyneos.animex.model.Episode

class EpisodeAdapter(
    private val onEpisodeClick: (Episode) -> Unit
) : RecyclerView.Adapter<EpisodeAdapter.EpisodeViewHolder>() {
    
    private var episodeList = listOf<Episode>()
    
    fun submitList(list: List<Episode>) {
        episodeList = list
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EpisodeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_episode, parent, false)
        return EpisodeViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: EpisodeViewHolder, position: Int) {
        holder.bind(episodeList[position])
    }
    
    override fun getItemCount() = episodeList.size
    
    inner class EpisodeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: MaterialCardView = itemView.findViewById(R.id.cardView)
        private val tvEpisodeNumber: TextView = itemView.findViewById(R.id.tvEpisodeNumber)
        private val tvEpisodeTitle: TextView = itemView.findViewById(R.id.tvEpisodeTitle)
        private val tvEpisodeDuration: TextView = itemView.findViewById(R.id.tvEpisodeDuration)
        private val btnPlay: MaterialButton = itemView.findViewById(R.id.btnPlay)
        
        fun bind(episode: Episode) {
            tvEpisodeNumber.text = "Episode ${episode.number}"
            tvEpisodeTitle.text = episode.title
            tvEpisodeDuration.text = episode.duration
            
            btnPlay.setOnClickListener {
                onEpisodeClick(episode)
            }
            
            cardView.setOnClickListener {
                onEpisodeClick(episode)
            }
        }
    }
}
