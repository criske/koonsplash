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

import okhttp3.CacheControl
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import pcf.crskdev.koonsplash.http.HttpClient
import pcf.crskdev.koonsplash.http.HttpClient.jsonBody
import java.io.IOException
import java.net.URI

/**
 * AuthCalls implementation.
 *
 * @author Cristian Pela
 * @since 0.1
 * @param httpClient Http client
 */
internal class AuthCallsImpl(
    private val httpClient: OkHttpClient,
) : AuthCalls {

    private val baseAuthHttpUrl = HttpClient.baseUrl.toHttpUrlOrNull()!!
        .newBuilder()
        .addPathSegment("oauth")
        .build()

    override fun authorize(
        accessKey: AccessKey,
        redirectUri: URI,
        scopes: AuthScope
    ): Result<AuthorizationCode> {
        val url = baseAuthHttpUrl
            .newBuilder()
            .addPathSegment("authorize")
            .addQueryParameter("client_id", accessKey)
            .addEncodedQueryParameter("redirect_uri", redirectUri.toString())
            .addQueryParameter("response_type", "code")
            .addEncodedQueryParameter("scope", scopes.value)
            .build()
        val request = Request.Builder()
            .cacheControl(CacheControl.FORCE_NETWORK)
            .url(url)
            .build()

        return tryResult({ httpClient.newCall(request).execute() }) { response ->
            val document = Jsoup.parse(response.body?.string())
            val authenticityToken = this.extractAuthenticityToken(document)
            this.extractAuthorizationCode(document)
                ?.let { Result.success(it) }
                ?: authenticityToken?.let {
                    Result.failure(NeedsLoginException(it))
                } ?: Result.failure(IllegalStateException("AuthenticityToken not found"))
        }
    }

    override fun authorizeForm(authorizeForm: AuthorizeForm): Result<AuthorizationCode> {
        val form = FormBody.Builder()
            .apply {
                authorizeForm.forEach {
                    this.add(it.first, it.second)
                }
            }
            .build()
        val request = Request.Builder()
            .url(baseAuthHttpUrl.newBuilder().addPathSegment("authorize").build())
            .cacheControl(CacheControl.FORCE_NETWORK)
            .post(form)
            .build()

        return tryResult({ httpClient.newCall(request).execute() }) { response ->
            val document = Jsoup.parse(response.body?.string())
            this.extractAuthorizationCode(document)
                ?.let { Result.success(it) }
                ?: Result.failure(
                    ConfirmAuthorizeException(
                        "Authorize form was submitted but authorization code was not received"
                    )
                )
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

        return tryResult({ httpClient.newCall(request).execute() }) { response ->
            val document = Jsoup.parse(response.body?.string())
            this.extractAuthorizationCode(document)
                ?.let { Result.success(it) }
                ?: this.extractConfirmAuthorizationForm(document)
                    ?.let { form ->
                        Result.failure(NeedsConfirmAuthorizeFormException(form))
                    }
                ?: Result.failure(InvalidCredentialsException)
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

        return tryResult({ httpClient.newCall(request).execute() }) { response ->
            Result.success(response.jsonBody())
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
    private fun extractAuthenticityToken(doc: Document): AuthenticityToken? {
        return doc.extractInputValue("authenticity_token")
    }

    /**
     * Extract confirm authorize form.
     *
     * @param doc Document
     * @return AuthorizeForm or null
     */
    private fun extractConfirmAuthorizationForm(doc: Document): AuthorizeForm? {
        return doc
            .selectFirst("body > div.container.authorization-container > div > div > div > div > div:nth-child(3)")
            ?.let {
                it.selectFirst("form")?.let { form ->
                    val utf8 = form.extractInputValue("utf8")
                    requireNotNull(utf8)
                    val authenticityToken = form.extractInputValue("authenticity_token")
                    requireNotNull(authenticityToken)
                    val clientId = form.extractInputValue("client_id")
                    requireNotNull(clientId)
                    val redirectUri = form.extractInputValue("redirect_uri")
                    requireNotNull(redirectUri)
                    val state = form.extractInputValue("state")
                    requireNotNull(state)
                    val responseType = form.extractInputValue("response_type")
                    requireNotNull(responseType)
                    val scope = form.extractInputValue("scope")
                    requireNotNull(scope)
                    listOf(
                        "utf8" to utf8,
                        "authenticity_token" to authenticityToken,
                        "client_id" to clientId,
                        "redirect_uri" to redirectUri,
                        "state" to state,
                        "response_type" to responseType,
                        "scope" to scope
                    )
                }
            }
    }

    /**
     * Extract input value from a parent.
     *
     * @param parent Element
     * @param name Name
     * @return Value or null
     */
    private fun Element.extractInputValue(name: String): String? =
        this.selectFirst("input[name=$name]")?.attr("value")

    /**
     * Try result helper.
     *
     * @param T type for result success.
     * @param responseBlock Http response block.
     * @param success Code block on response success
     * @receiver Returns result
     * @return Result
     */
    private fun <T> tryResult(responseBlock: () -> Response, success: (Response) -> Result<T>): Result<T> =
        try {
            val response = responseBlock()
            if (response.isSuccessful) {
                success(response)
            } else {
                Result.failure(IllegalStateException(response.body?.string()))
            }
        } catch (ex: IOException) {
            Result.failure(ex)
        }
}
