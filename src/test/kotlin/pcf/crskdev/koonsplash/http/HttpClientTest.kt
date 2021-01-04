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

package pcf.crskdev.koonsplash.http

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.SocketPolicy
import pcf.crskdev.koonsplash.http.HttpClient.executeCo
import pcf.crskdev.koonsplash.util.StringSpecIT
import java.io.IOException

internal class HttpClientTest : StringSpecIT({

    val templateCall: () -> Call = {
        HttpClient.http.newCall(
            Request.Builder()
                .url(HttpClient.apiBaseUrl.toString())
                .build()
        )
    }
    "should be Success" {
        dispatchable = { MockResponse().setResponseCode(200) }

        templateCall().executeCo().isSuccessful shouldBe true
    }

    "should be Failure" {
        dispatchable = {
            MockResponse().setResponseCode(404)
        }
        val exception = shouldThrow<HttpException> {
            templateCall().executeCo()
        }
        exception.code shouldBe 404
    }

    "should throw IO when there is a network problem" {
        dispatchable = {
            MockResponse()
                .setSocketPolicy(SocketPolicy.DISCONNECT_AFTER_REQUEST)
        }
        shouldThrow<IOException> {
            templateCall().executeCo()
        }
    }

    "should cancel request" {
        dispatchable = {
            MockResponse()
                .setSocketPolicy(SocketPolicy.NO_RESPONSE)
        }
        val call = templateCall()
        val job = launch {
            call.executeCo()
        }

        launch {
            delay(500)
            job.cancel()
        }.join()

        call.isCanceled() shouldBe true
    }
})
