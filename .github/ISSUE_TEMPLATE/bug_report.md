---
name: Bug Report
about: Create a report to help us improve
title: '[BUG] '
labels: bug
assignees: ''
---

## Bug Description

A clear and concise description of what the bug is.

## Reproduction Steps

1. **Setup Environment**

```groovy
dependencies {
    implementation 'io.fastpix.data:mediaplayer:1.0.0'
}
```

2. **Code To Reproduce**

```kotlin
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
```

3. **Expected Behavior**
```
<!-- A clear and concise description of what you expected to happen.  -->
```

4. **Actual Behavior**
```
<!-- A clear and concise description of what actually happened. -->
```

5. **Environment**

- **SDK Version**: [e.g., 1.2.2]
- **Android Version**: [e.g., Android 12]
- **Min SDK Version**: [e.g., 24]
- **Target SDK Version**: [e.g., 35]
- **Device/Emulator**: [e.g., Pixel 5, Android Emulator]
- **Player**: [e.g., ExoPlayer 2.19.0, VideoView, etc.]
- **Kotlin Version**: [e.g., 2.0.21]

## Code Sample

```kotlin
// Please provide a minimal code sample that reproduces the issue
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
```

## Logs/Stack Trace

```
Paste relevant logs or stack traces here
```

## Additional Context

Add any other context about the problem here.

## Screenshots

If applicable, add screenshots to help explain your problem.

