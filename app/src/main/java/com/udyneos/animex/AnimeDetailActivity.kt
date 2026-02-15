package com.udyneos.animex

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
    
    private lateinit var tvTitle: TextView
    private lateinit var tvDescription: TextView
    private lateinit var tvGenre: TextView
    private lateinit var tvStatus: TextView
    private lateinit var tvRating: TextView
    private lateinit var tvStudio: TextView
    private lateinit var tvReleaseYear: TextView
    private lateinit var tvEpisodeCount: TextView
    private lateinit var btnPlay: MaterialButton
    private lateinit var rvEpisodes: RecyclerView
    private lateinit var episodeAdapter: EpisodeAdapter
    private var currentAnime: Anime? = null
    private var episodeList = listOf<Episode>()
    private var backPressedTime = 0L
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_anime_detail)
        
        // Initialize views
        tvTitle = findViewById(R.id.tvTitle)
        tvDescription = findViewById(R.id.tvDescription)
        tvGenre = findViewById(R.id.tvGenre)
        tvStatus = findViewById(R.id.tvStatus)
        tvRating = findViewById(R.id.tvRating)
        tvStudio = findViewById(R.id.tvStudio)
        tvReleaseYear = findViewById(R.id.tvReleaseYear)
        tvEpisodeCount = findViewById(R.id.tvEpisodeCount)
        btnPlay = findViewById(R.id.btnPlay)
        rvEpisodes = findViewById(R.id.rvEpisodes)
        
        // Setup toolbar dengan back button
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
        
        // Get anime data
        currentAnime = intent.getParcelableExtra("anime_data")
        
        if (currentAnime == null) {
            Toast.makeText(this, "Anime data not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        loadAnimeDetails()
        setupEpisodeList()
        fetchEpisodesFromGithub()
        setupClickListeners()
    }
    
    private fun loadAnimeDetails() {
        currentAnime?.let { anime ->
            tvTitle.text = anime.title
            tvDescription.text = anime.description
            tvGenre.text = anime.genre
            tvStatus.text = anime.status
            tvRating.text = String.format("%.1f", anime.rating)
            tvStudio.text = anime.studio
            tvReleaseYear.text = anime.releaseYear.toString()
            tvEpisodeCount.text = anime.episodeCount
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
    
    private fun fetchEpisodesFromGithub() {
        currentAnime?.let { anime ->
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val episodes = NetworkService.fetchEpisodes(anime.id)
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
    }
    
    private fun setupClickListeners() {
        btnPlay.setOnClickListener {
            if (episodeList.isNotEmpty()) {
                // Play episode 1
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
    
    override fun onBackPressed() {
        if (backPressedTime + 2000 > System.currentTimeMillis()) {
            super.onBackPressed()
            finishAffinity()
        } else {
            Toast.makeText(this, "Tekan sekali lagi untuk keluar", Toast.LENGTH_SHORT).show()
            backPressedTime = System.currentTimeMillis()
        }
    }
}
