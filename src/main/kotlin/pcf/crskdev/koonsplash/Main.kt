package pcf.crskdev.koonsplash

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import pcf.crskdev.koonsplash.api.Link
import pcf.crskdev.koonsplash.api.downloadToPhoto
import pcf.crskdev.koonsplash.auth.AccessKey
import pcf.crskdev.koonsplash.auth.ApiKeysLoader
import pcf.crskdev.koonsplash.auth.AuthScope
import pcf.crskdev.koonsplash.auth.AuthToken
import pcf.crskdev.koonsplash.auth.AuthTokenStorage
import pcf.crskdev.koonsplash.auth.SecretKey
import pcf.crskdev.koonsplash.http.HttpClient
import java.awt.Desktop
import java.io.File
import kotlin.coroutines.EmptyCoroutineContext

@ExperimentalCoroutinesApi
@FlowPreview
@ExperimentalStdlibApi
fun main() {

    val storage = object : AuthTokenStorage {
        override fun save(token: AuthToken) {
            println("Saved Token $token")
        }

        override fun load(): AuthToken? =
            AuthToken(
                System.getenv("access_token"),
                "bearer",
                "",
                System.currentTimeMillis()
            )

        override fun clear() {}
    }

    val keysLoader = object : ApiKeysLoader {
        override val accessKey: AccessKey = System.getenv("access_key")
        override val secretKey: SecretKey = System.getenv("secret_key")
    }

    val scope = CoroutineScope(EmptyCoroutineContext)
    runBlocking {
        val api = Koonsplash.builder(keysLoader, storage)
            .build()
            .authenticated(
                System.getenv("email"),
                System.getenv("password"),
                AuthScope.PUBLIC + AuthScope.READ_USER + AuthScope.WRITE_USER
            )
            .api
        scope.launch {

            val me = api.call("/me")()

            val myLikesLink: Link.Api = me["links"]["likes"]()
            val firstLikedPhoto = myLikesLink.call()[1]
            val downloadLink: Link.Download = firstLikedPhoto["links"]["download_location"]()

            val id: String = firstLikedPhoto["id"]()
//            downloadLink
//                .download(File("C:\\Users\\user\\Desktop"), id)
//                .collect { status ->
//                    when (status) {
//                        is ApiCall.ProgressStatus.Canceled -> status.err.printStackTrace()
//                        is ApiCall.ProgressStatus.Current -> println("Current: ${status.value}%")
//                        is ApiCall.ProgressStatus.Done -> println("Done downloading")
//                        is ApiCall.ProgressStatus.Starting -> println("Starting")
//                    }
//                }

            val photo = downloadLink
                .downloadToPhoto(File("C:\\Users\\user\\Desktop"), id)
            Desktop.getDesktop().browse(photo.url)
        }.join()
    }

    HttpClient.http.dispatcher.executorService.shutdown()
    println("Bye")
}
