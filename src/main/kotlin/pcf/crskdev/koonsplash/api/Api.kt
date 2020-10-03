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

import okhttp3.OkHttpClient
import pcf.crskdev.koonsplash.auth.AccessKey

/**
 * Api endpoints.
 *
 * @author Cristian Pela
 * @since 0.1
 */
interface Api {

    /**
     * Api call builder.
     *
     * Then endpoint may contain wildcards in this format _{my_wildcard}_
     *
     * Usage examples:
     *
     * ``` kotlin
     *  api.call("/me")()
     *  api.call("me")()
     *  api.call("https://api.unsplash.com/me")()
     *  api.call("/me", verb = Verb.Modify.Put("location" to "London"))()
     *
     *  val prepared = api.call("/photos/{id}")
     *  val photo1 = prepared("id_1")
     *  val photo2 = prepared("id_2")
     *
     *  api.call("/search/photos?page=1&query=office")()
     *
     *  api.call("/search/photos?page={page}&query={query}")
     * ```
     * The call parameters could be a string, number or boolean depending
     * of Unsplash endpoint api specification. Just to be safe passing all strings is advised
     * Passing an object will throw.
     *
     * @param endpoint Endpoint path
     * @param verb Http verb, default [Verb.Get]
     * @return ApiCall than can be called with Params if wildcard is present
     */
    fun call(endpoint: String, verb: Verb = Verb.Get): ApiCall
}

/**
 * Api implementation.
 *
 * @property httpClient Http client/
 */
internal class ApiImpl(
    private val httpClient: OkHttpClient,
    private val accessKey: AccessKey,
) : Api {

    override fun call(endpoint: String, verb: Verb): ApiCall =
        ApiCallImpl(Endpoint(endpoint, verb), httpClient, accessKey, null)
}
