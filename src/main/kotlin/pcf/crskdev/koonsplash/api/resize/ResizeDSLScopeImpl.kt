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

package pcf.crskdev.koonsplash.api.resize

/**
 * Filter d s l scope impl.
 *
 * @property parameters Parameters source.
 * @constructor Create empty Filter d s l scope impl
 * @author Cristian Pela
 * @since 0.1
 */
@ExperimentalUnsignedTypes
internal class ResizeDSLScopeImpl(internal val parameters: MutableMap<String, String>) : ResizeDSL.Scope {

    override fun crop(vararg crop: ResizeDSL.Scope.Crop) {
        parameters["crop"] = crop.joinToString(",") { it.toString().toLowerCase() }
    }

    override fun fm(fm: ResizeDSL.Scope.Fm) {
        parameters["fm"] = fm.toString().toLowerCase()
    }

    override fun auto(auto: ResizeDSL.Scope.Auto) {
        parameters["auto"] = auto.toString().toLowerCase()
    }

    override fun fit(fit: ResizeDSL.Scope.Fit) {
        parameters["fit"] = fit.toString().toLowerCase()
    }

    override val UInt.w: Unit
        get() {
            parameters["w"] = this.toString()
            Unit
        }

    override val UInt.h: Unit
        get() {
            parameters["h"] = this.toString()
            Unit
        }

    override val UInt.q: Unit
        get() {
            parameters["q"] = this.toString()
            Unit
        }

    override val UInt.dpr: Unit
        get() {
            parameters["dpr"] = this.toString()
            Unit
        }
}
