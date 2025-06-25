package io.fastpix.data.exo

import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaPlayer.OnCompletionListener
import android.media.MediaPlayer.OnPreparedListener
import android.media.MediaPlayer.OnSeekCompleteListener
import android.media.MediaPlayer.OnVideoSizeChangedListener
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.SystemClock
import android.util.Log
import android.view.View
import io.fastpix.data.Interfaces.DeviceContract
import io.fastpix.data.Interfaces.PlayerObserver
import io.fastpix.data.Interfaces.RequestHandler
import io.fastpix.data.entity.CustomerDataEntity
import io.fastpix.data.entity.CustomerVideoDataEntity
import io.fastpix.data.request.AnalyticsEventLogger
import io.fastpix.data.request.CustomOptions
import io.fastpix.data.request.FastPixMetrics
import io.fastpix.data.request.FastPixNetworkRequests
import io.fastpix.data.request.MediaPresentation
import io.fastpix.data.request.PlayerViewOrientation
import io.fastpix.data.request.RequestFailureException
import io.fastpix.data.streaming.EventHandler
import io.fastpix.data.streaming.InternalErrorEvent
import io.fastpix.data.streaming.MediaStreaming
import org.json.JSONException
import java.lang.ref.WeakReference
import java.util.Locale
import java.util.UUID

/**
 * FastPixBaseMediaPlayer is a custom media player wrapper that integrates FastPixMetrics
 * for tracking playback metrics and customer data.
 *
 * @param ctx Context used for initializing device-related metrics.
 * @param player The underlying MediaPlayer instance to be observed.
 * @param playerName Optional name to identify the player instance (useful for logging or analytics).
 * @param customerDataEntity Data entity containing customer-related information for metrics association.
 * @param customOptions Custom configuration options for Fine-tuning FastPixMetrics behavior.
 * @param network Optional custom implementation of RequestHandler for network operations.
 *                Defaults to FastPixNetworkRequests if not provided.
 *
 * Implements:
 * - EventHandler: Base class to manage internal event handling.
 * - PlayerObserver: Interface to observe and react to player lifecycle events.
 * - MediaPlayer.OnCompletionListener: Handles completion of media playback.
 * - MediaPlayer.OnErrorListener: Handles playback errors.
 * - MediaPlayer.OnInfoListener: Receives informational events during playback.
 * - MediaPlayer.OnPreparedListener: Called when the player is ready to start playback.
 * - MediaPlayer.OnSeekCompleteListener: Called when a seek operation completes.
 * - MediaPlayer.OnVideoSizeChangedListener: Called when the video size changes.
 */
class FastPixBaseMediaPlayer @JvmOverloads constructor(
    ctx: Context?, player: MediaPlayer?, playerName: String?,
    customerDataEntity: CustomerDataEntity?,
    customOptions: CustomOptions?,
    network: RequestHandler? = FastPixNetworkRequests()
) : EventHandler(), PlayerObserver, OnCompletionListener, MediaPlayer.OnErrorListener,
    MediaPlayer.OnInfoListener, OnPreparedListener, OnSeekCompleteListener,
    OnVideoSizeChangedListener {
    // Metrics object used to gather performance or playback statistics (custom class).
    private var fastPixMetrics: FastPixMetrics?

    // Weak reference to the MediaPlayer instance to avoid memory leaks.
    private var player: WeakReference<MediaPlayer?>?

    // Weak reference to the UI view used for video playback (e.g., SurfaceView or TextureView).
    private var playerView: WeakReference<View?>? = null

    // Weak reference to a listener triggered when video playback completes.
    private var onCompletionListener: WeakReference<OnCompletionListener?>? = null

    // Weak reference to a listener triggered when a playback error occurs.
    private var onErrorListener: WeakReference<MediaPlayer.OnErrorListener?>? = null

    // Weak reference to a listener triggered when informational events occur (e.g., buffering start/end).
    private var onInfoListener: WeakReference<MediaPlayer.OnInfoListener?>? = null

    // Weak reference to a listener triggered when the MediaPlayer is fully prepared and ready to play.
    private var onPreparedListener: WeakReference<OnPreparedListener?>? = null

    // Weak reference to a listener triggered when a seek operation completes.
    private var onSeekCompleteListener: WeakReference<OnSeekCompleteListener?>? = null

    // Weak reference to a listener triggered when the video size changes (e.g., resolution change).
    private var onVideoSizeChangedListener: WeakReference<OnVideoSizeChangedListener?>? = null

    // Original width of the video source.
    private var sourceWidth: Int = 0

    // Original height of the video source.
    private var sourceHeight: Int = 0

    // Flag to indicate if a seek operation is currently in progress.
    private var isSeeking = false

    // Flag to indicate whether playback is currently paused.
    private var isPaused = false

    // Flag to indicate if the player is currently buffering.
    private var isBufferingTrue: Boolean = false

    // Flag to indicate if rebuffering is occurring (buffering during resumed playback).
    private var isRebuffering = false

    // Flag to track whether the MediaPlayer has been prepared and is ready for playback.
    private var isPlayerPrepared: Boolean = false

    // Flag to track if playback has started at least once.
    private var hasStartedPlaying = false


    init {
        // Store a weak reference to the player to avoid memory leaks
        this.player = WeakReference(player)

        // Set up FastPixMetrics with device and network information
        FastPixMetrics.setHostDevice(ctx?.let { FPDevice(it) })  // Register the host device context
        FastPixMetrics.setHostNetworkApi(network)      // Register the host network API

        // Initialize FastPixMetrics with relevant configuration
        fastPixMetrics = FastPixMetrics(this, playerName, customerDataEntity, customOptions)

        // Add FastPixMetrics as a listener to receive player events
        addListener(fastPixMetrics)
    }

    // Notifies FastPixMetrics of a change in player view orientation (e.g., portrait or landscape)
    fun orientationChange(orientation: PlayerViewOrientation?) {
        fastPixMetrics?.orientationChange(orientation)
    }

    // Notifies FastPixMetrics of a change in the media presentation (e.g., full screen, PiP)
    fun presentationChange(presentation: MediaPresentation?) {
        fastPixMetrics?.presentationChange(presentation)
    }

    // Updates FastPixMetrics with new video metadata, such as title or ID
    fun videoChange(customerVideoDataEntity: CustomerVideoDataEntity?) {
        fastPixMetrics?.videoChange(customerVideoDataEntity)
    }

    // Updates FastPixMetrics when the program or content being played changes
    fun programChange(customerVideoDataEntity: CustomerVideoDataEntity?) {
        fastPixMetrics?.programChange(customerVideoDataEntity)
    }

    // Sets the reference to the player view using a WeakReference to avoid memory leaks
    fun setPlayerView(view: View?) {
        playerView = WeakReference(view)
    }

    /**
     * Sets the size of the player view for metric tracking.
     *
     * @param width The width of the player in pixels.
     * @param height The height of the player in pixels.
     */
    fun setPlayerSize(width: Int, height: Int) {
        fastPixMetrics?.setPlayerSize(width, height)
    }

    /**
     * Sets the size of the device screen for metric tracking.
     *
     * @param width The width of the screen in pixels.
     * @param height The height of the screen in pixels.
     */
    fun setScreenSize(width: Int, height: Int) {
        fastPixMetrics?.setScreenSize(width, height)
    }

    /**
     * Reports an error to FastPixMetrics for logging or analytics.
     *
     * @param e The exception representing the request failure.
     */
    fun error(e: RequestFailureException?) {
        fastPixMetrics?.error(e)
    }

    /**
     * Enables or disables automatic error tracking within FastPixMetrics.
     *
     * @param enabled Set to true to enable automatic error tracking, false to disable it.
     */
    fun setAutomaticErrorTracking(enabled: Boolean) {
        fastPixMetrics?.setAutomaticErrorTracking(enabled)
    }

    /**
     * Releases all resources and references held by the player and FastPixMetrics.
     *
     * This is important for preventing memory leaks and ensuring proper cleanup
     * when the media player is no longer needed.
     */
    fun release() {
        fastPixMetrics?.release() // Release internal resources used by FastPixMetrics
        fastPixMetrics = null      // Clear the FastPixMetrics reference
        player = null              // Clear the player reference
        playerView = null          // Clear the player view reference
    }

    // Helper methods to wrap another listener for MediaPlayer events. This allows users of
    // this class to chain it as a listener.
    /**
     * Stores a weak reference to the OnCompletionListener to avoid memory leaks
     * and returns the listener instance for use in MediaPlayer.
     *
     * @param listener The OnCompletionListener to register.
     * @return The same listener instance.
     */
    fun getOnCompletionListener(
        listener: OnCompletionListener?
    ): OnCompletionListener {
        onCompletionListener = WeakReference(listener) // Store listener as a weak reference
        return this                                     // Return this object (assumed to implement OnCompletionListener)
    }

    /**
     * Stores a weak reference to the OnErrorListener and returns the listener instance.
     *
     * @param listener The MediaPlayer.OnErrorListener to register.
     * @return The same listener instance.
     */
    fun getOnErrorListener(listener: MediaPlayer.OnErrorListener?): MediaPlayer.OnErrorListener {
        onErrorListener = WeakReference(listener)      // Store listener as a weak reference
        return this                                     // Return this object (assumed to implement OnErrorListener)
    }

    /**
     * Stores a weak reference to the OnInfoListener and returns the listener instance.
     *
     * @param listener The MediaPlayer.OnInfoListener to register.
     * @return The same listener instance.
     */
    fun getOnInfoListener(listener: MediaPlayer.OnInfoListener?): MediaPlayer.OnInfoListener {
        onInfoListener = WeakReference(listener)       // Store listener as a weak reference
        return this                                     // Return this object (assumed to implement OnInfoListener)
    }

    /**
     * Stores a weak reference to the OnPreparedListener and returns the listener instance.
     *
     * @param listener The OnPreparedListener to register.
     * @return The same listener instance.
     */
    fun getOnPreparedListener(
        listener: OnPreparedListener?
    ): OnPreparedListener {
        onPreparedListener = WeakReference(listener)   // Store listener as a weak reference
        return this                                     // Return this object (assumed to implement OnPreparedListener)
    }

    /**
     * Stores a weak reference to the OnSeekCompleteListener and returns the listener instance.
     *
     * @param listener The OnSeekCompleteListener to register.
     * @return The same listener instance.
     */
    fun getOnSeekCompleteListener(
        listener: OnSeekCompleteListener?
    ): OnSeekCompleteListener {
        onSeekCompleteListener = WeakReference(listener) // Store listener as a weak reference
        return this                                       // Return this object (assumed to implement OnSeekCompleteListener)
    }

    /**
     * Stores a weak reference to the OnVideoSizeChangedListener and returns the listener instance.
     *
     * @param listener The OnVideoSizeChangedListener to register.
     * @return The same listener instance.
     */
    fun getOnVideoSizeChangedListener(
        listener: OnVideoSizeChangedListener?
    ): OnVideoSizeChangedListener {
        onVideoSizeChangedListener = WeakReference(listener) // Store listener as a weak reference

        return this                                           // Return this object (assumed to implement OnVideoSizeChangedListener)
    }


    // IPlayerListener implementation
    /**
     * Returns the current playback position in milliseconds.
     *
     * @return The current position if the player is prepared and valid; otherwise, returns 0.
     */
    override fun getCurrentPosition(): Long {
        if (isPlayerPrepared && player != null && player?.get() != null) {
            return player?.get()?.currentPosition?.toLong() ?: 0L // Safely return current position
        }
        return 0 // Return 0 if player is not ready or unavailable
    }

    /**
     * Returns the MIME type of the currently playing video.
     *
     * @return The video MIME type if available; otherwise, returns an empty string.
     */
    override fun getMimeType(): String {
        if (
            Build.VERSION.SDK_INT >= 26 &&               // Ensure API level supports MediaPlayer metrics
            isPlayerPrepared &&                          // Ensure player is ready
            player != null &&                            // Check player is not null
            player?.get() != null &&                    // Check WeakReference is still valid
            player?.get()?.metrics != null             // Check metrics are available
        ) {
            if (player?.get()?.metrics?.getString(MediaPlayer.MetricsConstants.MIME_TYPE_VIDEO) != null) {
                return player?.get()?.metrics?.getString(MediaPlayer.MetricsConstants.MIME_TYPE_VIDEO)+""
            }else{
                return ""
            // Return MIME type
            }
        }
        return "" // Return empty string if unavailable
    }

    /**
     * Returns the width of the video source.
     *
     * @return The source video width in pixels.
     */
    override fun getSourceWidth(): Int = sourceWidth


    /**
     * Returns the height of the video source.
     *
     * @return The source video height in pixels.
     */
    override fun getSourceHeight(): Int = sourceHeight


    /**
     * Returns the frame rate of the video source.
     * (Note: currently hardcoded to 0; can be updated when FPS data is available.)
     *
     * @return The source video frame rate (FPS).
     */
    override fun getSourceFps(): Int = 0


    /**
     * Returns the advertised bitrate of the source.
     *
     * @return Always returns 0 as this implementation does not provide bitrate data.
     */
    override fun getSourceAdvertisedBitrate(): Int = 0


    /**
     * Returns the advertised framerate of the source.
     *
     * @return Always returns 0.0f as framerate information is not provided.
     */
    override fun getSourceAdvertisedFramerate(): Float = 0.0f


    /**
     * Returns the total duration of the media source in milliseconds.
     *
     * @return The duration if the player is prepared and valid; otherwise, 0L.
     */
    override fun getSourceDuration(): Int {
        if (isPlayerPrepared && player != null && player?.get() != null) {
            return player?.get()?.duration ?: 0 // Return media duration
        } else {
            return 0 // Return 0 if unavailable
        }
    }

    /**
     * Indicates whether the media playback is currently paused.
     *
     * @return True if the player is prepared and not playing; otherwise, false.
     */
    override fun isPaused(): Boolean {
        if (isPlayerPrepared && player != null && player?.get() != null)
            return player?.get()?.isPlaying ?: true// Return true if not playing
        return false // Default to false if player is unavailable
    }

    /**
     * Indicates whether the player is set to autoplay.
     *
     * @return Always returns false as autoplay is not implemented.
     */
    override fun isAutoPlay(): Boolean = false


    /**
     * Indicates whether the player is currently buffering.
     *
     * @return True if buffering; otherwise, false.
     */
    override fun isBuffering(): Boolean = isBufferingTrue


    /**
     * Returns the width of the player view in pixels.
     *
     * @return The width if the player view is available; otherwise, 0.
     */
    override fun getPlayerViewWidth(): Int {
        if (playerView != null && playerView?.get() != null) {
            return playerView?.get()?.width ?: 0
        }
        return 0
    }

    /**
     * Returns the height of the player view in pixels.
     *
     * @return The height if the player view is available; otherwise, 0.
     */
    override fun getPlayerViewHeight(): Int {
        if (playerView != null && playerView?.get() != null) {
            return playerView?.get()?.height ?: 0
        }
        return 0
    }

    /**
     * Returns the codec used by the player to decode the video stream.
     *
     * @return Currently returns an empty string as codec information is not implemented.
     */
    override fun getPlayerCodec(): String = ""


    /**
     * Returns the hostname of the source video URL.
     *
     * @return Currently returns an empty string as hostname extraction is not implemented.
     */
    override fun getSourceHostName(): String = ""


    /**
     * Returns the program time from the player (e.g., for live streams).
     *
     * @return Always returns 0L as program time is not currently supported.
     */
    override fun getPlayerProgramTime(): Long = 0L


    /**
     * Returns the newest available time from the manifest for live playback.
     *
     * @return Always returns 0L; manifest parsing is not implemented.
     */
    override fun getPlayerManifestNewestTime(): Long = 0L


    /**
     * Returns the video holdback time, typically used in live streams to buffer before playback.
     *
     * @return Always returns 0L; feature not implemented.
     */
    override fun getVideoHoldback(): Long = 0L


    /**
     * Returns the holdback duration for video parts in low-latency streaming.
     *
     * @return Always returns 0L; feature not implemented.
     */
    override fun getVideoPartHoldback(): Long = 0L


    /**
     * Returns the target duration of each video part (used in chunked streaming).
     *
     * @return Always returns 0L; not implemented.
     */
    override fun getVideoPartTargetDuration(): Long = 0L


    /**
     * Returns the target duration of each video segment (standard HLS/DASH).
     *
     * @return Always returns 0L; not implemented.
     */
    override fun getVideoTargetDuration(): Long = 0L


    // MediaPlayer.OnVideoSizeChangedListener implementation
    /**
     * Called when the video size changes during playback.
     *
     * This updates internal width/height tracking, forwards the event to any registered listener,
     * and dispatches a `variantChanged` event if the resolution has changed.
     *
     * @param mp The MediaPlayer instance that triggered the change.
     * @param width The new width of the video.
     * @param height The new height of the video.
     */
    override fun onVideoSizeChanged(mp: MediaPlayer, width: Int, height: Int) {
        // Forward the event to any registered external listener, if available
        if (onVideoSizeChangedListener != null && onVideoSizeChangedListener?.get() != null) {
            onVideoSizeChangedListener?.get()?.onVideoSizeChanged(mp, width, height)
        }

        // Determine if the video size actually changed
        val changed = sourceWidth != width && sourceHeight != height

        // Update the stored source dimensions
        sourceWidth = width
        sourceHeight = height

        // If there was a change in dimensions, dispatch a variantChanged event
        if (changed) {
            try {
                dispatch(MediaStreaming(MediaStreaming.EventType.variantChanged, null))
            } catch (e: JSONException) {
                Log.e("Tag", "JSONException") // Log JSON parsing error if dispatch fails
            }
        }
    }

    // MediaPlayer.OnInfoListener implementation
    // Callback for receiving informational events from the MediaPlayer.
    override fun onInfo(mp: MediaPlayer, what: Int, extra: Int): Boolean {

        // If a custom info listener is set, forward the event to it.
        if (onInfoListener != null && onInfoListener?.get() != null) {
            onInfoListener?.get()?.onInfo(mp, what, extra)
        }

        // If the media player starts buffering
        if (what == MediaPlayer.MEDIA_INFO_BUFFERING_START) {
            isBufferingTrue = true  // Set the buffering flag

            // If not seeking and playback has already started, it's a rebuffering event
            if (!isSeeking && hasStartedPlaying) {
                isRebuffering = true  // Mark that we are rebuffering
                try {
                    // Dispatch a buffering event (possibly for analytics or UI update)
                    dispatch(MediaStreaming(MediaStreaming.EventType.buffering, null))
                } catch (e: JSONException) {
                    Log.e("Tag", "JSONException")  // Log any JSON errors
                }
            }
            return true  // Handled the info event
        }
        // If buffering has ended
        else if (what == MediaPlayer.MEDIA_INFO_BUFFERING_END) {
            isBufferingTrue = false  // Reset the buffering flag

            // If we were rebuffering, end that state and notify
            if (isRebuffering) {
                isRebuffering = false
                try {
                    // Dispatch a buffered event
                    dispatch(MediaStreaming(MediaStreaming.EventType.buffered, null))
                } catch (e: JSONException) {
                    Log.e("Tag", "JSONException")  // Log JSON exception
                }

                // If the media player is playing again after buffering
                if (mp.isPlaying) {
                    isPaused = false  // Update pause state
                    try {
                        // Dispatch a playing event
                        dispatch(MediaStreaming(MediaStreaming.EventType.playing, null))
                    } catch (e: JSONException) {
                        Log.e("Tag", "JSONException")  // Log JSON exception
                    }
                }
            }
            return true  // Handled the info event
        }
        // If video rendering has started (first frame shown)
        else if (what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
            hasStartedPlaying = true  // Mark playback as started
            return true  // Handled the info event
        }

        // Event not handled here, pass it on
        return false
    }

    // MediaPlayer.OnCompletionListener implementation
    // Callback triggered when MediaPlayer finishes playback of a media file.
    override fun onCompletion(mp: MediaPlayer) {

        // If a custom completion listener is set, forward the event to it.
        if (onCompletionListener != null && onCompletionListener?.get() != null) {
            onCompletionListener?.get()?.onCompletion(mp)
        }

        // If playback was not paused manually when completion occurred
        if (!isPaused) {
            try {
                // Dispatch a pause event (possibly for analytics or UI update)
                dispatch(MediaStreaming(MediaStreaming.EventType.pause, null))
            } catch (e: JSONException) {
                Log.e("Tag", "JSONException")  // Log JSON-related error
            }
        }

        try {
            // Dispatch an "ended" event to signal that playback has finished.
            dispatch(MediaStreaming(MediaStreaming.EventType.ended, null))
        } catch (e: JSONException) {
            Log.e("Tag", "JSONException")  // Log JSON-related error
        }
    }

    // MediaPlayer.OnError implementation
// Callback triggered when an error occurs during media playback.
    override fun onError(mp: MediaPlayer, what: Int, extra: Int): Boolean {

        // MediaPlayer is now in an error state; it's no longer safe to use most of its methods.
        isPlayerPrepared = false

        // If a custom error listener is set, forward the error event to it.
        if (onErrorListener != null && onErrorListener?.get() != null) {
            onErrorListener?.get()?.onError(mp, what, extra)
        }

        // Determine a human-readable message based on the error code (`extra`).
        val message = when (extra) {
            MediaPlayer.MEDIA_ERROR_IO -> "MEDIA_ERROR_IO"  // File or network related I/O operation failure
            MediaPlayer.MEDIA_ERROR_MALFORMED -> "MEDIA_ERROR_MALFORMED"  // Bitstream is not conforming to the expected format
            MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK -> "MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK"  // Invalid for progressive streaming
            MediaPlayer.MEDIA_ERROR_TIMED_OUT -> "MEDIA_ERROR_TIMED_OUT"  // Operation timed out
            MediaPlayer.MEDIA_ERROR_UNSUPPORTED -> "MEDIA_ERROR_UNSUPPORTED"  // Unsupported media content
            else -> "unknown"  // Fallback for unidentified error codes
        }

        try {
            // Dispatch an internal error event with the error code and message (for logging or analytics).
            dispatch(InternalErrorEvent(what, message))
        } catch (e: JSONException) {
            Log.e("Tag", "JSONException")  // Log any JSON-related exceptions
        }

        // Return false to indicate that the error was not fully handled here,
        // so that the MediaPlayer may invoke its own default error handling.
        return false
    }

    // MediaPlayer.OnSeekCompleteListener implementation
// Callback triggered when a seek operation on the MediaPlayer has completed.
    override fun onSeekComplete(mp: MediaPlayer) {
        // Seek operation is no longer in progress.
        isSeeking = false

        // If a custom seek complete listener is set, forward the event to it.
        if (onSeekCompleteListener != null && onSeekCompleteListener?.get() != null) {
            onSeekCompleteListener?.get()?.onSeekComplete(mp)
        }

        // Assume buffering is finished once seeking is done.
        isBufferingTrue = false

        try {
            // Dispatch a "seeked" event to indicate that seek is complete.
            dispatch(MediaStreaming(MediaStreaming.EventType.seeked, null))
        } catch (e: JSONException) {
            Log.e("Tag", "JSONException")  // Log JSON exception
        }

        try {
            // Dispatch a "timeUpdate" event, possibly to update UI with current playback position.
            dispatch(MediaStreaming(MediaStreaming.EventType.timeUpdate, null))
        } catch (e: JSONException) {
            Log.e("Tag", "JSONException")  // Log JSON exception
        }

        // If the player is actively playing after the seek
        if (player?.get()?.isPlaying == true) {
            isPaused = false  // Mark that playback is not paused
            try {
                // Dispatch a "playing" event to reflect resumed playback after seeking.
                dispatch(MediaStreaming(MediaStreaming.EventType.playing, null))
            } catch (e: JSONException) {
                Log.e("Tag", "JSONException")  // Log JSON exception
            }
        }
    }

    // MediaPlayer.OnPreparedListener implementation
// Callback triggered when the MediaPlayer is ready to start playback.
    override fun onPrepared(mp: MediaPlayer) {

        // If a custom prepared listener is set, forward the event to it.
        if (onPreparedListener != null && onPreparedListener?.get() != null) {
            onPreparedListener?.get()?.onPrepared(mp)
        }

        // Mark the player as prepared so other parts of the app know it's ready to play.
        isPlayerPrepared = true
    }

    /**
     * Invoke this method just after [MediaPlayer.start] is called.
     */
    fun play() {
        isPaused = false
        try {
            dispatch(MediaStreaming(MediaStreaming.EventType.play, null))
        } catch (e: JSONException) {
            Log.e("Tag", "JSONException");
        }

        if (player?.get()?.isPlaying == true) {
            try {
                dispatch(MediaStreaming(MediaStreaming.EventType.playing, null))
            } catch (e: JSONException) {
                Log.e("Tag", "JSONException");
            }
        }
    }

    /**
     * Invoke this method just after [MediaPlayer.pause] is called.
     */
    fun pause() {
        if (!isPaused) {
            isPaused = true
            try {
                dispatch(MediaStreaming(MediaStreaming.EventType.pause, null))
            } catch (e: JSONException) {
                Log.e("Tag", "JSONException");
            }
        }
    }

    /**
     * Invoke this method just after [MediaPlayer.seekTo] is called.
     */
    fun seeking() {
        isSeeking = true
        isBufferingTrue = true

        if (!hasStartedPlaying) {
            try {
                dispatch(MediaStreaming(MediaStreaming.EventType.play, null))
            } catch (e: JSONException) {
                Log.e("Tag", "JSONException");
            }
        }

        if (!isPaused) {
            isPaused = true
            try {
                dispatch(MediaStreaming(MediaStreaming.EventType.pause, null))
            } catch (e: JSONException) {
                Log.e("Tag", "JSONException");
            }
        }

        try {
            dispatch(MediaStreaming(MediaStreaming.EventType.seeking, null))
        } catch (e: JSONException) {
            Log.e("Tag", "JSONException");
        }
    }


    /**
     * Should be set to true once [MediaPlayer.setDataSource] has been called. Should be
     * set to false if [MediaPlayer.reset] is called on the encapsulated player.
     */
    fun setIsPlayerPrepared(isPrepared: Boolean) {
        isPlayerPrepared = isPrepared
    }


    internal class FPDevice(ctx: Context) : DeviceContract {
        private var contextRef: WeakReference<Context>

        private var deviceId: String
        private var appName = ""
        private var appVersion = ""
        val CONNECTION_TYPE_CELLULAR: String = "cellular"
        val CONNECTION_TYPE_WIFI: String = "wifi"
        val CONNECTION_TYPE_WIRED: String = "wired"
        val CONNECTION_TYPE_OTHER: String = "other"

        val EXO_SOFTWARE = "MediaPlayer"
        val FASTPIX_DEVICE_ID: String = "FASTPIX_DEVICE_ID"

        init {
            val sharedPreferences =
                ctx.getSharedPreferences(FASTPIX_DEVICE_ID, Context.MODE_PRIVATE)
            deviceId = sharedPreferences.getString(FASTPIX_DEVICE_ID, "")!!
            if (deviceId == null || deviceId.isEmpty()) {
                deviceId = UUID.randomUUID().toString()
                val editor = sharedPreferences.edit()
                editor.putString(FASTPIX_DEVICE_ID, deviceId)
                editor.commit()
            }

            contextRef = WeakReference(ctx)
            try {
                val pi = ctx.packageManager.getPackageInfo(ctx.packageName, 0)
                appName = pi.packageName
                appVersion = pi.versionName!!
            } catch (e: PackageManager.NameNotFoundException) {
                AnalyticsEventLogger.w(
                    "FPDevice",
                    "ExoPlayer library-HLS not available. Some features may not work. Exception: " + e.message
                )
            }
        }

        override fun getHardwareArchitecture(): String = Build.HARDWARE


        override fun getOSFamily(): String = "Android"


        override fun getOSVersion(): String =
            Build.VERSION.RELEASE + " (" + Build.VERSION.SDK_INT + ")"


        override fun getDeviceName(): String = ""


        override fun getDeviceCategory(): String = ""


        override fun getManufacturer(): String = Build.MANUFACTURER


        override fun getModelName(): String = Build.MODEL


        override fun getPlayerVersion(): String = osVersion


        override fun getDeviceId(): String = deviceId


        override fun getAppName(): String = appName


        override fun getAppVersion(): String = appVersion


        override fun getPluginName(): String = "media_player_fastpix"


        override fun getPluginVersion(): String = BuildConfig.LIBRARY_VERSION


        override fun getPlayerSoftware(): String = EXO_SOFTWARE

        override fun getNetworkConnectionType(): String {
            val context = contextRef.get() ?: return ""

            val connectivityMgr =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            if (connectivityMgr != null) {
                val activeNetwork = connectivityMgr.activeNetworkInfo
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val nc = connectivityMgr.getNetworkCapabilities(connectivityMgr.activeNetwork)
                    if (nc == null) {
                        AnalyticsEventLogger.d(
                            "FPDevice",
                            "ERROR: Failed to obtain NetworkCapabilities manager !!!"
                        )
                        return ""
                    }
                    return if (nc.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                        CONNECTION_TYPE_WIRED
                    } else if (nc.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                        CONNECTION_TYPE_WIFI
                    } else if (nc.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                        CONNECTION_TYPE_CELLULAR
                    } else {
                        CONNECTION_TYPE_OTHER
                    }
                } else {
                    if (activeNetwork != null) {
                        return when (activeNetwork.type) {
                            ConnectivityManager.TYPE_ETHERNET -> CONNECTION_TYPE_WIRED
                            ConnectivityManager.TYPE_WIFI -> CONNECTION_TYPE_WIFI
                            ConnectivityManager.TYPE_MOBILE -> CONNECTION_TYPE_CELLULAR
                            else -> CONNECTION_TYPE_OTHER
                        }
                    }
                }
            }
            return ""
        }

        override fun getElapsedRealtime(): Long = SystemClock.elapsedRealtime()

        override fun outputLog(logPriority: String, s: String, s1: String, throwable: Throwable) {
            Log.e("outputLog", s, throwable)
        }

        override fun outputLog(logPriority: String, tag: String, msg: String) {
            when (logPriority.lowercase(Locale.getDefault())) {
                "error" -> Log.e(tag, msg)
                "warn" -> Log.w(tag, msg)
                "info" -> Log.i(tag, msg)
                "debug" -> Log.d(tag, msg)
                else -> Log.v(tag, msg)
            }
        }

        override fun outputLog(tag: String, msg: String) {
            Log.v(tag, msg)
        }

    }

}
