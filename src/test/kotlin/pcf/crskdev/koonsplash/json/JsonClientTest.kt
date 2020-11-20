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

package pcf.crskdev.koonsplash.json

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import pcf.crskdev.koonsplash.auth.AuthScope
import pcf.crskdev.koonsplash.auth.AuthToken

internal class JsonClientTest : StringSpec({

    "should deserialize an AuthToken from json" {
        val json = """{
            "access_token": "09134xxx",
            "refresh_token": "",
            "token_type": "bearer",
            "scope": "public read_photos write_photos",
            "created_at": 1436544465
        }
        """.trimIndent()

        val token = JsonClient.json.fromJson(json, AuthToken::class.java)

        token.accessToken shouldBe "09134xxx"
        token.tokenType shouldBe "bearer"
        token.refreshToken shouldBe ""
        token.scope.value shouldBe (AuthScope.PUBLIC + AuthScope.READ_PHOTOS + AuthScope.WRITE_PHOTOS).value
        token.createdAt shouldBe 1436544465
    }

    "should serialize an AuthToken to json" {
        val json = JsonClient.json.toJson(
            AuthToken(
                "09134xxx",
                "bearer",
                "",
                AuthScope.PUBLIC + AuthScope.READ_PHOTOS + AuthScope.WRITE_PHOTOS,
                1436544465
            )
        )

        val token = JsonClient.json.fromJson(json, AuthToken::class.java)

        token.accessToken shouldBe "09134xxx"
        token.tokenType shouldBe "bearer"
        token.refreshToken shouldBe ""
        token.scope.value shouldBe (AuthScope.PUBLIC + AuthScope.READ_PHOTOS + AuthScope.WRITE_PHOTOS).value
        token.createdAt shouldBe 1436544465
    }
})
