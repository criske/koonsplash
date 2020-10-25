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
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.mockk
import java.io.StringReader
import java.lang.IllegalStateException

internal class ApiJsonTest : StringSpec({
    "it should be empty" {
        ApiJson.createFromString("{}", mockk()).isEmpty shouldBe true
        ApiJson.createFromString("[]", mockk()).isEmpty shouldBe true
        ApiJson.createFromString("  ", mockk()).isEmpty shouldBe true
    }
    "should delegate toString" {
        ApiJson.createFromReader(StringReader("[]"), mockk()).toString() shouldBe "[]"
    }
    "should invoke as primitive or as Link" {
        ApiJson.createFromString(""" "true" """, mockk())<Boolean>() shouldBe true
        ApiJson.createFromString(""" "1" """, mockk())<Int>() shouldBe 1
        ApiJson.createFromString(""" "1" """, mockk())<Long>() shouldBe 1L
        ApiJson.createFromString(""" "1" """, mockk())<Number>() shouldBe 1
        ApiJson.createFromString(""" "${Long.MAX_VALUE}" """, mockk())<Number>() shouldBe Long.MAX_VALUE
        ApiJson.createFromString(""" "1.0" """, mockk())<Double>() shouldBe 1.0
        ApiJson.createFromString(""" "1.0" """, mockk())<Number>() shouldBe 1.0
        ApiJson.createFromString(""" "1.0f" """, mockk())<Float>() shouldBe 1.0f
        ApiJson.createFromString(""" "1.0f" """, mockk())<Number>() shouldBe 1.0f
        ApiJson.createFromString(""" "Hi" """, mockk())<String>() shouldBe "Hi"
        ApiJson.createFromString(""" "http://www.example.xyz" """, mockk())<Link>().shouldBeInstanceOf<Link.Browser>()
        shouldThrow<IllegalStateException> {
            ApiJson.createFromString(""" "Hi" """, mockk())<Any>()
        }
    }
})
