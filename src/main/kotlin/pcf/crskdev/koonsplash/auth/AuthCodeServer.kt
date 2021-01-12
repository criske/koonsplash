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

import okhttp3.HttpUrl
import java.net.URI

/**
 * Authorization code server.
 *
 * @author Cristian Pela
 * @since 0.1
 */
interface AuthCodeServer {

    companion object {

        /**
         * Default port.
         */
        const val DEFAULT_PORT = 3000u

        /**
         * Default host
         */
        const val DEFAULT_HOST = "localhost"

        /**
         * Callback builder
         *
         * @param host Host.
         * @param port Port.
         * @return URI
         */
        @ExperimentalUnsignedTypes
        fun createCallback(host: String, port: UInt): URI =
            HttpUrl.Builder().scheme("http").host(host).port(port.toInt()).build().toUri()
    }

    /**
     * Callback uri
     */
    val callbackUri: URI

    /**
     * Start serving within the given timeout.
     *
     * @param timeoutSec Timeout in seconds.
     * @return True is server started.
     */
    fun startServing(timeoutSec: Int = 30): Boolean

    /**
     * Stop serving
     *
     */
    fun stopServing()

    /**
     * Called when authorization code is available.
     *
     * @param block
     * @receiver Contains the result with the code.
     */
    fun onAuthorizeCode(block: (AuthorizationCode) -> Unit)
}
