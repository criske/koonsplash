package pcf.crskdev.koonsplash

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.runBlocking
import pcf.crskdev.koonsplash.api.Link
import pcf.crskdev.koonsplash.api.download
import pcf.crskdev.koonsplash.api.resize.ResizeDSL.Scope.Fm
import pcf.crskdev.koonsplash.api.resize.safeCrop
import pcf.crskdev.koonsplash.auth.AuthScope
import pcf.crskdev.koonsplash.auth.AuthToken
import pcf.crskdev.koonsplash.auth.AuthTokenStorage
import pcf.crskdev.koonsplash.http.HttpClient
import java.io.File
import kotlin.time.ExperimentalTime

@ExperimentalTime
@ExperimentalUnsignedTypes
@ExperimentalCoroutinesApi
@FlowPreview
@ExperimentalStdlibApi
fun main() {

    val storage = object : AuthTokenStorage {
        override fun save(token: AuthToken) {
            println("Saved Token $token")
        }

        override fun load(): AuthToken? = null
//            AuthToken(
//                System.getenv("access_token"),
//                "bearer",
//                "",
//                 AuthScope.PUBLIC + AuthScope.READ_USER + AuthScope.WRITE_USER,
//                100
//            )

        override fun clear() {}
    }

    runBlocking {
        val api = Koonsplash.builder(System.getenv("access_key"))
            .authTokenStorage(storage)
            .build()
            .authenticated(
                Koonsplash
                    .AuthenticatedBuilder(System.getenv("secret_key").toCharArray())
                    .scopes(AuthScope.PUBLIC + AuthScope.READ_USER + AuthScope.WRITE_USER)
            )
            .api

        val me = api.call("/me")()
        val myLikesLink: Link.Api = me["links"]["likes"]()
        val firstLikedPhoto = myLikesLink.call()[0]
        val id: String = firstLikedPhoto["id"]()
//        val downloadLink: Link.Download = firstLikedPhoto["links"]["download_location"]()

//            downloadLink
//                .downloadWithProgress(File("C:\\Users\\user\\Desktop"), id)
//                .collect { status ->
//                    when (status) {
//                        is ApiCall.ProgressStatus.Canceled -> status.err.printStackTrace()
//                        is ApiCall.ProgressStatus.Current -> println("Current: ${status.value}%")
//                        is ApiCall.ProgressStatus.Done -> println("Done downloading")
//                        is ApiCall.ProgressStatus.Starting -> println("Starting")
//                    }
//                }
        val rawPhoto: Link.Photo = firstLikedPhoto["urls"]["raw"]()
        val filter = rawPhoto.resize {
            fm(Fm.JPG)
            safeCrop(500u)
            80u.q
        }
        // Desktop.getDesktop().browse(filter.asPhotoLink().url)
        val saved = filter.asDownloadLink().download(File("C:\\Users\\user\\Desktop"), id)
        saved.open()
    }

    HttpClient.http.dispatcher.executorService.shutdown()
    println("Bye")
}
