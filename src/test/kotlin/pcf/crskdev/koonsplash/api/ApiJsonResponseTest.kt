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

package pcf.crskdev.koonsplash.api

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import okhttp3.Headers
import java.io.StringReader

internal class ApiJsonResponseTest : StringSpec({

    val response = ApiJsonResponse(
        { mockk() },
        StringReader("[{\"message\":\"Hi\",\"place\":{\"name\":\"World\"}}]"),
        ApiMeta(Headers.Builder().build())
    )

    "should traverse the json tree" {
        response[0]["message"]<String>() shouldBe "Hi"
        response[0]["place"]["name"]<String>() shouldBe "World"
    }

    "should throw if selector key is not a string or int" {
        shouldThrow<IllegalStateException> {
            val badSelector = object : Comparable<Any> {
                override fun compareTo(other: Any): Int = 0
            }
            response[badSelector]
        }
    }

    "should throw if value type is not supported" {
        shouldThrow<IllegalStateException> {
            response[0]["message"]<List<*>>()
        }
    }
})
