package com.udyneos.animex

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.android.material.chip.Chip
import com.udyneos.animex.databinding.ActivityAnimeDetailBinding
import com.udyneos.animex.model.Anime

class AnimeDetailActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityAnimeDetailBinding
    private var currentAnime: Anime? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAnimeDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Get anime data from intent
        currentAnime = intent.getParcelableExtra("anime_data")
        
        setupToolbar()
        loadAnimeDetails()
        setupClickListeners()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        
        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }
    
    private fun loadAnimeDetails() {
        currentAnime?.let { anime ->
            // Set title
            binding.toolbar.title = anime.title
            
            // Load thumbnail with Glide
            Glide.with(this)
                .load(anime.thumbnailUrl)
                .placeholder(R.drawable.placeholder_anime)
                .error(R.drawable.error_anime)
                .centerCrop()
                .into(binding.ivThumbnail)
            
            // Set basic info
            binding.tvTitle.text = anime.title
            binding.tvRating.text = String.format("%.1f", anime.rating)
            binding.tvReleaseYear.text = anime.releaseYear.toString()
            binding.tvStudio.text = anime.studio
            binding.tvStatus.text = anime.status
            binding.tvEpisodeCount.text = anime.episodeCount
            binding.tvDescription.text = anime.description
            
            // Set genres as chips
            setupGenreChips(anime.genre)
            
            // Set synopsis expand/collapse
            setupSynopsisToggle()
        }
    }
    
    private fun setupGenreChips(genres: String) {
        binding.chipGroupGenres.removeAllViews()
        val genreList = genres.split(",").map { it.trim() }
        
        genreList.forEach { genre ->
            val chip = Chip(this).apply {
                text = genre
                isClickable = false
                isCheckable = false
                setChipBackgroundColorResource(com.google.android.material.R.color.material_dynamic_primary50)
                setTextColor(resources.getColor(android.R.color.white, null))
            }
            binding.chipGroupGenres.addView(chip)
        }
    }
    
    private fun setupSynopsisToggle() {
        var isExpanded = false
        
        binding.tvDescription.post {
            val maxLines = 4
            if (binding.tvDescription.lineCount > maxLines) {
                binding.tvDescription.maxLines = maxLines
                binding.btnToggleSynopsis.visibility = android.view.View.VISIBLE
                
                binding.btnToggleSynopsis.setOnClickListener {
                    isExpanded = !isExpanded
                    if (isExpanded) {
                        binding.tvDescription.maxLines = Int.MAX_VALUE
                        binding.btnToggleSynopsis.text = "Show Less"
                    } else {
                        binding.tvDescription.maxLines = maxLines
                        binding.btnToggleSynopsis.text = "Read More"
                    }
                }
            }
        }
    }
    
    private fun setupClickListeners() {
        // Play button
        binding.btnPlay.setOnClickListener {
            currentAnime?.let { anime ->
                if (anime.videoUrl.isNotEmpty()) {
                    val intent = Intent(this, VideoPlayerActivity::class.java)
                    intent.putExtra("anime_title", anime.title)
                    intent.putExtra("video_url", anime.videoUrl)
                    startActivity(intent)
                } else {
                    Toast.makeText(
                        this,
                        "Video not available",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
        
        // Share button
        binding.btnShare.setOnClickListener {
            currentAnime?.let { anime ->
                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, "Check out this anime: ${anime.title}\n\nDownload AnimeX to watch!")
                    type = "text/plain"
                }
                startActivity(Intent.createChooser(shareIntent, "Share via"))
            }
        }
        
        // Favorite button
        binding.btnFavorite.setOnClickListener {
            // Toggle favorite state
            val isFavorite = binding.btnFavorite.tag as? Boolean ?: false
            if (isFavorite) {
                binding.btnFavorite.setImageResource(R.drawable.ic_favorite_border)
                binding.btnFavorite.tag = false
                Toast.makeText(this, "Removed from favorites", Toast.LENGTH_SHORT).show()
            } else {
                binding.btnFavorite.setImageResource(R.drawable.ic_favorite)
                binding.btnFavorite.tag = true
                Toast.makeText(this, "Added to favorites", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
