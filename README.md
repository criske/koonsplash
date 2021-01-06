![CircleCI](https://circleci.com/gh/criske/koonsplash.svg?style=svg) [![codecov.io](https://codecov.io/github/criske/koonsplash/coverage.svg?branch=master)](https://codecov.io/github/criske/koonsplash)
    
# **Koonsplash**

Unofficial client side Kotlin wrapper for Unsplash API. 

#### Usage

```kotlin
runBlocking {
    val api = Koonsplash.builder(keysLoader, storage)
                .build()
                .authenticated(AuthScope.PUBLIC + AuthScope.READ_USER + AuthScope.WRITE_USER){
                    Desktop.browse(it) // launching the browser depends on platform
                }    
                .api
    val me = api.call("/me")()
    val myLikesLink: Link.Api = me["links"]["likes"]()
    val firstLikedPhoto = myLikesLink.call()[0]
    val downloadLink: Link.Download = firstLikedPhoto["links"]["download_location"]()
    val photo = downloadLink.downloadToPhoto(File("<path>"), id)
    // or with progress
    downloadLink
        .downloadWithProgress(File("<path>"), id)
        .collect { status ->
            when (status) {
                is ApiCall.ProgressStatus.Canceled -> status.err.printStackTrace()
                is ApiCall.ProgressStatus.Current -> println("Current: ${status.value}%")
                is ApiCall.ProgressStatus.Done -> println("Done downloading")
                is ApiCall.ProgressStatus.Starting -> println("Starting")
            }
        }
}        
```
**DSL Support for image resizing**
```kotlin
val photo: Link.Photo = firstLikedPhoto["urls"]["raw"]
val resizedPhoto = photo.filter{
    500u.w
    500u.h
    fit(Fit.CROP)
    crop(Crop.TOP, Crop.LEFT)
    100.q
    fm(Fm.PNG)
}.toDownloadLink()
resizedPhoto.download(File("<path>"), "my-resized-photo")
``

**Work in progress...**
