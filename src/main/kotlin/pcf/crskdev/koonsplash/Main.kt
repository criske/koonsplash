package pcf.crskdev.koonsplash

import kotlinx.coroutines.runBlocking
import pcf.crskdev.koonsplash.auth.AccessKey
import pcf.crskdev.koonsplash.auth.ApiKeysLoader
import pcf.crskdev.koonsplash.auth.AuthScope
import pcf.crskdev.koonsplash.auth.AuthToken
import pcf.crskdev.koonsplash.auth.AuthTokenStorage
import pcf.crskdev.koonsplash.auth.SecretKey
import pcf.crskdev.koonsplash.http.HttpClient
import pcf.crskdev.koonsplash.json.JsonClient

@ExperimentalStdlibApi
fun main() {

    val storage = object : AuthTokenStorage {
        override fun save(token: AuthToken) {
            println("Saved Token $token")
        }

        override fun load(): AuthToken? =
            AuthToken(
                System.getenv("access_token"), "bearer", "", System.currentTimeMillis()
            )

        override fun clear() {}
    }

    val keysLoader = object : ApiKeysLoader {
        override val accessKey: AccessKey = System.getenv("access_key")
        override val secretKey: SecretKey = System.getenv("secret_key")
    }

    runBlocking {
        val api = Koonsplash.builder(keysLoader, storage)
            .build()
            .authenticated(
                System.getenv("email"),
                System.getenv("password"),
                AuthScope.PUBLIC + AuthScope.READ_USER + AuthScope.WRITE_USER
            )
            .api
        val me: String = api.call("/me")()
        println(me.jsonPrettyPrint())
    }

    // https://github.com/square/okhttp/issues/4029
    // manually shutdown okhttp
    HttpClient.http.dispatcher.executorService.shutdown()
}

fun String.jsonPrettyPrint(): String {
    val fromJson = JsonClient.json.fromJson(this, Map::class.java)
    return JsonClient.json.toJson(fromJson)
}
