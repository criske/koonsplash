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

package pcf.crskdev.koonsplash

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.Dispatchers
import pcf.crskdev.koonsplash.auth.AuthScope
import pcf.crskdev.koonsplash.auth.AuthTokenStorage
import java.net.URI
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import kotlin.time.toDuration

@ExperimentalTime
@ExperimentalUnsignedTypes
@ExperimentalStdlibApi
internal class KoonsplashTest : StringSpec({

    "should build" {
        Koonsplash.builder("123")
            .authTokenStorage(AuthTokenStorage.None)
            .dispatcher(Dispatchers.Main)
            .openLinksStrategy { Result.success(Unit) }
            .build()
            .shouldBeInstanceOf<Koonsplash>()
    }

    "should build authenticated" {
        val launcher: (URI) -> Unit = {}
        val built = Koonsplash.AuthenticatedBuilder("123".toCharArray())
            .scopes(AuthScope.PUBLIC)
            .host("foo")
            .port(1u)
            .timeout(10.toDuration(DurationUnit.MINUTES))
            .browserLauncher(launcher)
            .build()
        built.secretKey shouldBe "123".toCharArray()
        built.host shouldBe "foo"
        built.port shouldBe 1u
        built.timeout shouldBe 10.toDuration(DurationUnit.MINUTES)
        built.browserLauncher shouldBe launcher
    }
})
