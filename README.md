# FastPix Media Player SDK

This SDK enables seamless integration with **Android Media Player**, offering advanced video analytics via the **FastPix Dashboard**. It's a wrapper built on [FastPix’s core Java library](https://github.com/FastPix/android-core-data-sdk) to deliver performance monitoring for video applications using [Google's Android Media Player](https://developer.android.com/media/platform/mediaplayer).

---

## Key Features
- **User engagement tracking** – Track key user actions such as play, pause, seek, and video completion to measure engagement.
- **Playback quality analytics** – Evaluate buffering, resolution changes, and network issues.
- **No Dependency** – Easily integrate FastPix SDK with Android’s native MediaPlayer without external dependencies.
- **Device & app diagnostics** – Gain insights into playback issues across devices.
- **Error logging** – Automatically capture fatal and handled playback errors.
---

## Prerequisites
- Android Studio Arctic Fox or newer
- Android SDK version 21+
- GitHub Personal Access Token (PAT) for private Maven access

---

## Installation

### Step 1: Add the GitHub Maven Repository to `settings.gradle`
```groovy
repositories {
    maven {
        url = uri("https://maven.pkg.github.com/FastPix/android-media-player-data")
        credentials {
            username = "<your-github-username>"
            password = "<your-personal-access-token>"
        }
    }
}
```

### Step 2: Add the SDK Dependency to `build.gradle`
```groovy
dependencies {
    implementation 'io.fastpix.data:mediaplayer:1.0.0'
}
```

---

## Basic Usage

Ensure Media Player is initialized properly:

### Kotlin Setup

```kotlin
import android.media.MediaPlayer
import io.fastpix.data.entity.*
import io.fastpix.data.exo.FastPixBaseMediaPlayer
import io.fastpix.data.request.CustomOptions

class VideoPlayerActivity : AppCompatActivity() {
    private val mediaPlayer: MediaPlayer? = null

    private lateinit var fastPixBaseMediaPlayer: FastPixBaseMediaPlayer
    private val surfaceView: SurfaceView? = null
    private val mediaController: android.widget.MediaController? = null
    private val videoUrl = "https://demo.com/mp4"
    private var videotitel = "Titel"
    private var videoid = "123"
    override fun onCreate(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_video_player)
        // setContentView(R.layout.your_layout) // don't forget this if needed 
        surfaceView = findViewById(R.id.surfaceView)
        val holder = surfaceView?.getHolder()

        surfaceView?.setOnTouchListener(OnTouchListener { v, event ->
            // handle the player options hide and show	 
        })

        holder?.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                try {
                    // Setup media player	 
                    mediaPlayer = MediaPlayer()
                    mediaPlayer!!.setScreenOnWhilePlaying(true)
                    mediaPlayer!!.setDataSource(videoUrl)
                    mediaPlayer!!.setDisplay(holder)

                    val customerPlayerDataEntity = CustomerPlayerDataEntity()
                    val customerVideoDataEntity = CustomerVideoDataEntity()
                    val customerViewDataEntity = CustomerViewDataEntity()

                    try { // //Get it from dashboard  Get it from dashboard  
                        customerPlayerDataEntity.workspaceKey = "123"
                        customerVideoDataEntity.videoSourceUrl = "videoUrl"
                        
                        customerViewDataEntity.viewSessionId = UUID.randomUUID().toString()
                        val customerDataEntity = CustomerDataEntity(
                            customerPlayerDataEntity,
                            customerVideoDataEntity,
                            customerViewDataEntity
                        )
                        val customOptions = CustomOptions()

                        val customDataFp = CustomDataEntity()
                        customDataFp.customData1 = "cd1"
                        customDataFp.customData2 = "cd2"
                                
                        customDataFp.customData10 = "cd10"

                        customerDataEntity.customData = customDataFp
                    } catch (e: JSONException) {
                        throw RuntimeException(e)
                    }

                    fastPixBaseMediaPlayer!!.setPlayerView(surfaceView)
                    mediaPlayer!!.prepareAsync()
                }

                catch (e: IOException){
                    e.printStackTrace()
                }
            }


            override fun surfaceDestroyed(holder: SurfaceHolder) {

                if (mediaPlayer != null) {
                    // destroy the media player 
                }
            }
        })

    }
}
```

### XML Layout
```xml
<SurfaceView
    android:id="@+id/surfaceView"
    android:layout_width="match_parent"
    android:layout_height="300dp"
    android:layout_alignParentTop="true" />
```

---

## Changing Video Sources
Call `fastPixBaseMediaPlayer.videoChange(CustomerVideoData)` when switching videos to reset analytics context:
```kotlin
val newVideoData = CustomerVideoDataEntity().apply {
    videoId = "newId"
    videoTitle = "New Video"
    videoSourceUrl = "newUrl"
}
fastPixBaseMediaPlayer.videoChange(newVideoData)
```

---

## Error Handling
To manually report handled errors:
```kotlin
fastPixBaseMediaPlayer.error(FastPixErrorException("Custom playback error"))
```
Disable automatic error tracking if needed:
```kotlin
fastPixBaseMediaPlayer.setAutomaticErrorTracking(false)
```

---

## Documentation
For advanced usage and APIs, refer to the [FastPix Developer Docs](https://docs.fastpix.io/docs/******).

