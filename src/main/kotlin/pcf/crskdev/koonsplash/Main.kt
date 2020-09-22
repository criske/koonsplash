package pcf.crskdev.koonsplash

import pcf.crskdev.koonsplash.auth.AccessKey
import pcf.crskdev.koonsplash.auth.AuthCalls
import pcf.crskdev.koonsplash.auth.AuthScope
import pcf.crskdev.koonsplash.auth.AuthToken
import pcf.crskdev.koonsplash.auth.AuthTokenStorage
import pcf.crskdev.koonsplash.auth.AuthenticityToken
import pcf.crskdev.koonsplash.auth.AuthorizationCode
import pcf.crskdev.koonsplash.auth.Authorizer
import pcf.crskdev.koonsplash.auth.InvalidCredentials
import pcf.crskdev.koonsplash.auth.LoginFormController
import pcf.crskdev.koonsplash.auth.LoginForm
import pcf.crskdev.koonsplash.auth.NeedsLogin
import pcf.crskdev.koonsplash.auth.SecretKey
import java.net.URI
import java.util.concurrent.Executors

fun main() {

    val storage = object : AuthTokenStorage {
        override fun save(token: AuthToken) {
            println("Token is $token")
        }

        override fun load(): AuthToken? = null
    }
    val accessKey = System.getenv("access_key")
    val secretKey = System.getenv("secret_key")
    // val email = System.getenv("email")
    // val password = System.getenv("password")

    val authorizer = Authorizer(
        accessKey,
        secretKey,
        authCalls = DummyAuthCalls,
        storage = storage
    )

    val executor = Executors.newSingleThreadExecutor()

    val loginFormController = object : LoginFormController() {
        override fun activateForm() {
            println("**Enter email:")
            val email = readLine()!!
            println("**Enter password:")
            val password = readLine()!!
            this.submit(email, password)
        }
    }
    loginFormController.attachForm(object : LoginForm {
        override fun onSuccess() {
            println("Login successful")
            executor.shutdownNow()
        }

        override fun onFailure() {
            println("Login failure")
        }
    })

    authorizer.authorize(
        executor,
        loginFormController,
        onError = { println(it) },
        onSuccess = { println(it) }
    )
}

object DummyAuthCalls : AuthCalls {

    override fun authorize(
        accessKey: AccessKey,
        redirectUri: URI,
        vararg scopes: AuthScope
    ): Result<AuthorizationCode> = Result.failure(NeedsLogin("1234"))

    override fun loginForm(
        authenticityToken: AuthenticityToken,
        email: String,
        password: String
    ): Result<AuthorizationCode> {
        return if (email == "foo@gmail.com" && password == "123") {
            Result.success("123code")
        } else {
            Result.failure(InvalidCredentials)
        }
    }

    override fun token(
        authorizationCode: AuthorizationCode,
        accessKey: AccessKey,
        secretKey: SecretKey,
        redirectUri: URI
    ): Result<AuthToken> = Result.success(AuthToken("123", "bearer", "123r", 13))
}
