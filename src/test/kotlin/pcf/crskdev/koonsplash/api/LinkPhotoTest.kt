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

package pcf.crskdev.koonsplash.api

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import pcf.crskdev.koonsplash.api.filter.FilterDSL
import pcf.crskdev.koonsplash.api.filter.FilterException
import pcf.crskdev.koonsplash.api.filter.safeCrop
import pcf.crskdev.koonsplash.api.filter.withFilter
import java.net.URI

@ExperimentalUnsignedTypes
internal class LinkPhotoTest : StringSpec({

    val baseUrl: HttpUrl = URI.create("http://localhost:8080/?ixid=1").toHttpUrlOrNull()!!

    fun HttpUrl.buildQueries(builderScope: HttpUrl.Builder.() -> Unit) =
        this.newBuilder().apply(builderScope).build()

    "should check if url has `ixid` parameter set" {
        shouldThrow<FilterException> {
            Link.Photo(URI.create("/"), mockk()).filter {}
        }

        Link.Photo(baseUrl.toUri(), mockk()).filter {}
    }

    "should apply filter" {
        Link.Photo(baseUrl.toUri(), mockk())
            .filter {
                fm(FilterDSL.Scope.Fm.JPG)
                safeCrop(500u, FilterDSL.Scope.Crop.FACES)
                80u.q
            }
            .asPhotoLink()
            .url shouldBe baseUrl.buildQueries {
            addQueryParameter("fm", "jpg")
            addQueryParameter("w", "500")
            addQueryParameter("h", "500")
            addQueryParameter("fit", "crop")
            addQueryParameter("crop", "faces")
        }.toUri()
    }

    "should overwrite filter from base url" {
        val photo = Link.Photo(
            baseUrl.buildQueries {
                addQueryParameter("h", "100")
                addQueryParameter("w", "5")
                addQueryParameter("dpr", "1")
            }.toUri(),
            mockk()
        )
        photo.filter {
            10u.w
        }.asPhotoLink().url shouldBe baseUrl.buildQueries {
            addQueryParameter("h", "100")
            addQueryParameter("w", "10")
            addQueryParameter("dpr", "1")
        }.toUri()
    }

    "should add new filter from photo link" {
        Link.Photo(baseUrl.toUri(), mockk())
            .filter { 10u.w }
            .asPhotoLink()
            .filter { 100u.h }
            .asPhotoLink().url shouldBe baseUrl
            .buildQueries {
                addQueryParameter("w", "10")
                addQueryParameter("h", "100")
            }.toUri()
    }

    "should add new filter upon existent one" {
        Link.Photo(baseUrl.toUri(), mockk())
            .filter { 10u.w }
            .add { 100u.h }
            .asPhotoLink().url shouldBe baseUrl
            .buildQueries {
                addQueryParameter("w", "10")
                addQueryParameter("h", "100")
            }.toUri()

        Link.Photo(baseUrl.toUri(), mockk())
            .filter(withFilter { 10u.w }) { 100u.h }
            .asPhotoLink().url shouldBe baseUrl
            .buildQueries {
                addQueryParameter("w", "10")
                addQueryParameter("h", "100")
            }.toUri()
    }

    "should add reset filter upon existent one" {
        Link.Photo(baseUrl.toUri(), mockk())
            .filter { 10u.w }
            .add { 100u.h }
            .reset { this.auto() }
            .asPhotoLink().url shouldBe baseUrl
            .buildQueries {
                addQueryParameter("auto", "format")
            }.toUri()
        Link.Photo(baseUrl.buildQueries { addQueryParameter("q", "10") }.toUri(), mockk())
            .filter { 10u.w }
            .add { 100u.h }
            .reset()
            .asPhotoLink().url shouldBe baseUrl
            .toUri()
    }
})
