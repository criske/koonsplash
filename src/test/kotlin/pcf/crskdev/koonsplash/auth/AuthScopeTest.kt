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
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.lang.IllegalStateException

internal class AuthScopeTest : StringSpec({

    "ALL should show all scopes delimited by `+`" {
        AuthScope.ALL.value shouldBe """
            public
            +read_user
            +write_user
            +read_photos
            +write_photos
            +write_likes
            +write_followers
            +read_collections
            +write_collections
        """.trimIndent().replace("\n", "")
    }

    "should add two 2 scopes" {
        (AuthScope.PUBLIC + AuthScope.READ_USER).value shouldBe "public+read_user"
    }

    "should ignore adding the same scope" {
        (AuthScope.PUBLIC + AuthScope.READ_USER + AuthScope.READ_USER).value shouldBe "public+read_user"
    }

    "should subtract scope" {
        (AuthScope.ALL - AuthScope.WRITE_PHOTOS).value shouldBe """
            public
            +read_user
            +write_user
            +read_photos
            +write_likes
            +write_followers
            +read_collections
            +write_collections
        """.trimIndent().replace("\n", "")
    }

    "should ignore + if it is first char when subtracting scope" {
        (AuthScope.ALL - AuthScope.PUBLIC).value shouldBe """
            read_user
            +write_user
            +read_photos
            +write_photos
            +write_likes
            +write_followers
            +read_collections
            +write_collections
        """.trimIndent().replace("\n", "")
    }

    "should subtract from a sum of scopes" {
        (AuthScope.ALL - (AuthScope.PUBLIC + AuthScope.WRITE_PHOTOS + AuthScope.WRITE_COLLECTIONS)).value shouldBe """
            read_user
            +write_user
            +read_photos
            +write_likes
            +write_followers
            +read_collections
        """.trimIndent().replace("\n", "")
    }

    "should have at least one scope after subtracting" {
        shouldThrow<IllegalStateException> {
            (AuthScope.PUBLIC + AuthScope.READ_USER) - (AuthScope.PUBLIC + AuthScope.READ_USER)
        }
    }

    "should ignore if scope not presented after subtracting again" {
        ((AuthScope.PUBLIC + AuthScope.READ_USER) - AuthScope.PUBLIC - AuthScope.PUBLIC).value shouldBe "read_user"
    }

    "should ignore empty scope" {
        (AuthScopeNone + AuthScope.PUBLIC - AuthScopeNone).value shouldBe "public"
        (AuthScopeNone - AuthScope.PUBLIC + AuthScopeNone + AuthScopeNone).value shouldBe "public"
        (AuthScopeNone + AuthScopeNone).value shouldBe ""
        (AuthScopeNone - AuthScopeNone).value shouldBe ""
    }

    "should decode scopes" {
        AuthScope.decode("read_user write_user").value shouldBe "read_user+write_user"
        AuthScope.decode("read_user+write_user").value shouldBe "read_user+write_user"
    }
})
