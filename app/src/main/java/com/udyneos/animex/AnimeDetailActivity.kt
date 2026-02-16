package com.udyneos.animex

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.udyneos.animex.adapter.EpisodeAdapter
import com.udyneos.animex.model.Anime
import com.udyneos.animex.model.Episode
import com.udyneos.animex.network.NetworkService
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
    private lateinit var btnLoadMore: MaterialButton
    private lateinit var progressBar: CircularProgressIndicator
    private lateinit var rvEpisodes: RecyclerView
    private lateinit var episodeAdapter: EpisodeAdapter
    
    private var currentAnime: Anime? = null
    private var allEpisodes = listOf<Episode>()
    private var displayedEpisodes = mutableListOf<Episode>()
    private var currentPage = 0
    private val pageSize = 30
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_anime_detail)

        initViews()
        setupToolbar()

        currentAnime = intent.getParcelableExtra("anime_data")
        if (currentAnime == null) {
            Toast.makeText(this, "Anime not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        supportActionBar?.title = currentAnime?.title
        loadAnimeDetails()
        setupEpisodeList()
        loadEpisodes()
        setupButtons()
    }
    
    private fun initViews() {
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
    }
    
    private fun setupToolbar() {
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }
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
                    .centerCrop()
                    .into(ivThumbnailBackground)
            }
        }
    }
    
    private fun setupEpisodeList() {
        episodeAdapter = EpisodeAdapter { episode ->
            startActivity(Intent(this, VideoPlayerActivity::class.java).apply {
                putExtra("video_url", episode.videoUrl)
                putExtra("episode_title", episode.title)
                putExtra("anime_title", currentAnime?.title)
            })
        }
        
        rvEpisodes.layoutManager = LinearLayoutManager(this)
        rvEpisodes.adapter = episodeAdapter
    }
    
    private fun loadEpisodes() {
        showLoading(true)
        
        lifecycleScope.launch {
            try {
                val episodes = withContext(Dispatchers.IO) {
                    currentAnime?.let { 
                        NetworkService.downloadEpisodes(it.title, forceRefresh = false) 
                    } ?: emptyList()
                }
                
                allEpisodes = episodes
                if (episodes.isNotEmpty()) {
                    loadNextPage()
                } else {
                    showLoading(false)
                    btnLoadMore.visibility = android.view.View.GONE
                    Toast.makeText(this@AnimeDetailActivity, "No episodes found", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                showLoading(false)
                Toast.makeText(this@AnimeDetailActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun loadNextPage() {
        val start = currentPage * pageSize
        val end = minOf(start + pageSize, allEpisodes.size)
        
        if (start >= allEpisodes.size) {
            btnLoadMore.visibility = android.view.View.GONE
            showLoading(false)
            return
        }
        
        val nextPage = allEpisodes.subList(start, end)
        displayedEpisodes.addAll(nextPage)
        episodeAdapter.submitList(displayedEpisodes.toList())
        
        currentPage++
        showLoading(false)
        
        val remaining = allEpisodes.size - (currentPage * pageSize)
        if (remaining > 0) {
            btnLoadMore.visibility = android.view.View.VISIBLE
            btnLoadMore.text = "Load ${minOf(remaining, pageSize)} More ($remaining left)"
        } else {
            btnLoadMore.visibility = android.view.View.GONE
        }
    }
    
    private fun showLoading(show: Boolean) {
        if (show) {
            progressBar.visibility = android.view.View.VISIBLE
            rvEpisodes.visibility = android.view.View.GONE
            btnLoadMore.visibility = android.view.View.GONE
        } else {
            progressBar.visibility = android.view.View.GONE
            rvEpisodes.visibility = android.view.View.VISIBLE
        }
    }
    
    private fun setupButtons() {
        btnPlay.setOnClickListener {
            if (displayedEpisodes.isNotEmpty()) {
                val firstEpisode = displayedEpisodes[0]
                startActivity(Intent(this, VideoPlayerActivity::class.java).apply {
                    putExtra("video_url", firstEpisode.videoUrl)
                    putExtra("episode_title", firstEpisode.title)
                    putExtra("anime_title", currentAnime?.title)
                })
            } else if (allEpisodes.isNotEmpty()) {
                val firstEpisode = allEpisodes[0]
                startActivity(Intent(this, VideoPlayerActivity::class.java).apply {
                    putExtra("video_url", firstEpisode.videoUrl)
                    putExtra("episode_title", firstEpisode.title)
                    putExtra("anime_title", currentAnime?.title)
                })
            } else {
                Toast.makeText(this, "No episodes", Toast.LENGTH_SHORT).show()
            }
        }
        
        btnLoadMore.setOnClickListener { loadNextPage() }
    }
}
