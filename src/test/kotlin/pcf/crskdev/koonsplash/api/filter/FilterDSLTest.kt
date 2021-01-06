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

package pcf.crskdev.koonsplash.api.filter

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import pcf.crskdev.koonsplash.api.filter.FilterDSL.Scope
import pcf.crskdev.koonsplash.api.filter.FilterDSL.Scope.Crop.BOTTOM
import pcf.crskdev.koonsplash.api.filter.FilterDSL.Scope.Crop.EDGES
import pcf.crskdev.koonsplash.api.filter.FilterDSL.Scope.Crop.ENTROPY
import pcf.crskdev.koonsplash.api.filter.FilterDSL.Scope.Crop.FACES
import pcf.crskdev.koonsplash.api.filter.FilterDSL.Scope.Crop.FOCALPOINT
import pcf.crskdev.koonsplash.api.filter.FilterDSL.Scope.Crop.LEFT
import pcf.crskdev.koonsplash.api.filter.FilterDSL.Scope.Crop.RIGHT
import pcf.crskdev.koonsplash.api.filter.FilterDSL.Scope.Crop.TOP
import pcf.crskdev.koonsplash.api.filter.FilterDSL.Scope.Fit.CROP

@ExperimentalUnsignedTypes
internal class FilterDSLTest : StringSpec({

    "should add parameters and can overwrite" {

        val params = withFilter {
            100u.w
            200u.w
            500u.h
            for (fit in Scope.Fit.values()) {
                fit(fit)
            }
            for (auto in Scope.Auto.values()) {
                auto(auto)
            }
            for (fm in Scope.Fm.values()) {
                fm(fm)
            }
        }()

        params.shouldBe(
            mapOf(
                "w" to "200",
                "h" to "500",
                "fit" to "scale",
                "auto" to "redeye",
                "fm" to "webp"
            )
        )
    }

    "should create dsl from other dsl" {

        val dsl = withFilter {
            100u.w
            500u.h
        }

        val params = withFilter(dsl) {
            50u.q
        }()

        params.shouldBe(
            mapOf(
                "w" to "100",
                "h" to "500",
                "q" to "50"
            )
        )
    }

    "should throw when `crop` if `fit` as crop, `w` and `h` are not set" {
        shouldThrow<FilterException> {
            withFilter {
                crop(TOP, BOTTOM)
            }()
        }
    }

    "should set crop parameter" {
        withFilter {
            100u.w
            500u.h
            fit(CROP)
            crop(ENTROPY)
        }() shouldBe mapOf(
            "w" to "100",
            "h" to "500",
            "fit" to "crop",
            "crop" to "entropy"
        )

        withFilter {
            safeCrop(
                100u, 500u,
                ENTROPY, LEFT, RIGHT, FACES, FOCALPOINT, EDGES
            )
        }() shouldBe mapOf(
            "w" to "100",
            "h" to "500",
            "fit" to "crop",
            "crop" to "entropy,left,right,faces,focalpoint,edges"
        )
    }

    "should throw if at least on crop type is not set in `safeCrop`" {
        shouldThrow<FilterException> {
            withFilter {
                safeCrop(100u, 500u)
            }
        }
    }

    "should ensure that quality is between 0 and 100" {
        shouldThrow<FilterException> {
            withFilter {
                200u.q
            }()
        }
        withFilter {
            50u.q
        }() shouldBe mapOf("q" to "50")
    }

    "should ensure that dpr can be set" {
        shouldThrow<FilterException> {
            withFilter {
                1u.dpr
            }()
        }
        shouldThrow<FilterException> {
            withFilter {
                1u.w
                1u.h
                0u.dpr
            }()
        }
        shouldThrow<FilterException> {
            withFilter {
                1u.w
                1u.h
                6u.dpr
            }()
        }
        withFilter {
            safeDpr(1u, 2u, 3u)
        }() shouldBe mapOf(
            "w" to "1",
            "h" to "2",
            "dpr" to "3"
        )
    }
})
