package io.fastpix.data.mediaplayer.sample

import android.content.res.Configuration
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import io.fastpix.data.entity.CustomDataEntity
import io.fastpix.data.entity.CustomerDataEntity
import io.fastpix.data.entity.CustomerPlayerDataEntity
import io.fastpix.data.entity.CustomerVideoDataEntity
import io.fastpix.data.entity.CustomerViewDataEntity
import io.fastpix.data.exo.FastPixBaseMediaPlayer
import io.fastpix.data.request.CustomOptions
import io.fastpix.data.request.PlayerViewOrientation
import java.io.IOException
import java.util.UUID

/**
 * Minimal end-to-end sample of the FastPix Media Player SDK.
 *
 * It plays an HLS/MP4 stream on a plain [MediaPlayer] rendered into a [SurfaceView], and wires
 * every playback signal FastPix needs into [FastPixBaseMediaPlayer] so the session shows up on the
 * FastPix dashboard.
 *
 * The SDK cannot observe [MediaPlayer] state on its own, so the three user-driven transitions —
 * play, pause and seek — have to be reported explicitly right after the corresponding
 * [MediaPlayer] call. Everything else (prepare, buffering, size changes, completion, errors) is
 * picked up through the listener wrappers registered in [attachFastPixListeners].
 */
class VideoPlayerActivity : AppCompatActivity() {

    private var mediaPlayer: MediaPlayer? = null
    private var fastPixBaseMediaPlayer: FastPixBaseMediaPlayer? = null

    private lateinit var surfaceView: SurfaceView
    private lateinit var playerControls: View
    private lateinit var playPauseButton: ImageButton

    /**
     * True only between [MediaPlayer.OnPreparedListener] and the next reset. `MediaPlayer` throws
     * (natively, as `error (-38, 0)`) if duration/position/start are queried before that point, so
     * every control below is gated on it.
     */
    private var isPrepared = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_player)

        surfaceView = findViewById(R.id.surfaceView)
        surfaceView.holder.addCallback(surfaceCallback)

        playerControls = findViewById(R.id.playerControls)
        playPauseButton = findViewById(R.id.playPauseButton)

        playPauseButton.setOnClickListener {
            if (mediaPlayer?.isPlaying == true) pause() else play()
            showControls()
        }
        findViewById<ImageButton>(R.id.seekButton).setOnClickListener {
            seekForward()
            showControls()
        }

        // Tapping the video toggles the overlay, the way a real player behaves.
        surfaceView.setOnClickListener {
            if (playerControls.isVisible) hideControls() else showControls()
        }
    }

    // ---------------------------------------------------------------------------------------
    // Control overlay
    // ---------------------------------------------------------------------------------------

    private fun showControls() {
        playerControls.isVisible = true
        playerControls.removeCallbacks(hideControlsRunnable)
        // Keep the overlay up while paused; only auto-hide during playback.
        if (mediaPlayer?.isPlaying == true) {
            playerControls.postDelayed(hideControlsRunnable, CONTROLS_TIMEOUT_MS)
        }
    }

    private fun hideControls() {
        playerControls.removeCallbacks(hideControlsRunnable)
        playerControls.isVisible = false
    }

    private val hideControlsRunnable = Runnable { playerControls.isVisible = false }

    /** Swaps the icon to match what the button will do next. */
    private fun updatePlayPauseIcon() {
        val playing = mediaPlayer?.isPlaying == true
        playPauseButton.setImageResource(
            if (playing) R.drawable.ic_pause else R.drawable.ic_play_arrow
        )
        playPauseButton.contentDescription =
            getString(if (playing) R.string.cd_pause else R.string.cd_play)
    }

    private val surfaceCallback = object : SurfaceHolder.Callback {
        override fun surfaceCreated(holder: SurfaceHolder) {
            startPlayback(holder)
        }

        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            // The SDK reports the rendered player size alongside the source resolution.
            fastPixBaseMediaPlayer?.setPlayerSize(width, height)
        }

        override fun surfaceDestroyed(holder: SurfaceHolder) {
            releasePlayers()
        }
    }

    // ---------------------------------------------------------------------------------------
    // Setup
    // ---------------------------------------------------------------------------------------

    private fun startPlayback(holder: SurfaceHolder) {
        if (mediaPlayer != null) return

        try {
            val player = MediaPlayer()
            player.setDataSource(VIDEO_URL)
            player.setDisplay(holder)
            // Only takes effect once a SurfaceHolder is attached, so it has to follow setDisplay().
            player.setScreenOnWhilePlaying(true)
            mediaPlayer = player

            fastPixBaseMediaPlayer = FastPixBaseMediaPlayer(
                this,
                player,
                PLAYER_NAME,
                buildCustomerData(),
                CustomOptions()
            ).apply {
                // Lets the SDK read the player view dimensions for view-size metrics.
                setPlayerView(surfaceView)
                // Automatic error tracking is on by default; call this to opt out and report
                // errors yourself via error(...).
                setAutomaticErrorTracking(true)
            }

            attachFastPixListeners(player)
            player.prepareAsync()
        } catch (e: IOException) {
            Log.e(TAG, "Unable to open $VIDEO_URL", e)
            toast("Unable to open the video source")
        }
    }

    // The SDK stores the listeners handed to getOnXxxListener() in WeakReferences, so an inline
    // lambda is collected on the next GC and the app silently stops receiving callbacks. Keep them
    // as fields — anything that outlives the player works.
    private val preparedListener = MediaPlayer.OnPreparedListener {
        isPrepared = true
        // The SDK flips its own prepared flag *after* this callback returns, so start playback on
        // the next loop iteration — otherwise the play event is recorded against a player the SDK
        // still considers unprepared.
        surfaceView.post { play() }
    }

    private val completionListener = MediaPlayer.OnCompletionListener {
        // Back to a "play" affordance so the video can be replayed from the overlay.
        updatePlayPauseIcon()
        showControls()
        toast("Playback finished")
    }

    private val seekCompleteListener = MediaPlayer.OnSeekCompleteListener { /* no-op */ }

    private val videoSizeChangedListener =
        MediaPlayer.OnVideoSizeChangedListener { _, width, height ->
            Log.d(TAG, "Video size: ${width}x$height")
        }

    private val infoListener = MediaPlayer.OnInfoListener { _, what, extra ->
        Log.d(TAG, "MediaPlayer info: what=$what extra=$extra")
        false
    }

    private val errorListener = MediaPlayer.OnErrorListener { _, what, extra ->
        // MediaPlayer moves to its Error state here; nothing may be queried until reset().
        isPrepared = false
        updatePlayPauseIcon()
        Log.e(TAG, "MediaPlayer error: what=$what extra=$extra")
        toast("Playback error ($what)")
        true
    }

    /**
     * Every listener is registered through its FastPix wrapper. The wrapper records the event and
     * forwards the callback to the listener passed in, so the app keeps full control of the
     * callbacks while FastPix still sees them.
     */
    private fun attachFastPixListeners(player: MediaPlayer) {
        val fastPix = fastPixBaseMediaPlayer ?: return

        player.setOnPreparedListener(fastPix.getOnPreparedListener(preparedListener))
        player.setOnCompletionListener(fastPix.getOnCompletionListener(completionListener))
        player.setOnSeekCompleteListener(fastPix.getOnSeekCompleteListener(seekCompleteListener))
        player.setOnVideoSizeChangedListener(
            fastPix.getOnVideoSizeChangedListener(videoSizeChangedListener)
        )
        player.setOnInfoListener(fastPix.getOnInfoListener(infoListener))
        player.setOnErrorListener(fastPix.getOnErrorListener(errorListener))
    }

    /**
     * Player, video and view metadata sent with the session. `workspaceKey` is the only required
     * field — grab it from the FastPix dashboard. Everything else makes the dashboard easier to
     * slice.
     */
    private fun buildCustomerData(): CustomerDataEntity {
        val playerData = CustomerPlayerDataEntity().apply {
            workspaceKey = WORKSPACE_KEY
        }

        val videoData = CustomerVideoDataEntity().apply {
            videoId = VIDEO_ID
            videoTitle = VIDEO_TITLE
            videoSourceUrl = VIDEO_URL
        }

        val viewData = CustomerViewDataEntity().apply {
            // A fresh id per view; keep it stable for the lifetime of a single playback session.
            viewSessionId = UUID.randomUUID().toString()
        }

        return CustomerDataEntity(playerData, videoData, viewData).apply {
            // Up to 10 free-form dimensions you can filter by on the dashboard.
            customData = CustomDataEntity().apply {
                customData1 = "sample-app"
                customData2 = BuildConfig.BUILD_TYPE
            }
        }
    }

    // ---------------------------------------------------------------------------------------
    // Playback controls — each one reports the transition to FastPix
    // ---------------------------------------------------------------------------------------

    private fun play() {
        val player = mediaPlayer ?: return
        if (!isPrepared || player.isPlaying) return
        player.start()
        fastPixBaseMediaPlayer?.play()
        updatePlayPauseIcon()
        showControls()
    }

    private fun pause() {
        val player = mediaPlayer ?: return
        if (isPrepared && player.isPlaying) {
            player.pause()
            fastPixBaseMediaPlayer?.pause()
            updatePlayPauseIcon()
            showControls()
        }
    }

    private fun seekForward() {
        val player = mediaPlayer ?: return
        if (!isPrepared) return
        val target = (player.currentPosition + SEEK_STEP_MS).coerceAtMost(player.duration)
        player.seekTo(target)
        fastPixBaseMediaPlayer?.seeking()
    }

    // ---------------------------------------------------------------------------------------
    // Lifecycle
    // ---------------------------------------------------------------------------------------

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        fastPixBaseMediaPlayer?.orientationChange(
            if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                PlayerViewOrientation.LANDSCAPE
            } else {
                PlayerViewOrientation.PORTRAIT
            }
        )
    }

    override fun onPause() {
        super.onPause()
        pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        releasePlayers()
    }

    private fun releasePlayers() {
        isPrepared = false
        playerControls.removeCallbacks(hideControlsRunnable)
        // Release FastPix first so the in-flight session is flushed while the player is still alive.
        fastPixBaseMediaPlayer?.release()
        fastPixBaseMediaPlayer = null

        mediaPlayer?.release()
        mediaPlayer = null
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val TAG = "FastPixSample"

        private const val PLAYER_NAME = "media-player-sample"

        /** Replace with the workspace key from your FastPix dashboard. */
        private const val WORKSPACE_KEY = "your-workspace-key"

        private const val VIDEO_ID = "sintel-trailer"
        private const val VIDEO_TITLE = "Sintel Trailer"
        private const val VIDEO_URL = "https://media.w3.org/2010/05/sintel/trailer.mp4"

        private const val SEEK_STEP_MS = 10_000

        private const val CONTROLS_TIMEOUT_MS = 3_000L
    }
}
