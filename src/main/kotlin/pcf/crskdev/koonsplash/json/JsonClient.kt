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

package pcf.crskdev.koonsplash.json

import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import pcf.crskdev.koonsplash.auth.AuthScope
import pcf.crskdev.koonsplash.auth.AuthScopeNone
import pcf.crskdev.koonsplash.auth.AuthToken

/**
 * Json client based on Gson.
 * @author Cristian Pela
 * @since 0.1
 */
object JsonClient {
    val json = GsonBuilder()
        .setPrettyPrinting()
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        .registerTypeAdapter(AuthToken::class.java, AuthTokenAdapter())
        .create()

    private class AuthTokenAdapter : TypeAdapter<AuthToken>() {
        override fun write(out: JsonWriter, value: AuthToken) {
            with(out) {
                beginObject()
                name("access_token")
                value(value.accessToken)
                name("token_type")
                value(value.tokenType)
                name("scope")
                value(value.scope.value)
                name("refresh_token")
                value(value.refreshToken)
                name("created_at")
                value(value.createdAt)
                endObject()
            }
        }

        override fun read(`in`: JsonReader): AuthToken {
            var accessToken = ""
            var tokenType = " "
            var refreshToken = ""
            var scope: AuthScope = AuthScopeNone
            var createdAt = -1L
            var field = ""
            `in`.beginObject()
            while (`in`.hasNext()) {
                val token = `in`.peek()
                if (token == JsonToken.NAME)
                    field = `in`.nextName()
                when (field) {
                    "access_token" -> accessToken = `in`.nextString()
                    "token_type" -> tokenType = `in`.nextString()
                    "scope" -> scope = AuthScope.decode(`in`.nextString())
                    "refresh_token" -> refreshToken = `in`.nextString()
                    "created_at" -> createdAt = `in`.nextLong()
                }
            }
            `in`.endObject()
            return AuthToken(accessToken, tokenType, refreshToken, scope, createdAt)
        }
    }
}
