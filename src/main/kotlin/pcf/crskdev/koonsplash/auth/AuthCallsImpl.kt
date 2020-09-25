/*
 *  Copyright (c) 2020. Pela Cristian
 *  Permission is hereby granted, free of charge, to any person obtaining a
 *  copy of this software and associated documentation files (the "Software"),
 *  to deal in the Software without restriction, including without limitation
 *  the rights to use, copy, modify, merge, publish, distribute, sublicense,
 *  and/or sell copies of the Software, and to permit persons to whom the
 *  Software is furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 *  OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 *  FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 *  DEALINGS IN THE SOFTWARE.
 */

package pcf.crskdev.koonsplash.auth

import com.google.gson.Gson
import okhttp3.CacheControl
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.IOException
import java.net.URI

/**
 * AuthCalls implementation.
 *
 * @author Cristian Pela
 * @since 0.1
 */
internal class AuthCallsImpl(
    private val httpClient: OkHttpClient,
    private val json: Gson
) : AuthCalls {

    private val baseAuthHttpUrl = HttpUrl.Builder()
        .host("unsplash.com")
        .scheme("https")
        .addPathSegment("oauth")
        .build()

    override fun authorize(
        accessKey: AccessKey,
        redirectUri: URI,
        vararg scopes: AuthScope
    ): Result<AuthorizationCode> {
        val url = baseAuthHttpUrl
            .newBuilder()
            .addPathSegment("authorize")
            .addQueryParameter("client_id", accessKey)
            .addEncodedQueryParameter("redirect_uri", redirectUri.toString())
            .addQueryParameter("response_type", "code")
            .addEncodedQueryParameter("scope", scopes.joinToString("+") { it.value })
            .build()
        val request = Request.Builder()
            .cacheControl(CacheControl.FORCE_NETWORK)
            .url(url)
            .build()
        val response = httpClient.newCall(request).execute()

        return tryResult(response) {
            val document = Jsoup.parse(response.body?.string())
            val authenticityToken = this.extractAuthenticityToken(document)
            this.extractAuthorizationCode(document)
                ?.let { Result.success(it) }
                ?: Result.failure(NeedsLogin(authenticityToken))
        }
    }

    override fun loginForm(
        authenticityToken: AuthenticityToken,
        email: String,
        password: String
    ): Result<AuthorizationCode> {
        val form = FormBody.Builder()
            .add("utf8", "&#x2713;")
            .add("authenticity_token", authenticityToken)
            .add("user[email]", email)
            .add("user[password]", password)
            .build()
        val request = Request.Builder()
            .url(baseAuthHttpUrl.newBuilder().addPathSegment("login").build())
            .cacheControl(CacheControl.FORCE_NETWORK)
            .post(form)
            .build()
        val response = httpClient.newCall(request).execute()
        return tryResult(response) {
            val document = Jsoup.parse(response.body?.string())
            this.extractAuthorizationCode(document)
                ?.let { Result.success(it) }
                ?: Result.failure(InvalidCredentials)
        }
    }

    override fun token(
        authorizationCode: AuthorizationCode,
        accessKey: AccessKey,
        secretKey: SecretKey,
        redirectUri: URI
    ): Result<AuthToken> {
        val authTokenForm = FormBody.Builder()
            .add("client_id", accessKey)
            .add("client_secret", secretKey)
            .add("redirect_uri", redirectUri.toString())
            .add("code", authorizationCode)
            .add("grant_type", "authorization_code")
            .build()
        val request = Request.Builder()
            .url(baseAuthHttpUrl.newBuilder().addPathSegment("token").build())
            .cacheControl(CacheControl.FORCE_NETWORK)
            .post(authTokenForm)
            .build()
        val response = httpClient.newCall(request).execute()
        return tryResult(response) {
            val bodyString = response.body?.string() ?: ""
            val authToken = json.fromJson(bodyString, AuthToken::class.java)
            Result.success(authToken)
        }
    }

    /**
     * Try extract authorization code or null.
     *
     * @param doc Document.
     * @return AuthorizationCode?
     */
    private fun extractAuthorizationCode(doc: Document): AuthorizationCode? {
        return doc.selectFirst("code")?.text()
    }

    /**
     * Extract authenticity token.
     *
     * Note: might be empty
     *
     * @param doc Document.
     * @return AuthenticityToken
     */
    private fun extractAuthenticityToken(doc: Document): AuthenticityToken {
        return doc.selectFirst("input[name=authenticity_token]")
            .attr("value")
    }

    /**
     * Try result helper.
     *
     * @param T type for result success.
     * @param response Http response.
     * @param block Code block on response success
     * @receiver Returns result
     * @return Result
     */
    private inline fun <T> tryResult(response: Response, block: () -> Result<T>): Result<T> =
        try {
            if (response.isSuccessful) {
                block()
            } else {
                Result.failure(IllegalStateException(response.body?.string()))
            }
        } catch (ex: IOException) {
            Result.failure(ex)
        }
}
