package com.udyneos.animex

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.button.MaterialButton
import com.udyneos.animex.adapter.EpisodeAdapter
import com.udyneos.animex.model.Anime
import com.udyneos.animex.model.Episode
import com.udyneos.animex.network.NetworkService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AnimeDetailActivity : AppCompatActivity() {
    
    private lateinit var ivThumbnailBackground: ImageView
    private lateinit var tvTitle: TextView
    private lateinit var tvRating: TextView
    private lateinit var tvReleaseYear: TextView
    private lateinit var tvStudio: TextView
    private lateinit var tvGenre: TextView
    private lateinit var tvStatus: TextView
    private lateinit var tvEpisodeCount: TextView
    private lateinit var tvDescription: TextView
    private lateinit var btnPlay: MaterialButton
    private lateinit var rvEpisodes: RecyclerView
    private lateinit var episodeAdapter: EpisodeAdapter
    private var episodeList = listOf<Episode>()
    private var currentAnime: Anime? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_anime_detail)

        // Initialize views
        ivThumbnailBackground = findViewById(R.id.ivThumbnailBackground)
        tvTitle = findViewById(R.id.tvTitle)
        tvRating = findViewById(R.id.tvRating)
        tvReleaseYear = findViewById(R.id.tvReleaseYear)
        tvStudio = findViewById(R.id.tvStudio)
        tvGenre = findViewById(R.id.tvGenre)
        tvStatus = findViewById(R.id.tvStatus)
        tvEpisodeCount = findViewById(R.id.tvEpisodeCount)
        tvDescription = findViewById(R.id.tvDescription)
        btnPlay = findViewById(R.id.btnPlay)
        rvEpisodes = findViewById(R.id.rvEpisodes)

        // Setup toolbar dengan back button ke MainActivity
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        
        toolbar.setNavigationOnClickListener {
            finish() // Kembali ke MainActivity
        }

        // Get anime data
        currentAnime = intent.getParcelableExtra("anime_data")

        if (currentAnime == null) {
            Toast.makeText(this, "Anime data not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Load anime details dengan thumbnail sebagai background
        loadAnimeDetails()
        
        // Setup RecyclerView untuk episode
        setupEpisodeList()
        
        // Fetch episodes dari GitHub
        fetchEpisodesFromGithub(currentAnime!!.id)

        // Setup play button untuk memutar episode 1
        setupPlayButton()
    }
    
    private fun loadAnimeDetails() {
        currentAnime?.let { anime ->
            tvTitle.text = anime.title
            tvRating.text = String.format("%.1f", anime.rating)
            tvReleaseYear.text = anime.releaseYear.toString()
            tvStudio.text = anime.studio
            tvGenre.text = anime.genre
            tvStatus.text = anime.status
            tvEpisodeCount.text = anime.episodeCount
            tvDescription.text = anime.description
            
            // Load thumbnail sebagai background dengan Glide
            if (anime.thumbnailUrl.isNotEmpty()) {
                Glide.with(this)
                    .load(anime.thumbnailUrl)
                    .placeholder(R.drawable.placeholder_anime)
                    .error(R.drawable.error_anime)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .centerCrop()
                    .into(ivThumbnailBackground)
            } else {
                ivThumbnailBackground.setImageResource(R.drawable.placeholder_anime)
            }
        }
    }
    
    private fun setupEpisodeList() {
        episodeAdapter = EpisodeAdapter { episode ->
            val intent = Intent(this, VideoPlayerActivity::class.java)
            intent.putExtra("video_url", episode.videoUrl)
            intent.putExtra("episode_title", episode.title)
            intent.putExtra("anime_title", currentAnime?.title)
            startActivity(intent)
        }
        
        rvEpisodes.layoutManager = LinearLayoutManager(this)
        rvEpisodes.adapter = episodeAdapter
    }
    
    private fun fetchEpisodesFromGithub(animeId: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val episodes = NetworkService.fetchEpisodes(animeId)
                episodeList = episodes
                
                withContext(Dispatchers.Main) {
                    if (episodes.isNotEmpty()) {
                        episodeAdapter.submitList(episodes)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    private fun setupPlayButton() {
        btnPlay.setOnClickListener {
            if (episodeList.isNotEmpty()) {
                val firstEpisode = episodeList[0]
                val intent = Intent(this, VideoPlayerActivity::class.java)
                intent.putExtra("video_url", firstEpisode.videoUrl)
                intent.putExtra("episode_title", firstEpisode.title)
                intent.putExtra("anime_title", currentAnime?.title)
                startActivity(intent)
            } else {
                Toast.makeText(this, "No episodes available", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
