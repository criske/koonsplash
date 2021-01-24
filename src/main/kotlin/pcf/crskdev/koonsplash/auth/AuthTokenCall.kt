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

import pcf.crskdev.koonsplash.api.ApiCall
import pcf.crskdev.koonsplash.api.ApiCallImpl
import pcf.crskdev.koonsplash.api.Endpoint
import pcf.crskdev.koonsplash.api.Verb
import pcf.crskdev.koonsplash.http.HttpClient
import pcf.crskdev.koonsplash.internal.KoonsplashContext
import java.net.URI

/**
 * OAuth2 Unsplash create token abstraction.
 *
 * @author Cristian Pela
 * @since 0.1
 */
internal interface AuthTokenCall {

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
internal class AuthTokenCallImpl : AuthTokenCall {

    override suspend fun token(
        authorizationCode: AuthorizationCode,
        accessKey: AccessKey,
        secretKey: SecretKey,
        redirectUri: URI
    ): AuthToken {

        val apiCall: ApiCall = ApiCallImpl(
            Endpoint(
                HttpClient.baseUrl.toString(),
                "/oauth/token",
                Verb.Modify.Post(
                    "client_id" to accessKey,
                    "client_secret" to secretKey.concatToString(),
                    "redirect_uri" to redirectUri.toString(),
                    "code" to authorizationCode,
                    "grant_type" to "authorization_code"
                )
            ),
            HttpClient.http,
            KoonsplashContext.Builder()
                .auth { AuthContext.None(accessKey) }
                .build()
        )

        return apiCall().let {
            AuthToken(
                it["access_token"](),
                it["token_type"](),
                it["refresh_token"](),
                AuthScope.decode(it["scope"]()),
                it["created_at"]()
            )
        }
    }
}
