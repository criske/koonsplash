package pcf.crskdev.koonsplash

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import pcf.crskdev.koonsplash.api.ApiCall
import pcf.crskdev.koonsplash.auth.AccessKey
import pcf.crskdev.koonsplash.auth.ApiKeysLoader
import pcf.crskdev.koonsplash.auth.AuthScope
import pcf.crskdev.koonsplash.auth.AuthToken
import pcf.crskdev.koonsplash.auth.AuthTokenStorage
import pcf.crskdev.koonsplash.auth.SecretKey
import pcf.crskdev.koonsplash.http.HttpClient
import kotlin.coroutines.EmptyCoroutineContext

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

            api.call("/me")
                .execute(emptyList(), ApiCall.Progress.Percent)
                .collect {
                    when (it) {
                        is ApiCall.ProgressStatus.Canceled -> println("Canceled")
                        is ApiCall.ProgressStatus.Current -> println("Current: ${it.value}")
                        is ApiCall.ProgressStatus.Done -> println("Done: ${it.resource["username"]}")
                        is ApiCall.ProgressStatus.Starting -> println("Starting")
                    }
                }

//            val myLikesLink: Link.Api = me["links"]["likes"]()
//            val firstLikedPhoto = myLikesLink.call()[0]
//            val browserLink: Link.Browser = firstLikedPhoto["links"]["html"]()
//            browserLink.open {
//                launch(Dispatchers.Default) {
//                    Desktop.getDesktop().browse(URI.create(it))
//                }
//            }
        }.join()
    }

    HttpClient.http.dispatcher.executorService.shutdown()
    println("Bye")
}
