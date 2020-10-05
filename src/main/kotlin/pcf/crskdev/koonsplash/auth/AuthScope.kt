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

@file:Suppress("MemberVisibilityCanBePrivate")

package pcf.crskdev.koonsplash.auth

/**
 * OAuth2 Unsplash authorize scope.
 *
 * @property value Name.
 * @author Cristian Pela
 * @since 0.1
 */
sealed class AuthScope {

    abstract val value: String

    operator fun plus(other: AuthScope): AuthScope =
        AuthScopePlus(this, other)

    operator fun minus(other: AuthScope): AuthScope =
        AuthScopeMinus(this, other)

    companion object {
        val PUBLIC: AuthScope = AuthScopeValue("public")
        val READ_USER: AuthScope = AuthScopeValue("read_user")
        val WRITE_USER: AuthScope = AuthScopeValue("write_user")
        val READ_PHOTOS: AuthScope = AuthScopeValue("read_photos")
        val WRITE_PHOTOS: AuthScope = AuthScopeValue("write_photos")
        val WRITE_LIKES: AuthScope = AuthScopeValue("write_likes")
        val WRITE_FOLLOWERS: AuthScope = AuthScopeValue("write_followers")
        val READ_COLLECTIONS: AuthScope = AuthScopeValue("read_collections")
        val WRITE_COLLECTIONS: AuthScope = AuthScopeValue("write_collections")
        val ALL: AuthScope =
            (PUBLIC + READ_USER + WRITE_USER + READ_PHOTOS + WRITE_PHOTOS + WRITE_LIKES + WRITE_FOLLOWERS + READ_COLLECTIONS + WRITE_COLLECTIONS)
    }
}

private class AuthScopeValue(override val value: String) : AuthScope()

private class AuthScopePlus(left: AuthScope, right: AuthScope) : AuthScope() {

    override val value: String = (left.values.toSet() + right.values).joinToString("+")
}

private class AuthScopeMinus(left: AuthScope, right: AuthScope) : AuthScope() {

    private val rightValues = right.value

    override val value: String = left.values.filter { !rightValues.contains(it) }
        .joinToString("+")
        .takeIf { it.isNotEmpty() }
        ?: throw IllegalStateException("It should be at least one scope after subtracting")
}

private val AuthScope.values get() = this.value.split("+").toList()
