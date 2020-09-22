package pcf.crskdev.koonsplash

import pcf.crskdev.koonsplash.auth.AuthToken
import pcf.crskdev.koonsplash.auth.AuthTokenStorage
import pcf.crskdev.koonsplash.auth.Authorizer

fun main() {
    val storage = object : AuthTokenStorage {
        override fun save(token: AuthToken) {
            TODO("Not yet implemented")
        }

        override fun load(): AuthToken? {
            TODO("Not yet implemented")
        }
    }
    val accessKey = System.getenv("access_key")
    val secretKey = System.getenv("secret_key")
    val email = System.getenv("email")
    val password = System.getenv("password")
    val authorizer = Authorizer(accessKey, secretKey, storage = storage)

    authorizer.authorize(
        email,
        password,
        onError = {
            println(it)
        },
        onSuccess = {
            println(it)
        }
    )
}
