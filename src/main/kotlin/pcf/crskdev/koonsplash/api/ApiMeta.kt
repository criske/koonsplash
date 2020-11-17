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

import java.net.URI

/**
 * Api response "metadata". This contains info about rate limit and pagination.
 *
 * @property apiCall [ApiCall] required for opening page links.
 * @property headers Multi map to extract limit and paging.
 * @author Cristian Pela
 * @since 0.1
 */
class ApiMeta internal constructor(
    private val headers: Headers,
    private val apiCall: (String) -> ApiCall
) {

    /**
     * Api calls rate limit.
     */
    val rateLimit: RateLimit = RateLimit.createRateLimit(headers)

    /**
     * Pagination, might be null.
     */
    val pagination: Pagination? = headers["Link"]
        ?.first()
        ?.let { Pagination.createPagination(apiCall, it) }
}

/**
 * Pagination header if present for [ApiJsonResponse].
 *
 * @property total Total pages.
 * @property currentNumber Current page number.
 * @property firstPage First page Link
 * @property prevPage Previous page Link
 * @property nextPage Next page Link
 * @property lastPage Last page Link
 * @constructor Create empty Pagination
 */
class Pagination internal constructor(
    val total: Int,
    val currentNumber: Int,
    val firstPage: Link.Api?,
    val prevPage: Link.Api?,
    val nextPage: Link.Api?,
    val lastPage: Link.Api?
) {
    companion object {

        internal fun createPagination(
            apiCall: (String) -> ApiCall,
            headerValue: String
        ): Pagination {
            val links = mutableMapOf<String, String>()
            var linkStarted = false
            var relStarted = false
            var link = ""
            val builder = StringBuilder()
            for (i in headerValue.indices) {
                if (i > 0 && headerValue[i - 1] == '<') {
                    linkStarted = true
                } else if (headerValue[i] == '>') {
                    linkStarted = false
                    link = builder.toString()
                    builder.setLength(0)
                }
                if (i > 0 && headerValue[i - 1] == '"' && headerValue[i] != ',') {
                    relStarted = true
                } else if (relStarted && headerValue[i] == '"') {
                    relStarted = false
                    require(link.isNotEmpty())
                    links[builder.toString()] = link
                    builder.setLength(0)
                }
                if (linkStarted || relStarted) {
                    builder.append(headerValue[i])
                }
            }
            val page: (String) -> Int = { it.split("page=")[1].toInt() }
            val total = links["last"]?.let(page)
                ?: links["prev"]?.let(page)?.inc()
                ?: 1
            val currentPageNo = links["prev"]?.let(page)?.inc()
                ?: links["next"]?.let(page)?.dec()
                ?: links["first"]?.let(page)
                ?: 1
            val firstPage = links["first"]?.let { Link.Api(URI.create(it), apiCall) }
            val prevPage = links["prev"]?.let { Link.Api(URI.create(it), apiCall) }
            val nextPage = links["next"]?.let { Link.Api(URI.create(it), apiCall) }
            val lastPage = links["last"]?.let { Link.Api(URI.create(it), apiCall) }
            return Pagination(total, currentPageNo, firstPage, prevPage, nextPage, lastPage)
        }
    }
}

/**
 * Rate limit header for [ApiJsonResponse]
 *
 * @property limit Limit
 * @property remaining Remaining api calls.
 * @constructor Create empty Rate limit
 */
class RateLimit internal constructor(val limit: Int, val remaining: Int) {

    object ReachedException : Exception()

    companion object {

        internal fun createRateLimit(headers: Headers): RateLimit {
            val limit = headers["X-Ratelimit-Limit"]?.first()?.toInt()
                ?: Int.MAX_VALUE
            val remaining = headers["X-Ratelimit-Remaining"]?.first()?.toInt()
                ?: Int.MAX_VALUE
            if (remaining <= 0) {
                throw ReachedException
            }
            return RateLimit(limit, remaining)
        }
    }
}
