# **Koonsplash**

Unofficial client side Kotlin wrapper for Unsplash API. 

#### Usage

```kotlin
runBlocking {
    val api = Koonsplash.builder(keysLoader, storage)
                .build()
                .authenticated(
                    System.getenv("email"),
                    System.getenv("password"),
                    AuthScope.PUBLIC + AuthScope.READ_USER + AuthScope.WRITE_USER
                )
                .api
    val me = api.call("/me")()
    val myLikesLink: Link.Api = me["links"]["likes"]()
    val firstLikedPhoto = myLikesLink.call()[0]
    val downloadLink: Link.Download = firstLikedPhoto["links"]["download_location"]()
    val photo = downloadLink.downloadToPhoto(File("<path>"), id)
    // or with progress
    downloadLink
        .download(File("<path>"), id)
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
see `Main.kt` for more

**Work in progress...**
