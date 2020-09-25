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

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.io.IOException
import java.net.URI
import java.util.concurrent.Executor

internal class AuthorizerImplTest : StringSpec({

    val authToken = AuthToken(
        "token123",
        "bearer",
        "refresh123",
        1
    )

    "should get token without login" {
        val server = mockk<AuthCodeServer>(relaxed = true)
        val controller = TestLoginFormController(null, "foo@mail.com", "123")
        val authCalls = MockAuthCalls(
            Result.success("code123"),
            Result.success("code123"),
            Result.success(authToken)
        )
        val onSuccess = mockk<(AuthToken) -> Unit>(relaxed = true)
        val onFailure = mockk<(Throwable) -> Unit>(relaxed = true)
        val authorizer = AuthorizerImpl(
            "access123",
            "secret123",
            server,
            authCalls
        )

        every { server.callbackUri } returns URI.create("/")
        every { server.startServing() } returns true

        authorizer.authorize(TestExecutor, controller, onFailure, onSuccess)

        verify {
            onSuccess(authToken)
            server.stopServing()
        }

        authCalls.calledAuthorize shouldBe true
        authCalls.calledLoginForm shouldBe false
        authCalls.calledToken shouldBe true
        controller.isDetached() shouldBe true
    }

    "should get token with log in" {
        val server = mockk<AuthCodeServer>(relaxed = true)
        val loginForm = mockk<LoginFormListener>(relaxed = true)
        val controller = TestLoginFormController(loginForm, "foo@mail.com", "123")
        val authCalls = MockAuthCalls(
            Result.failure(NeedsLoginException("csrf")),
            Result.success("code123"),
            Result.success(authToken)
        )
        val onSuccess = mockk<(AuthToken) -> Unit>(relaxed = true)
        val onFailure = mockk<(Throwable) -> Unit>(relaxed = true)
        val authorizer = AuthorizerImpl(
            "access123",
            "secret123",
            server,
            authCalls,
        )

        every { server.callbackUri } returns URI.create("/")
        every { server.startServing() } returns true

        authorizer.authorize(TestExecutor, controller, onFailure, onSuccess)

        verify {
            onSuccess(authToken)
            loginForm.onSuccess()
            server.stopServing()
        }

        authCalls.calledAuthorize shouldBe true
        authCalls.calledLoginForm shouldBe true
        authCalls.calledToken shouldBe true
        controller.isDetached() shouldBe true
    }

    "should fail login due to invalid credentials" {
        val server = mockk<AuthCodeServer>(relaxed = true)
        val loginForm = mockk<LoginFormListener>(relaxed = true)
        val controller = TestLoginFormController(loginForm, "foo@mail.com", "123")
        val authCalls = MockAuthCalls(
            Result.failure(NeedsLoginException("csrf")),
            Result.failure(InvalidCredentialsException),
            Result.success(authToken)
        )
        val onSuccess = mockk<(AuthToken) -> Unit>(relaxed = true)
        val onFailure = mockk<(Throwable) -> Unit>(relaxed = true)
        val authorizer = AuthorizerImpl(
            "access123",
            "secret123",
            server,
            authCalls,
        )

        every { server.callbackUri } returns URI.create("/")
        every { server.startServing() } returns true

        authorizer.authorize(TestExecutor, controller, onFailure, onSuccess)

        verify(exactly = 0) {
            loginForm.onSuccess()
            onSuccess(any())
        }

        verify(exactly = 1) {
            loginForm.onFailure()
            loginForm.onGiveUp()
            onFailure(any())
            server.stopServing()
        }

        authCalls.calledAuthorize shouldBe true
        authCalls.calledLoginForm shouldBe true
        authCalls.calledToken shouldBe false
        controller.isDetached() shouldBe true
    }

    "should call onFailure if something occurs on authorize" {
        val server = mockk<AuthCodeServer>(relaxed = true)
        val loginForm = mockk<LoginFormListener>(relaxed = true)
        val controller = TestLoginFormController(loginForm, "foo@mail.com", "123")
        val authCalls = MockAuthCalls(
            Result.failure(IOException()),
            Result.success("code123"),
            Result.success(authToken)
        )
        val onSuccess = mockk<(AuthToken) -> Unit>(relaxed = true)
        val onFailure = mockk<(Throwable) -> Unit>(relaxed = true)
        val authorizer = AuthorizerImpl(
            "access123",
            "secret123",
            server,
            authCalls,
        )

        every { server.callbackUri } returns URI.create("/")
        every { server.startServing() } returns true

        authorizer.authorize(TestExecutor, controller, onFailure, onSuccess)

        verify(exactly = 0) {
            onSuccess(any())
            loginForm.onSuccess()
            loginForm.onFailure()
        }

        verify {
            onFailure(any())
            server.stopServing()
        }
        authCalls.calledAuthorize shouldBe true
        authCalls.calledLoginForm shouldBe false
        authCalls.calledToken shouldBe false
        controller.isDetached() shouldBe true
    }

    "should call onFailure if something occurs on login" {
        val server = mockk<AuthCodeServer>(relaxed = true)
        val loginForm = mockk<LoginFormListener>(relaxed = true)
        val controller = TestLoginFormController(loginForm, "foo@mail.com", "123")
        val authCalls = MockAuthCalls(
            Result.failure(NeedsLoginException("csrf")),
            Result.failure(IOException()),
            Result.success(authToken)
        )
        val onSuccess = mockk<(AuthToken) -> Unit>(relaxed = true)
        val onFailure = mockk<(Throwable) -> Unit>(relaxed = true)
        val authorizer = AuthorizerImpl(
            "access123",
            "secret123",
            server,
            authCalls,
        )

        every { server.callbackUri } returns URI.create("/")
        every { server.startServing() } returns true

        authorizer.authorize(TestExecutor, controller, onFailure, onSuccess)

        verify(exactly = 0) {
            onSuccess(any())
            loginForm.onSuccess()
            loginForm.onFailure()
        }

        verify {
            onFailure(any())
            server.stopServing()
        }

        authCalls.calledAuthorize shouldBe true
        authCalls.calledLoginForm shouldBe true
        authCalls.calledToken shouldBe false
        controller.isDetached() shouldBe true
    }

    "should call onFailure if something occurs on token" {
        val server = mockk<AuthCodeServer>(relaxed = true)
        val loginForm = mockk<LoginFormListener>(relaxed = true)
        val controller = TestLoginFormController(loginForm, "foo@mail.com", "123")
        val authCalls = MockAuthCalls(
            Result.failure(NeedsLoginException("csrf")),
            Result.success("code123"),
            Result.failure(IOException())
        )
        val onSuccess = mockk<(AuthToken) -> Unit>(relaxed = true)
        val onFailure = mockk<(Throwable) -> Unit>(relaxed = true)
        val authorizer = AuthorizerImpl(
            "access123",
            "secret123",
            server,
            authCalls,
        )

        every { server.callbackUri } returns URI.create("/")
        every { server.startServing() } returns true

        authorizer.authorize(TestExecutor, controller, onFailure, onSuccess)

        verify(exactly = 0) {
            onSuccess(any())
            loginForm.onSuccess()
            loginForm.onFailure()
        }

        verify {
            onFailure(any())
            server.stopServing()
        }

        authCalls.calledAuthorize shouldBe true
        authCalls.calledLoginForm shouldBe true
        authCalls.calledToken shouldBe true
        controller.isDetached() shouldBe true
    }
})

class MockAuthCalls(
    private val authorize: Result<AuthorizationCode>,
    private val login: Result<AuthorizationCode>,
    private val token: Result<AuthToken>
) : AuthCalls {

    var calledAuthorize = false

    var calledLoginForm = false

    var calledToken = false

    override fun authorize(
        accessKey: AccessKey,
        redirectUri: URI,
        vararg scopes: AuthScope
    ): Result<AuthorizationCode> {
        calledAuthorize = true
        return authorize
    }

    override fun loginForm(
        authenticityToken: AuthenticityToken,
        email: String,
        password: String
    ): Result<AuthorizationCode> {
        calledLoginForm = true
        return login
    }

    override fun token(
        authorizationCode: AuthorizationCode,
        accessKey: AccessKey,
        secretKey: SecretKey,
        redirectUri: URI
    ): Result<AuthToken> {
        calledToken = true
        return token
    }
}

object TestExecutor : Executor {
    override fun execute(command: Runnable) {
        command.run()
    }
}

private class TestLoginFormController(
    loginFormListener: LoginFormListener?,
    private val email: String,
    private val password: String
) : LoginFormController() {

    private var submitted = false

    init {
        loginFormListener?.let { attachFormListener(it) }
    }

    override fun activateForm() {
        if (!submitted) {
            submitted = true
            this.submit(email, password)
        } else {
            this.giveUp()
        }
    }
}
