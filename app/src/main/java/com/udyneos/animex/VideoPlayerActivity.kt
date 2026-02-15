package com.udyneos.animex

import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.udyneos.animex.databinding.ActivityVideoPlayerBinding

class VideoPlayerActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityVideoPlayerBinding
    private var exoPlayer: ExoPlayer? = null
    private var currentWindow = 0
    private var playbackPosition = 0L
    private var isPlaying = true
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVideoPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Keep screen on while playing video
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        // Set requested orientation to landscape for video player
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        
        val videoUrl = intent.getStringExtra("video_url") ?: ""
        val animeTitle = intent.getStringExtra("anime_title") ?: "Video Player"
        
        setupToolbar(animeTitle)
        initializePlayer(videoUrl)
        setupListeners()
    }
    
    private fun setupToolbar(title: String) {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = title
        
        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }
    
    private fun initializePlayer(videoUrl: String) {
        if (videoUrl.isEmpty()) {
            Toast.makeText(this, "Invalid video URL", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        try {
            // Create track selector
            val trackSelector = DefaultTrackSelector(this).apply {
                setParameters(buildUponParameters().setMaxVideoSizeSd())
            }
            
            // Build player
            exoPlayer = ExoPlayer.Builder(this)
                .setTrackSelector(trackSelector)
                .build()
            
            // Attach player to view
            binding.playerView.player = exoPlayer
            
            // Create media source with better buffering
            val dataSourceFactory = DefaultHttpDataSource.Factory()
            
            val mediaSource = when {
                videoUrl.contains(".m3u8") -> {
                    // HLS stream
                    HlsMediaSource.Factory(dataSourceFactory)
                        .createMediaSource(MediaItem.fromUri(Uri.parse(videoUrl)))
                }
                else -> {
                    // Regular video file
                    val mediaItem = MediaItem.fromUri(Uri.parse(videoUrl))
                    com.google.android.exoplayer2.source.ProgressiveMediaSource.Factory(dataSourceFactory)
                        .createMediaSource(mediaItem)
                }
            }
            
            // Prepare player with media source
            exoPlayer?.apply {
                setMediaSource(mediaSource)
                seekTo(currentWindow, playbackPosition)
                prepare()
                playWhenReady = isPlaying
            }
            
            // Add listener for player events
            exoPlayer?.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        ExoPlayer.STATE_BUFFERING -> {
                            binding.progressBar.visibility = View.VISIBLE
                        }
                        ExoPlayer.STATE_READY -> {
                            binding.progressBar.visibility = View.GONE
                        }
                        ExoPlayer.STATE_ENDED -> {
                            // Video ended
                            Toast.makeText(this@VideoPlayerActivity, "Video ended", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                
                override fun onPlayerError(error: PlaybackException) {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(
                        this@VideoPlayerActivity,
                        "Error playing video: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
            
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to initialize player: ${e.message}", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
    
    private fun setupListeners() {
        // Controller visibility listener
        binding.playerView.setControllerVisibilityListener(
            StyledPlayerView.ControllerVisibilityListener { visibility ->
                if (visibility == View.GONE) {
                    // Hide toolbar when controllers are hidden
                    binding.toolbar.visibility = View.GONE
                    
                    // Enter immersive mode
                    hideSystemUI()
                } else {
                    // Show toolbar when controllers are visible
                    binding.toolbar.visibility = View.VISIBLE
                    
                    // Exit immersive mode
                    showSystemUI()
                }
            }
        )
        
        // Fullscreen button click
        binding.playerView.findViewById<View>(com.google.android.exoplayer2.ui.R.id.exo_fullscreen)?.setOnClickListener {
            toggleFullscreen()
        }
    }
    
    private fun toggleFullscreen() {
        if (resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
    }
    
    private fun hideSystemUI() {
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                )
    }
    
    private fun showSystemUI() {
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                )
    }
    
    override fun onResume() {
        super.onResume()
        if (exoPlayer == null) {
            val videoUrl = intent.getStringExtra("video_url") ?: ""
            initializePlayer(videoUrl)
        }
    }
    
    override fun onPause() {
        super.onPause()
        releasePlayer()
    }
    
    override fun onStop() {
        super.onStop()
        releasePlayer()
    }
    
    private fun releasePlayer() {
        exoPlayer?.let { player ->
            playbackPosition = player.currentPosition
            currentWindow = player.currentWindowIndex
            isPlaying = player.playWhenReady
            player.release()
            exoPlayer = null
        }
    }
    
    override fun onBackPressed() {
        // Reset orientation
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        super.onBackPressed()
    }
}
