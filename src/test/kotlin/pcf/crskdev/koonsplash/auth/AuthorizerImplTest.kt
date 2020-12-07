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

import io.kotest.matchers.shouldBe
import okhttp3.mockwebserver.MockResponse
import pcf.crskdev.koonsplash.util.StringSpecIT
import pcf.crskdev.koonsplash.util.withJSONHeader

internal class AuthorizerImplTest : StringSpecIT({

    "should authorize and get token" {
        dispatchable = {
            val path = this.path ?: ""
            when {
                path.contains("token") -> {
                    val token = """
                         {
                           "access_token": "091343ce13c8ae780065ecb3b13dc903475dd22cb78a05503c2e0c69c5e98044",
                           "token_type": "bearer",
                           "scope": "public read_photos write_photos",
                           "created_at": 1436544465
                         }
                    """.trimIndent()
                    MockResponse()
                        .withJSONHeader()
                        .setResponseCode(200)
                        .setBody(token)
                }
                else -> MockResponse().setResponseCode(400)
            }
        }

        val codeServer = MockAuthCodeServer()
        val authorizer = AuthorizerImpl(AuthApiCallImpl(), codeServer)

        val token = authorizer.authorize(
            "123",
            "123",
            AuthScope.PUBLIC
        ) {
            codeServer.enqueueCode("123code")
        }

        token shouldBe AuthToken(
            "091343ce13c8ae780065ecb3b13dc903475dd22cb78a05503c2e0c69c5e98044",
            "bearer",
            "",
            AuthScope.PUBLIC + AuthScope.READ_PHOTOS + AuthScope.WRITE_PHOTOS,
            1436544465
        )
    }
})
