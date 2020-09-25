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

import java.net.URI

/**
 * OAuth2 Unsplash calls abstraction.
 *
 * @author Cristian Pela
 * @since 0.1
 */
interface AuthCalls {

    /**
     * Request authorization code.
     *
     * If _NeedsLogin_ is thrown one must use _loginForm_ with
     * _authenticityToken(csrf-token)_ that comes with the exception.
     *
     * @param accessKey API accessKey.
     * @param redirectUri Redirect Uri for authorization code.
     * @param scopes Scopes see AuthScope.
     * @return AuthorizationCode result.
     */
    fun authorize(accessKey: AccessKey, redirectUri: URI, vararg scopes: AuthScope): Result<AuthorizationCode>

    /**
     * Intermediary step login form.
     *
     * On successfully login, then one must use the _authorizeForm_ with _AuthenticityToken_ from result.
     *
     * @param authenticityToken Csrf-token vouching for the form.
     * @param email User's email address.
     * @param password User's password.
     * @return AuthorizationCode result on success.
     */
    fun loginForm(authenticityToken: AuthenticityToken, email: String, password: String): Result<AuthorizationCode>

    /**
     * Final request for authentication token.
     *
     * @param authorizationCode Authorization code.
     * @param accessKey API access key.
     * @param secretKey API private/secret key.
     * @param redirectUri A URI that handles creating a new installation-specific client_id (not used here) .
     * @return AuthToken on success.
     */
    fun token(
        authorizationCode: AuthorizationCode,
        accessKey: AccessKey,
        secretKey: SecretKey,
        redirectUri: URI
    ): Result<AuthToken>
}
