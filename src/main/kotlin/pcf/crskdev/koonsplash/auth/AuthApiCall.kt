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
import okhttp3.Request
import pcf.crskdev.koonsplash.http.HttpClient
import pcf.crskdev.koonsplash.http.HttpClient.executeCo
import pcf.crskdev.koonsplash.http.HttpClient.jsonBody
import java.net.URI

/**
 * OAuth2 Unsplash calls abstraction.
 *
 * @author Cristian Pela
 * @since 0.1
 */
internal interface AuthApiCall {

    /**
     * Request for authentication token.
     *
     * @param authorizationCode Authorization code.
     * @param accessKey API access key.
     * @param secretKey API private/secret key.
     * @param redirectUri A URI that handles creating a new installation-specific client_id (not used here) .
     * @return AuthToken on success.
     */
    suspend fun token(
        authorizationCode: AuthorizationCode,
        accessKey: AccessKey,
        secretKey: SecretKey,
        redirectUri: URI
    ): AuthToken
}

/**
 * Auth api call impl
 *
 * @constructor Create empty Auth api call impl
 */
internal class AuthApiCallImpl : AuthApiCall {

    override suspend fun token(
        authorizationCode: AuthorizationCode,
        accessKey: AccessKey,
        secretKey: SecretKey,
        redirectUri: URI
    ): AuthToken {
        val authTokenForm = FormBody.Builder()
            .add("client_id", accessKey)
            .add("client_secret", secretKey)
            .add("redirect_uri", redirectUri.toString())
            .add("code", authorizationCode)
            .add("grant_type", "authorization_code")
            .build()
        val request = Request.Builder()
            .url(
                HttpClient.baseUrl.toHttpUrlOrNull()!!
                    .newBuilder()
                    .addPathSegment("oauth")
                    .addPathSegment("token")
                    .build()
            )
            .cacheControl(CacheControl.FORCE_NETWORK)
            .post(authTokenForm)
            .build()

        return HttpClient.http.newCall(request).executeCo().jsonBody()
    }
}
