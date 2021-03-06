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

import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import kotlin.time.toDuration

/**
 * Authorizer for authenticated API requests.
 *
 * @author Cristian Pela
 * @since 0.1
 */
@ExperimentalTime
@ExperimentalUnsignedTypes
interface Authorizer {

    /**
     * OAuth2 authorization flow done in background thread provided by executor.
     * unless there is an AuthToken saved in storage.
     *
     * @param accessKey Access key (Client id).
     * @param secretKey Secret key.
     * @param scopes Scopes.
     * @param host Host.
     * @param port Int.
     * @param timeout Amount of time within the authorization must execute. Default 5 minutes.
     * @receiver
     * @return AuthToken
     */
    suspend fun authorize(
        accessKey: AccessKey,
        secretKey: SecretKey,
        scopes: AuthScope,
        host: String = AuthCodeServer.DEFAULT_HOST,
        port: UInt = AuthCodeServer.DEFAULT_PORT,
        timeout: Duration = 5.toDuration(DurationUnit.MINUTES)
    ): AuthToken
}
