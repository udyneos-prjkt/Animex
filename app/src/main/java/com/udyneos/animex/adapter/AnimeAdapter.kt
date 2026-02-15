package com.udyneos.animex.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.udyneos.animex.R
import com.udyneos.animex.model.Anime

class AnimeAdapter(
    private val onItemClick: (Anime) -> Unit
) : RecyclerView.Adapter<AnimeAdapter.AnimeViewHolder>() {
    
    private var animeList = listOf<Anime>()
    
    fun submitList(list: List<Anime>) {
        animeList = list
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AnimeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_anime_grid, parent, false)
        return AnimeViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: AnimeViewHolder, position: Int) {
        holder.bind(animeList[position])
    }
    
    override fun getItemCount() = animeList.size
    
    inner class AnimeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: MaterialCardView = itemView.findViewById(R.id.cardView)
        private val ivThumbnail: ImageView = itemView.findViewById(R.id.ivThumbnail)
        private val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        private val tvGenre: TextView = itemView.findViewById(R.id.tvGenre)
        private val tvEpisode: TextView = itemView.findViewById(R.id.tvEpisode)
        private val ratingChip: Chip = itemView.findViewById(R.id.ratingChip)
        
        fun bind(anime: Anime) {
            tvTitle.text = anime.title
            tvGenre.text = anime.genre.split(",").firstOrNull() ?: anime.genre
            tvEpisode.text = anime.episodeCount
            ratingChip.text = String.format("%.1f", anime.rating)
            
            // Load thumbnail dengan Glide
            if (anime.thumbnailUrl.isNotEmpty()) {
                Glide.with(itemView.context)
                    .load(anime.thumbnailUrl)
                    .apply(
                        RequestOptions()
                            .placeholder(R.drawable.placeholder_anime)
                            .error(R.drawable.error_anime)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .centerCrop()
                    )
                    .into(ivThumbnail)
            } else {
                ivThumbnail.setImageResource(R.drawable.placeholder_anime)
            }
            
            cardView.setOnClickListener {
                onItemClick(anime)
            }
        }
    }
}
