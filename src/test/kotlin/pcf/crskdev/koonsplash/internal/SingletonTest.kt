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

package pcf.crskdev.koonsplash.internal

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.mockk.coEvery
import io.mockk.mockk
import pcf.crskdev.koonsplash.Koonsplash
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import kotlin.time.toDuration

@ExperimentalTime
@ExperimentalUnsignedTypes
internal class SingletonTest : StringSpec({

    afterTest {
        KoonsplashSingleton.instanceInternal = null
    }

    "should store the Koonsplash lib entry instance" {
        val koonsplash = mockk<Koonsplash>(relaxed = true)
        val auth = mockk<Koonsplash.Auth>(relaxed = true)
        val singleton = KoonsplashSingleton(koonsplash)

        coEvery { auth.signOut() } returns koonsplash
        coEvery { koonsplash.authenticated(any(), any(), any(), 3000u, 5.toDuration(DurationUnit.MINUTES), any()) } returns auth

        KoonsplashSingleton.instance.shouldBeInstanceOf<Koonsplash>()

        val singleAuth = singleton.authenticated("secret".toCharArray(), browserLauncher = mockk())

        singleAuth.shouldBeInstanceOf<Koonsplash.Auth>()
        singleAuth shouldBeSameInstanceAs KoonsplashSingleton.instance
        KoonsplashSingleton.instance.shouldBeInstanceOf<Koonsplash.Auth>()

        singleAuth.signOut()

        KoonsplashSingleton.instance.shouldBeInstanceOf<Koonsplash>()
    }

    "should throw if trying to authenticate twice without sign out" {
        val koonsplash = mockk<Koonsplash>(relaxed = true)
        val auth = mockk<Koonsplash.Auth>(relaxed = true)
        coEvery { koonsplash.authenticated(any(), any(), any(), 3000u, 5.toDuration(DurationUnit.MINUTES), any()) } returns auth

        val singleton = KoonsplashSingleton(koonsplash)
        singleton.authenticated("secret".toCharArray(), browserLauncher = mockk())
        shouldThrow<IllegalStateException> {
            singleton.authenticated("secret".toCharArray(), browserLauncher = mockk())
        }
    }

    "should throw if not initialized" {
        shouldThrow<IllegalStateException> {
            KoonsplashSingleton.instance
        }
    }
})
