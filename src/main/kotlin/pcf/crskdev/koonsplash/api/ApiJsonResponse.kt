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

import com.google.gson.JsonParser
import java.io.Reader

/**
 * Api json response. This encapsulates the json model and response metadata.
 *
 * Usage:
 * ```kotlin
 *  //assuming json response looks like "[{"message":"Hello","who":{"name":"John"}}]"
 *  val response = api.cal(...)()
 *  val root: ApiJson = response[0]
 *  println(root)//{"message":"Hi","place":{"name":"World"}}
 *  val message: String = root["message"]() // "hello"
 *  val name: String = root["message"]["name"]() // "John"
 * ```
 * The ApiJson invocation ([ApiJson.invoke]) will try to get a value of String, Boolean, Number and Link.
 *
 * @property apiCall [ApiCall] required for opening links
 * @property reader [Reader]
 * @property meta [ApiMeta]
 * @author Cristian Pela
 * @since 0.1
 */
class ApiJsonResponse internal constructor(
    private val apiCall: (String) -> ApiCall,
    private val reader: Reader,
    val meta: ApiMeta
) {

    /**
     * Root json element from response.
     */
    private val jsonEl = ApiJson(apiCall, JsonParser.parseReader(reader))

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
    operator fun get(selector: Selector): ApiJson = this.jsonEl[selector]

    override fun toString(): String = this.jsonEl.toString()
}
