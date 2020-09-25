package pcf.crskdev.koonsplash

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import pcf.crskdev.koonsplash.auth.AccessKey
import pcf.crskdev.koonsplash.auth.ApiKeysLoader
import pcf.crskdev.koonsplash.auth.AuthCalls
import pcf.crskdev.koonsplash.auth.AuthScope
import pcf.crskdev.koonsplash.auth.AuthToken
import pcf.crskdev.koonsplash.auth.AuthTokenStorage
import pcf.crskdev.koonsplash.auth.AuthenticityToken
import pcf.crskdev.koonsplash.auth.AuthorizationCode
import pcf.crskdev.koonsplash.auth.InvalidCredentialsException
import pcf.crskdev.koonsplash.auth.NeedsLoginException
import pcf.crskdev.koonsplash.auth.SecretKey
import pcf.crskdev.koonsplash.http.HttpClient
import pcf.crskdev.koonsplash.internal.KoonsplashImpl
import java.net.URI

@ExperimentalStdlibApi
fun main() {

    val storage = object : AuthTokenStorage {
        override fun save(token: AuthToken) {
            println("Saved Token $token")
        }

        override fun load(): AuthToken? = null
        override fun clear() {}
    }

    val keysLoader = object : ApiKeysLoader {
        override val accessKey: AccessKey = System.getenv("access_key")
        override val secretKey: SecretKey = System.getenv("secret_key")
    }

    runBlocking {
        withContext(Dispatchers.IO) {
            KoonsplashImpl(keysLoader, storage, HttpClient.http)
                .authenticated(
                    System.getenv("email"),
                    System.getenv("password")
                )
        }
    }
}

object DummyAuthCalls : AuthCalls {

    override fun authorize(
        accessKey: AccessKey,
        redirectUri: URI,
        vararg scopes: AuthScope
    ): Result<AuthorizationCode> = Result.failure(NeedsLoginException("1234"))

    override fun loginForm(
        authenticityToken: AuthenticityToken,
        email: String,
        password: String
    ): Result<AuthorizationCode> {
        return if (email == "foo@gmail.com" && password == "123") {
            Result.success("123code")
        } else {
            Result.failure(InvalidCredentialsException)
        }
    }

    override fun token(
        authorizationCode: AuthorizationCode,
        accessKey: AccessKey,
        secretKey: SecretKey,
        redirectUri: URI
    ): Result<AuthToken> = Result.success(AuthToken("123", "bearer", "123r", 13))
}
