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

import java.lang.IllegalStateException

/**
 * Endpoint parser.
 *
 * @property endpoint Endpoint
 * @author Cristian Pela
 * @since 0.1
 */
internal class EndpointParser(
    private val apiUrl: String,
    private val endpoint: String
) {

    /**
     * Endpoint token type.
     *
     */
    sealed class Token {
        data class Path(val value: String) : Token()
        data class QueryParam(val name: String, val value: String) : Token()
    }

    /**
     * Parse the path with optional wildcard parameters
     *
     * @param params Params
     * @return list of endpoint tokens.
     */
    fun parse(vararg params: Any): List<Token> {
        params.forEachIndexed { index, param ->
            if (!(param is String || param is Number || param is Boolean)) {
                throw IllegalStateException(
                    "Current param at index $index is ${param.javaClass}. Allowed types are String, Number or Boolean."
                )
            }
        }
        return this.endpoint
            .let {
                if (params.isNotEmpty()) {
                    it.replace("\\{[a-zA-Z0-9_]+}".toRegex(), "%s")
                        .format(*params)
                } else {
                    it
                }
            }
            .split("?")
            .let { split ->
                split[0] // segment paths part
                    .let {
                        if (it.startsWith(apiUrl)) {
                            it.substring(apiUrl.length)
                        } else {
                            it
                        }
                    }
                    .split("/")
                    .filter { it.isNotEmpty() }
                    .map { Token.Path(it) }
                    .plus(
                        split.getOrNull(1) // query params part
                            ?.split("&")
                            ?.map {
                                it.split("=")
                                    .let { query ->
                                        Token.QueryParam(query[0], query[1])
                                    }
                            }
                            ?: emptyList()
                    )
            }
    }
}
