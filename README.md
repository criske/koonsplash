![CircleCI](https://circleci.com/gh/criske/koonsplash.svg?style=svg) [![codecov.io](https://codecov.io/github/criske/koonsplash/coverage.svg?branch=master)](https://codecov.io/github/criske/koonsplash)
    
# **Koonsplash**

Unofficial client side Kotlin wrapper for Unsplash API. 

#### Usage

```kotlin
runBlocking {
    val api = Koonsplash.builder("my-access-key-client-id")
                .openLinksStrategy {
                    //if _openLinksStrategy_ is not set. Koonsplash will try to open browser 
                    //or photo links using os terminal.
                    Desptop.browse(it)
                    Result.success(Unit)
                }
                .build()
                .authenticated(
                    Koonsplash.AuthenticatedBuilder("secret-key".toCharArray())
                        .scopes(AuthScope.PUBLIC + AuthScope.READ_USER + AuthScope.WRITE_USER)
                )   
                .api
    val me = api.endpoint("/me").call()
    val myLikesLink: Link.Api = me["links"]["likes"]()
    val firstLikedPhoto = myLikesLink.call()[0]
    val link: Link.Download = firstLikedPhoto["links"]["download_location"]()
    val photo: Link.Browser = link.download(File("<path>"), id)
    // or with progress
    link
        .downloadWithProgress(File("<path>"), id)
        .collect { status ->
            when (status) {
                is ApiCall.Status.Canceled -> status.err.printStackTrace()
                is ApiCall.Status.Current  -> println("Progress: ${status.value}%")
                is ApiCall.Status.Done     -> status.resource.open()
                is ApiCall.Status.Starting -> println("Starting")
            }
        }
}        
```
**DSL Support for image resizing**
```kotlin
val photo: Link.Photo = firstLikedPhoto["urls"]["raw"]
val resizedPhoto = photo.resize{
    500u.w
    500u.h
    fit(Fit.CROP)
    crop(Crop.TOP, Crop.LEFT)
    100.q
    fm(Fm.PNG)
}.toDownloadLink()
val saved: Link.Browser = resizedPhoto.download(File("<path>"), "my-resized-photo")
saved.open()
```
**Cancelling a request**
```kotlin
val cancelButton = MutableSharedFlow<Unit>()
launch {
   val photo = api.call("/photos/random").cancelable(cancelFlow)
   if(photo != null){
     val link: Link.Download = photo["links"]["download_location"]()
     //...
   }else{
     //request was manually canceled
     showMessage("Canceled")
   }
}
launch {
   api.call("/photos/random")
        .cancelableExecute(cancelFlow)
        .collect{ status ->
            when (status) {
                is ApiCall.Status.Starting -> showProgressBar()
                is ApiCall.Status.Current -> updateProgressBar(status.value)
                is ApiCall.Status.Done -> {
                    hideProgressBar()
                    val photo = status.resource()
                    //....
                }
                is ApiCall.Status.Canceled -> {
                    hideProgressBar()
                    showMessage("Canceled")
                }           
            }
        }
}
//pressing the supposed button will cancel the request.
launch {
    delay(1000)
    cancelButton.emit(Unit)
}
```
**Work in progress...**
