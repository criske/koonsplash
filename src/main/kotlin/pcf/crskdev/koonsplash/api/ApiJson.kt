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

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.internal.LazilyParsedNumber
import pcf.crskdev.koonsplash.internal.KoonsplashContext
import java.io.Reader

/**
 * Api response json model tree backed by gson.
 *
 * @property context: KoonsplashContext
 * @property jsonEl Current json element
 * @constructor Create empty Api json
 * @author Cristian Pela
 * @since 0.1
 */
class ApiJson internal constructor(
    @PublishedApi internal val jsonEl: JsonElement,
    @PublishedApi internal val context: KoonsplashContext
) {

    companion object {

        /**
         * Create ApiJson from reader
         *
         * @param context KoonsplashContext
         * @param apiCall Api Call provider
         */
        internal fun createFromReader(reader: Reader, context: KoonsplashContext) =
            ApiJson(JsonParser.parseReader(reader), context)

        /**
         * Create ApiJson from string mainly used as convenience method in tests.
         *
         * @param json String
         * @param context KoonsplashContext
         * @receiver
         */
        internal fun createFromString(json: String, context: KoonsplashContext) =
            ApiJson(JsonParser.parseString(json), context)
    }

    /**
     * Try to get a value of String, Boolean, Number, Link associated with this json key.
     *
     * @param T
     * @return T as accepted value type.
     */
    inline operator fun <reified T> invoke(): T {
        val kClass = T::class
        if (Link::class.java.isAssignableFrom(kClass.java)) {
            return Link.create(this.jsonEl.asString, this.context) as T
        }
        return when (kClass) {
            Number::class -> (this.jsonEl.asNumber as LazilyParsedNumber).toKotlinNumber()
            Int::class -> this.jsonEl.asInt
            Long::class -> this.jsonEl.asLong
            Double::class -> this.jsonEl.asDouble
            Float::class -> this.jsonEl.asFloat
            String::class -> this.jsonEl.asString
            Boolean::class -> this.jsonEl.asBoolean
            else -> throw IllegalStateException(
                "Type not supported ${T::class}. Available types are: Number, String, Boolean, Link."
            )
        } as T
    }

    @PublishedApi
    internal fun LazilyParsedNumber.toKotlinNumber(): Number {
        val value = this.toString()
        return if (value.contains(".")) {
            if (value.endsWith("f", true)) {
                this.toFloat()
            } else {
                this.toDouble()
            }
        } else {
            val num = this.toLong()
            if (num in Int.MIN_VALUE..Int.MAX_VALUE) {
                num.toInt()
            } else {
                num
            }
        }
    }

    /**
     * Gets an ApiJson element based on:
     *  - string key selector if querying a json object
     *  - int index for a json array.
     *
     * Note: Only string and integers are accepted as selectors and one must know
     * the json response structure/schema beforehand.
     *
     * @param selector String or Int
     * @return [ApiJson]
     */
    operator fun get(selector: Selector): ApiJson {
        val element = when (selector) {
            is String -> (this.jsonEl as JsonObject).get(selector) ?: JsonNull.INSTANCE
            is Int -> (this.jsonEl as JsonArray)[selector]
            else -> throw IllegalStateException(
                "Invalid selector ${selector::class.java}. Only strings or integers are permitted."
            )
        }
        return ApiJson(element, this.context)
    }

    /**
     * Checks if the ApiJson's wrapped element is empty.
     */
    val isEmpty: Boolean = when (jsonEl) {
        is JsonArray -> jsonEl.size() == 0
        is JsonObject -> jsonEl.size() == 0
        is JsonNull -> true
        else -> jsonEl.toString().isBlank()
    }

    override fun toString(): String {
        return this.jsonEl.toString()
    }
}
