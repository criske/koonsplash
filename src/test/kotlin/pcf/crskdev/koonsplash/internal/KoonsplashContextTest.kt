/*
 *  Copyright (c) 2021. Pela Cristian
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

package pcf.crskdev.koonsplash.internal

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.mockk.mockk
import pcf.crskdev.koonsplash.api.ApiCall
import pcf.crskdev.koonsplash.auth.AuthContext
import pcf.crskdev.koonsplash.auth.BrowserLauncher
import java.lang.IllegalStateException
import java.net.URI

internal class KoonsplashContextTest : StringSpec({

    "should throw if context is not init" {
        val context = KoonsplashContext.Builder().build()
        shouldThrow<IllegalStateException> {
            context.apiCaller("Foo")
        }
        shouldThrow<IllegalStateException> {
            context.auth.accessKey
        }
        shouldThrow<IllegalStateException> {
            context.browserLauncher.launch(URI.create("/"))
        }
    }

    "should create a new context" {
        val auth = mockk<AuthContext>()
        val launcher = mockk<BrowserLauncher>()
        val apiCaller: (String) -> ApiCall = mockk()
        val context = KoonsplashContext.Builder()
            .auth { auth }
            .apiCaller(apiCaller)
            .browserLauncher(launcher)
            .build()
        context.auth shouldBeSameInstanceAs auth
        context.apiCaller shouldBeSameInstanceAs apiCaller
        context.browserLauncher shouldBeSameInstanceAs launcher
    }

    "should create a new context based upon" {
        val auth = mockk<AuthContext>()
        val launcher = mockk<BrowserLauncher>()
        val apiCaller: (String) -> ApiCall = mockk()
        val context = KoonsplashContext.Builder()
            .auth { auth }
            .build()
            .newBuilder()
            .apiCaller(apiCaller)
            .browserLauncher(launcher)
            .build()
        context.auth shouldBeSameInstanceAs auth
        context.apiCaller shouldBeSameInstanceAs apiCaller
        context.browserLauncher shouldBeSameInstanceAs launcher
    }
})
