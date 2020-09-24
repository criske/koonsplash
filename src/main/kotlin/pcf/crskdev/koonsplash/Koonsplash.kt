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

package pcf.crskdev.koonsplash

import kotlinx.coroutines.coroutineScope
import pcf.crskdev.koonsplash.api.Api
import pcf.crskdev.koonsplash.api.ApiAuth
import pcf.crskdev.koonsplash.auth.LoginFormController
import pcf.crskdev.koonsplash.auth.OneShotLoginFormController

/**
 * Koonsplash entry point.
 *
 * @author Cristian Pela
 * @since 0.1
 */
interface Koonsplash {

    /**
     * Api
     */
    val api: Api

    /**
     * Tries to authenticate by interacting with a [LoginFormController].
     *
     * @param controller Controller
     * @return [Koonsplash.Auth]
     */
    suspend fun authenticated(controller: LoginFormController): Auth

    /**
     * One shot authentication.
     *
     * @param email Email
     * @param password Password
     * @return [Koonsplash.Auth]
     */
    suspend fun authenticated(email: String, password: String): Auth = coroutineScope {
        authenticated(OneShotLoginFormController(email, password))
    }

    /**
     * Koonsplash entry point for authenticated requests.
     *
     */
    interface Auth {

        val api: ApiAuth

        suspend fun signOut(): Koonsplash
    }
}
