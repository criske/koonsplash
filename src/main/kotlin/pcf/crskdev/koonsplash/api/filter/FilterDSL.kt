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

import java.net.URI

/**
 * Filter d s l.
 *
 * [See more](https://unsplash.com/documentation#dynamically-resizable-images)
 *
 * @property parameters
 * @property scopeBlock
 * @constructor Create empty Filter d s l
 * @author Cristian Pela
 * @since 0.1
 */
@ExperimentalUnsignedTypes
class FilterDSL internal constructor(
    private val parameters: Map<String, String>,
    private val scopeBlock: Scope.() -> Unit = {}
) {

    companion object {

        /**
         * Allowed keys for parameters
         */
        private val PARAMETER_KEYS = listOf("crop", "fm", "auto", "fit", "w", "h", "dpr")

        /**
         * N o n e representation of FilterDSL.
         */
        val NONE = FilterDSL(mutableMapOf())

        /**
         * Creates a dsl from uri query parameters.
         *
         * @param url
         */
        fun fromUrl(url: URI) = url.query
            ?.split("&")
            ?.fold(mutableMapOf<String, String>()) { acc, s ->
                acc.apply {
                    val (key, value) = s.split("=")
                    this[key] = value
                }
            }?.let {
                FilterDSL(it)
            } ?: NONE
    }

    /**
     * Dsl scope.
     *
     * @constructor Create empty Scope
     */
    @ExperimentalUnsignedTypes
    interface Scope {

        /**
         * Crop types.
         *
         * @constructor Create empty Crop
         */
        enum class Crop {
            TOP, BOTTOM, LEFT, RIGHT, FACES, FOCALPOINT, EDGES, ENTROPY;
        }

        /**
         * Image Format types.
         *
         * @constructor Create empty Fm
         */
        enum class Fm {
            GIF, JP2, JPG, JSON, JXR, PJPG, MP4, PNG, PNG8, PNG32, WEBM, WEBP;
        }

        /**
         * Automatic types.
         *
         * @constructor Create empty Auto
         */
        enum class Auto {
            COMPRESS, ENHANCE, FORMAT, REDEYE
        }

        /**
         * Fit types.
         *
         * @constructor Create empty Fit
         */
        enum class Fit {
            CLAMP, CLIP, CROP, FACEAREA, FILL, FILLMAX, MAX, MIN, SCALE;
        }

        /**
         * For applying cropping to the photo.
         *
         * Parameters [Scope.fit] = [Fit.CROP], [Scope.w], [Scope.h] must be set in order to work.
         *
         * [See more](https://docs.imgix.com/apis/rendering/size/crop)
         * @param crop Crop.
         */
        fun crop(vararg crop: Crop)

        /**
         *
         * Converting image format.
         *
         * [See more](https://docs.imgix.com/apis/rendering/format/fm)
         * @param fm Fm
         */
        fun fm(fm: Fm)

        /**
         * Helps you automate a baseline level of optimization across your entire image catalog.
         *
         * [See more](https://docs.imgix.com/apis/rendering/auto/auto)
         * @param auto Auto
         */
        fun auto(auto: Auto = Auto.FORMAT)

        /**
         * Controls how the output image is fit to its target dimensions after resizing,
         * and how any background areas will be filled.
         *
         * [See more](https://docs.imgix.com/apis/rendering/size/fit)
         * @param fit Fit
         */
        fun fit(fit: Fit = Fit.CLIP)

        /**
         * Adjusting the width.
         *
         * [See more](https://docs.imgix.com/apis/rendering/size/w)
         */
        val UInt.w: Unit

        /**
         * Adjusting the height.
         *
         * [See more](https://docs.imgix.com/apis/rendering/size/w)
         */
        val UInt.h: Unit

        /**
         * For changing the compression quality when using lossy file formats.
         * From 0 to 100.
         *
         * [See more](https://docs.imgix.com/apis/url/format/q)
         */
        val UInt.q: Unit

        /**
         * Controls the output density of your image, so you can serve images at the correct density for every
         * user's device from a single master image.
         *
         * Parameters [Scope.w] and [Scope.h] must be set in order to work.
         *
         * Value range: from 1 to 5.
         *
         * [See more](https://docs.imgix.com/apis/rendering/pixel-density/dpr)
         *
         * @param value Value
         */
        val UInt.dpr: Unit
    }

    private val scope: FilterDSLScopeImpl

    init {
        // hook-up the scope
        val mutableParameters = mutableMapOf<String, String>()
        this.parameters.forEach { (k, v) ->
            // ensure that provided parameters contain the allowed keys.
            if (PARAMETER_KEYS.contains(k)) {
                mutableParameters[k] = v
            }
        }
        scope = FilterDSLScopeImpl(mutableParameters).apply(this.scopeBlock)
    }

    /**
     * Invoke the parameters map.
     *
     * @return Map.
     */
    operator fun invoke(): Map<String, String> {
        val parameters = scope.parameters
        if (
            parameters.containsKey("crop") && (!parameters.containsKey("w") || !parameters.containsKey("h") || !parameters.containsKey("fit") || (parameters.containsKey("fit") && parameters["fit"] != "crop"))
        ) {
            throw FilterException("When applying crop parameters `w`,`h` and `fit` as `crop` must be set")
        }
        if (parameters["q"]?.toUInt() ?: 100u > 100u) {
            throw FilterException("Quality must be between 0 and 100")
        }
        if (parameters.containsKey("dpr")) {
            if (!parameters.containsKey("w") || !parameters.containsKey("h")) {
                throw FilterException("In order that dpr to work, both w and h parameters must be set")
            }
            if (parameters["dpr"]!!.toUInt() !in (1u..5u)) {
                throw FilterException("Dpr value must be between 1 and 5")
            }
        }
        return parameters.toMap()
    }

    /**
     * Merge with other dsl
     *
     * @param other
     * @param scope
     * @receiver
     * @return new Dsl
     */
    fun merge(other: FilterDSL, scope: FilterDSL.Scope.() -> Unit = {}): FilterDSL {
        val merged = mutableMapOf<String, String>().apply {
            this.putAll(this@FilterDSL())
            this.putAll(other())
        }
        return FilterDSL(merged, scope)
    }
}

/**
 * Filter dsl creator.
 *
 * @param scope Scope
 * @param from From other DSL config.
 * @receiver
 * @return Filter Dsl
 */
@ExperimentalUnsignedTypes
fun withFilter(
    from: FilterDSL = FilterDSL.NONE,
    scope: FilterDSL.Scope.() -> Unit = {}
): FilterDSL = FilterDSL(emptyMap()).merge(from, scope)

/**
 * Ensure that crop parameter is properly set without throwing a FilterDSLException.
 *
 * @param width Width
 * @param height Height
 * @param crop Crop
 */
@ExperimentalUnsignedTypes
fun FilterDSL.Scope.safeCrop(width: UInt, height: UInt, vararg crop: FilterDSL.Scope.Crop) {
    if (crop.isEmpty()) {
        throw FilterException("At least one crop type must be set when safe cropping")
    }
    width.w
    height.h
    fit(FilterDSL.Scope.Fit.CROP)
    crop(*crop)
}

/**
 * Ensure that crop parameter is properly set without throwing a FilterDSLException.
 *
 * @param size Size in pixels
 * @param crop Crop
 */
@ExperimentalUnsignedTypes
fun FilterDSL.Scope.safeCrop(size: UInt, vararg crop: FilterDSL.Scope.Crop) {
    size.w
    size.h
    fit(FilterDSL.Scope.Fit.CROP)
    crop(*crop)
}

/**
 * Ensure that dpr can safely be set.
 *
 * @param width Width
 * @param height Height
 * @param dpr Dpr
 */
@ExperimentalUnsignedTypes
fun FilterDSL.Scope.safeDpr(width: UInt, height: UInt, dpr: UInt) {
    width.w
    height.h
    dpr.dpr
}
