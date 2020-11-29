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

import fi.iki.elonen.NanoHTTPD
import java.net.URI
import java.util.concurrent.TimeUnit

/**
 * Authorization code server implementation.
 *
 * @author Cristian Pela
 * @since 0.1
 */
internal class AuthCodeServerImpl(override val callbackUri: URI) :
    NanoHTTPD(callbackUri.host, callbackUri.port), AuthCodeServer {

    @Volatile
    private var onAuthorizeCode: (AuthorizationCode) -> Unit = {}

    override fun serve(session: IHTTPSession): Response {
        val code = session.parms["code"]
        if (code != null)
            this.onAuthorizeCode(code)
        return newFixedLengthResponse(
            "<!DOCTYPE html><html><head><title>Code</title></head><body><code>$code</code></body></html>"
        )
    }

    override fun startServing(timeoutSec: Int): Boolean {
        start()
        var timeout = 0
        while (!isAlive) {
            Thread.sleep(10)
            timeout += 1
            if (timeout >= TimeUnit.SECONDS.toMillis(30)) {
                return false
            }
        }
        return true
    }

    override fun stopServing() {
        stop()
        this.onAuthorizeCode = {}
    }

    override fun onAuthorizeCode(block: (AuthorizationCode) -> Unit) {
        this.onAuthorizeCode = block
    }
}
