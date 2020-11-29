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
import java.util.concurrent.Executor

/**
 * Authorizer for authenticated API requests.
 *
 * @author Cristian Pela
 * @since 0.1
 */
interface Authorizer {

    /**
     * OAuth2 authorization flow done in background thread provided by executor.
     * unless there is an AuthToken saved in storage.
     *
     * @param executor Background thread.
     * @param loginFormController Login controller activates the login form if needed.
     * @param scopes Scopes
     * @param onError Failed authorizing callback.
     * @param onSuccess Success authorizing callback.
     * @receiver onError receives the error message.
     * @receiver onSuccess receives the AuthToken.
     */
    @Deprecated("Use the browser based authorize flavor")
    fun authorize(
        executor: Executor,
        loginFormController: LoginFormController,
        scopes: AuthScope,
        onError: (Throwable) -> Unit,
        onSuccess: (AuthToken) -> Unit
    )

    /**
     * OAuth2 authorization flow done in background thread provided by executor.
     * unless there is an AuthToken saved in storage.
     *
     * @param accessKey Access key (Client id).
     * @param secretKey Secret key.
     * @param scopes Scopes
     * @param server AuthCodeServer
     * @param browserLauncher Lambda that launches the os browser app.
     * @receiver
     * @return AuthToken
     */
    suspend fun authorize(
        accessKey: AccessKey,
        secretKey: SecretKey,
        scopes: AuthScope,
        server: AuthCodeServer,
        browserLauncher: (URI) -> Unit
    ): AuthToken
}
