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

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk

internal class PaginationTest : StringSpec({
    "should parse link header when on first page" {
        val link =
            "<https://api.unsplash.com/photos?page=20182>; rel=\"last\", <https://api.unsplash.com/photos?page=2>; rel=\"next\""
        val pagination = Pagination.createPagination(mockk(), link)
        pagination.currentNumber shouldBe 1
        pagination.total shouldBe 20182
        pagination.firstPage shouldBe null
        pagination.prevPage shouldBe null
        pagination.nextPage?.toString() shouldBe "https://api.unsplash.com/photos?page=2"
        pagination.lastPage?.toString() shouldBe "https://api.unsplash.com/photos?page=20182"
    }

    "should fully parse link header with all page links for page 2" {
        val link =
            "<https://api.unsplash.com/photos?page=1>; rel=\"first\", <https://api.unsplash.com/photos?page=1>; rel=\"prev\", <https://api.unsplash.com/photos?page=20128>; rel=\"last\", <https://api.unsplash.com/photos?page=3>; rel=\"next\""
        val pagination = Pagination.createPagination(mockk(), link)
        pagination.currentNumber shouldBe 2
        pagination.total shouldBe 20128
        pagination.firstPage.toString() shouldBe "https://api.unsplash.com/photos?page=1"
        pagination.prevPage?.toString() shouldBe "https://api.unsplash.com/photos?page=1"
        pagination.nextPage?.toString() shouldBe "https://api.unsplash.com/photos?page=3"
        pagination.lastPage?.toString() shouldBe "https://api.unsplash.com/photos?page=20128"
    }

    "should parse link header when on last page" {
        val link =
            "<https://api.unsplash.com/photos?page=1>; rel=\"first\", <https://api.unsplash.com/photos?page=20148>; rel=\"prev\""
        val pagination = Pagination.createPagination(mockk(), link)
        pagination.currentNumber shouldBe 20149
        pagination.total shouldBe 20149
        pagination.firstPage.toString() shouldBe "https://api.unsplash.com/photos?page=1"
        pagination.prevPage?.toString() shouldBe "https://api.unsplash.com/photos?page=20148"
        pagination.nextPage shouldBe null
        pagination.lastPage shouldBe null
    }

    "should default totalPages and current to 1" {
        val link = ""
        val pagination = Pagination.createPagination(mockk(), link)
        pagination.currentNumber shouldBe 1
        pagination.total shouldBe 1
    }
})
