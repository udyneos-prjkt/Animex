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
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.udyneos.animex.adapter.EpisodeAdapter
import com.udyneos.animex.model.Anime
import com.udyneos.animex.model.Episode
import com.udyneos.animex.network.NetworkService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
    private lateinit var btnLoadMore: MaterialButton
    private lateinit var progressBar: CircularProgressIndicator
    private lateinit var rvEpisodes: RecyclerView
    private lateinit var episodeAdapter: EpisodeAdapter
    private var allEpisodes = listOf<Episode>()
    private var displayedEpisodes = mutableListOf<Episode>()
    private var currentPage = 0
    private val pageSize = 50
    private var currentAnime: Anime? = null
    private var isLoading = false
    
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
        btnLoadMore = findViewById(R.id.btnLoadMore)
        progressBar = findViewById(R.id.progressBar)
        rvEpisodes = findViewById(R.id.rvEpisodes)

        // Setup toolbar dengan back button ke MainActivity
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        
        toolbar.setNavigationOnClickListener {
            finish()
        }

        // Get anime data
        currentAnime = intent.getParcelableExtra("anime_data")

        if (currentAnime == null) {
            Toast.makeText(this, "Anime data not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Load anime details
        loadAnimeDetails()
        
        // Setup RecyclerView untuk episode
        setupEpisodeList()
        
        // Fetch semua episodes dari GitHub
        fetchAllEpisodesFromGithub(currentAnime!!.id)

        // Setup buttons
        setupPlayButton()
        setupLoadMoreButton()
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
            
            if (anime.thumbnailUrl.isNotEmpty()) {
                Glide.with(this)
                    .load(anime.thumbnailUrl)
                    .placeholder(R.drawable.placeholder_anime)
                    .error(R.drawable.error_anime)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .centerCrop()
                    .into(ivThumbnailBackground)
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
    
    private fun fetchAllEpisodesFromGithub(animeId: Int) {
        showLoading(true)
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Fetch episodes dari NetworkService
                val episodes = NetworkService.fetchEpisodes(animeId)
                allEpisodes = episodes
                
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    
                    if (episodes.isNotEmpty()) {
                        // Load page pertama
                        loadNextPage()
                        updateEpisodeCount()
                    } else {
                        Toast.makeText(this@AnimeDetailActivity, "No episodes found", Toast.LENGTH_SHORT).show()
                        btnLoadMore.visibility = android.view.View.GONE
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    Toast.makeText(this@AnimeDetailActivity, "Error loading episodes", Toast.LENGTH_SHORT).show()
                    e.printStackTrace()
                }
            }
        }
    }
    
    private fun loadNextPage() {
        if (isLoading) return
        
        isLoading = true
        
        val start = currentPage * pageSize
        val end = minOf(start + pageSize, allEpisodes.size)
        
        if (start >= allEpisodes.size) {
            // Sudah mencapai akhir
            btnLoadMore.visibility = android.view.View.GONE
            isLoading = false
            return
        }
        
        // Simulasi loading untuk UX yang lebih baik
        CoroutineScope(Dispatchers.Main).launch {
            btnLoadMore.isEnabled = false
            btnLoadMore.text = "Loading..."
            
            // Delay kecil untuk UX
            delay(500)
            
            val nextPageEpisodes = allEpisodes.subList(start, end)
            displayedEpisodes.addAll(nextPageEpisodes)
            episodeAdapter.submitList(displayedEpisodes.toList())
            
            currentPage++
            isLoading = false
            
            // Update button
            btnLoadMore.isEnabled = true
            val remainingEpisodes = allEpisodes.size - (currentPage * pageSize)
            
            if (remainingEpisodes > 0) {
                val nextBatchSize = minOf(remainingEpisodes, pageSize)
                btnLoadMore.text = "Load $nextBatchSize More Episodes ($remainingEpisodes left)"
            } else {
                btnLoadMore.visibility = android.view.View.GONE
            }
        }
    }
    
    private fun updateEpisodeCount() {
        val totalEpisodes = allEpisodes.size
        tvEpisodeCount.text = "$totalEpisodes Episodes"
    }
    
    private fun showLoading(show: Boolean) {
        if (show) {
            progressBar.visibility = android.view.View.VISIBLE
            rvEpisodes.visibility = android.view.View.GONE
            btnLoadMore.visibility = android.view.View.GONE
        } else {
            progressBar.visibility = android.view.View.GONE
            rvEpisodes.visibility = android.view.View.VISIBLE
            
            // Tampilkan button load more jika masih ada episode
            if (allEpisodes.size > pageSize) {
                btnLoadMore.visibility = android.view.View.VISIBLE
                val remainingEpisodes = allEpisodes.size
                btnLoadMore.text = "Load ${minOf(remainingEpisodes, pageSize)} More Episodes ($remainingEpisodes left)"
            }
        }
    }
    
    private fun setupPlayButton() {
        btnPlay.setOnClickListener {
            if (displayedEpisodes.isNotEmpty()) {
                val firstEpisode = displayedEpisodes[0]
                val intent = Intent(this, VideoPlayerActivity::class.java)
                intent.putExtra("video_url", firstEpisode.videoUrl)
                intent.putExtra("episode_title", firstEpisode.title)
                intent.putExtra("anime_title", currentAnime?.title)
                startActivity(intent)
            } else if (allEpisodes.isNotEmpty()) {
                val firstEpisode = allEpisodes[0]
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
    
    private fun setupLoadMoreButton() {
        btnLoadMore.setOnClickListener {
            loadNextPage()
        }
    }
}
