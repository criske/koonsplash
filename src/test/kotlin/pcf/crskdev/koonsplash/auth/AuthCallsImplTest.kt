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

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import okhttp3.mockwebserver.MockResponse
import pcf.crskdev.koonsplash.http.HttpClient
import pcf.crskdev.koonsplash.util.StringSpecIT
import pcf.crskdev.koonsplash.util.setBodyFromResource
import pcf.crskdev.koonsplash.util.withHTMLHeader
import pcf.crskdev.koonsplash.util.withJSONHeader
import java.net.URI

/**
 * Auth calls impl test for [AuthCallsImpl].
 *
 * @constructor Create empty Auth calls impl test
 */
internal class AuthCallsImplTest : StringSpecIT({

    val authCalls = AuthCallsImpl(HttpClient.http)

    "should authorize" {
        dispatchable = {
            MockResponse()
                .withHTMLHeader()
                .setBody("<!DOCTYPE html><html><head><title>Code</title></head><body><code>code123</code></body></html>")
        }

        val result = authCalls.authorize("123", URI.create("/"), AuthScope.ALL)
        result.isSuccess shouldBe true
        result.getOrThrow() shouldBe "code123"
    }

    "should authorize fail with require login" {
        dispatchable = {
            MockResponse()
                .withHTMLHeader()
                .setBodyFromResource("loginFlow/Login_Unsplash.html")
        }
        val result = authCalls.authorize("123", URI.create("/"), AuthScope.ALL)

        shouldThrow<NeedsLoginException> {
            result.getOrThrow()
        }
    }

    "should authorize fail authorization token not found" {
        dispatchable = {
            MockResponse()
                .withHTMLHeader()
                .setBody("")
        }
        val result = authCalls.authorize("123", URI.create("/"), AuthScope.ALL)

        shouldThrow<IllegalStateException> {
            result.getOrThrow()
        }
    }

    "should authorize fail" {
        dispatchable = { MockResponse().setResponseCode(500) }
        val result = authCalls.authorize("123", URI.create("/"), AuthScope.ALL)

        shouldThrow<IllegalStateException> {
            result.getOrThrow()
        }
    }

    "should login" {
        dispatchable = {
            MockResponse()
                .withHTMLHeader()
                .setBody("<!DOCTYPE html><html><head><title>Code</title></head><body><code>code123</code></body></html>")
        }

        val result = authCalls.loginForm("123", "foo@email", "123")

        result.isSuccess shouldBe true
        result.getOrThrow() shouldBe "code123"
    }

    "should login fail" {
        dispatchable = {
            MockResponse()
                .withHTMLHeader()
                .setBodyFromResource("loginFlow/Login_Unsplash_Invalid.html")
        }
        val result = authCalls.loginForm("123", "foo@email", "123")

        shouldThrow<InvalidCredentialsException> {
            result.getOrThrow()
        }
    }

    "should login with confirm authorizing" {
        dispatchable = {
            MockResponse()
                .withHTMLHeader()
                .setBodyFromResource("loginFlow/Confirm_Authorize.html")
        }
        val result = authCalls.loginForm("123", "foo@email", "123")

        val exception = shouldThrow<NeedsConfirmAuthorizeFormException> {
            result.getOrThrow()
        }
        exception.form shouldBe listOf(
            "utf8" to "✓",
            "authenticity_token" to "authenticity_token_xxx",
            "client_id" to "client_id_xxx",
            "redirect_uri" to "http://localhost:3000",
            "state" to "",
            "response_type" to "code",
            "scope" to "public read_user"
        )
    }

    "should confirm authorizing" {
        dispatchable = {
            MockResponse()
                .withHTMLHeader()
                .setBody("<!DOCTYPE html><html><head><title>Code</title></head><body><code>code123</code></body></html>")
        }

        val result = authCalls.authorizeForm(
            listOf(
                "utf8" to "✓",
                "authenticity_token" to "authenticity_token_xxx",
                "client_id" to "client_id_xxx",
                "redirect_uri" to "http://localhost:3000",
                "state" to "",
                "response_type" to "code",
                "scope" to "public read_user"
            )
        )
        result.isSuccess shouldBe true
        result.getOrThrow() shouldBe "code123"
    }

    "should confirm authorizing fail" {
        dispatchable = {
            MockResponse()
                .withHTMLHeader()
                .setBody("")
        }

        val result = authCalls.authorizeForm(
            listOf(
                "utf8" to "✓",
                "authenticity_token" to "authenticity_token_xxx",
                "client_id" to "client_id_xxx",
                "redirect_uri" to "http://localhost:3000",
                "state" to "",
                "response_type" to "code",
                "scope" to "public read_user"
            )
        )
        shouldThrow<ConfirmAuthorizeException> {
            result.getOrThrow()
        }
    }

    "should get token" {
        dispatchable = {
            MockResponse()
                .withJSONHeader()
                .setBody(
                    """
                    {
                       "access_token": "09134xxx",
                       "refresh_token": "",
                       "token_type": "bearer",
                       "scope": "public read_photos write_photos",
                       "created_at": 1436544465
                    }
                    """.trimIndent()
                )
        }

        val result = authCalls.token("code123", "123", "123", URI.create("/"))

        result.isSuccess shouldBe true
        result.getOrThrow() shouldBe AuthToken("09134xxx", "bearer", "", 1436544465)
    }

    "should fail getting token" {
        dispatchable = { MockResponse().setResponseCode(500) }
        val result = authCalls.token("code123", "123", "123", URI.create("/"))

        shouldThrow<IllegalStateException> {
            result.getOrThrow()
        }
    }
})
