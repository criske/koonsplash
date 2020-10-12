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

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import okhttp3.HttpUrl

internal class EndpointParserTest : StringSpec({

    val api = HttpUrl.Builder()
        .scheme("https")
        .host("api.unsplash.com")
        .build()
        .toString()

    "should parse without wildcard params" {
        EndpointParser(Endpoint(api, "/photos/random")).parse() shouldBe listOf(
            EndpointParser.Token.Path("photos"),
            EndpointParser.Token.Path("random")
        )
        EndpointParser(Endpoint(api, "photos/random")).parse() shouldBe listOf(
            EndpointParser.Token.Path("photos"),
            EndpointParser.Token.Path("random")
        )
    }

    "should parse without wildcard params and allow base url in endpoint" {
        EndpointParser(Endpoint(api, "$api/photos/random")).parse() shouldBe listOf(
            EndpointParser.Token.Path("photos"),
            EndpointParser.Token.Path("random")
        )
    }

    "should parse with wildcard params" {
        EndpointParser(
            Endpoint(
                api,
                "$api/photos/{id}/collection/{collection_id}?page={page}&q={q}&fm={jpg}&ordered={ordered}"
            )
        ).parse("12345", "col23", 1, 75, "jpg", true) shouldBe listOf(
            EndpointParser.Token.Path("photos"),
            EndpointParser.Token.Path("12345"),
            EndpointParser.Token.Path("collection"),
            EndpointParser.Token.Path("col23"),
            EndpointParser.Token.QueryParam("page", "1"),
            EndpointParser.Token.QueryParam("q", "75"),
            EndpointParser.Token.QueryParam("fm", "jpg"),
            EndpointParser.Token.QueryParam("ordered", "true")
        )
    }

    "should throw when one of params is not string, number of boolean" {
        shouldThrow<IllegalStateException> {
            EndpointParser(Endpoint(api, "/photos/{id}")).parse(Any())
        }
    }

    "should return empty tokens if we have a full url" {
        EndpointParser(Endpoint("http://foobar.zyx")).parse() shouldBe emptyList<EndpointParser.Token>()
    }
})
