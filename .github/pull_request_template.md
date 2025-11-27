# FastPix Resumable Uploads SDK - Documentation PR

## Documentation Changes

### What Changed
- [ ] New documentation added
- [ ] Existing documentation updated
- [ ] Documentation errors fixed
- [ ] Code examples updated
- [ ] Links and references updated

### Files Modified
- [ ] README.md
- [ ] docs/ files
- [ ] USAGE.md
- [ ] CONTRIBUTING.md
- [ ] Other: _______________

### Summary
**Brief description of changes:**

<!-- What documentation was added, updated, or fixed? -->

### Code Examples
```kotlin 
// If you added/updated code examples, include them here
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

### Testing
- [ ] All code examples tested
- [ ] Links verified
- [ ] Grammar checked
- [ ] Formatting consistent

### Review Checklist
- [ ] Content is accurate
- [ ] Code examples work
- [ ] Links are working
- [ ] Grammar is correct
- [ ] Formatting is consistent

---

**Ready for review!**
