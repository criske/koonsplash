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
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import pcf.crskdev.koonsplash.auth.AuthCodeServer.Companion.DEFAULT_PORT
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.net.URI
import java.util.concurrent.TimeUnit

/**
 * Authorization code server implementation.
 *
 * @author Cristian Pela
 * @since 0.1
 */
@ExperimentalUnsignedTypes
internal class AuthCodeServerImpl(
    private val callback: URI = AuthCodeServer.createCallback(
        AuthCodeServer.DEFAULT_HOST,
        AuthCodeServer.DEFAULT_PORT
    ),
    private val port: UInt? = null
) :
    NanoHTTPD(
        callback.host,
        port?.toInt() ?: callback.port.takeIf { it > 0 } ?: DEFAULT_PORT.toInt()
    ),
    AuthCodeServer {

    @Volatile
    private var onAuthorizeCode: (AuthorizationCode) -> Unit = {}

    override val callbackUri: URI
        get() {
            val uriWithPort: (UInt) -> URI = {
                this.callback.toHttpUrlOrNull()
                    ?.newBuilder()
                    ?.port(it.toInt())
                    ?.build()
                    ?.toUri()
                    ?: throw IllegalStateException("Invalid auth code server callback")
            }
            return when {
                callback.port < 0 -> uriWithPort(DEFAULT_PORT)
                this.port != null -> uriWithPort(this.port)
                else -> this.callback
            }
        }

    override fun serve(session: IHTTPSession): Response {
        val code = session.parms["code"]!!
        val response =
            """
            <!DOCTYPE html>
                <html>
                    <head>
                        <title>Koonsplash</title>
                        <style>
                            body {
                                display: flex;
                                flex-direction: column;
                                align-items: center;
                                justify-content: center;
                                background-color: rgb(19, 18, 18);
                                color:rgb(248, 237, 215);
                                height: 100vh;
                            }
                            h1 {
                                font-size: 3em;
                            }
                            h5 {
                                font-size: 1.5em;
                            }
                        </style>
                    </head>
                    <body>
                        <h1>Koonsplash</h1>
                        <h5>Authorization code obtained. It is safe to close this window or tab!</h5>
                    </body>
            </html>
            """.trimIndent().toByteArray()
        return newFixedLengthResponse(
            Response.Status.OK,
            MIME_HTML,
            OnCloseInputStream(response) { this.onAuthorizeCode(code) },
            response.size.toLong()
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

    /**
     * Input stream decorator that notifies when is being closed.
     *
     * @property onClose Callback
     * @constructor
     *
     * @param bytes Byte array
     */
    private class OnCloseInputStream(bytes: ByteArray, private val onClose: () -> Unit) : InputStream() {

        /**
         * Delegated byte stream.
         */
        private val byteStream = ByteArrayInputStream(bytes)

        override fun close() {
            byteStream.close()
            onClose()
        }

        override fun read(): Int = byteStream.read()

        override fun read(b: ByteArray): Int = byteStream.read(b)

        override fun read(b: ByteArray, off: Int, len: Int): Int = byteStream.read(b, off, len)

        override fun skip(n: Long): Long = byteStream.skip(n)

        override fun available(): Int = byteStream.available()

        override fun mark(readlimit: Int) = byteStream.mark(readlimit)

        override fun reset() = byteStream.reset()

        override fun markSupported(): Boolean = byteStream.markSupported()
    }
}
