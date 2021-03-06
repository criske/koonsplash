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

package pcf.crskdev.koonsplash.auth

/**
 * Auth token storage
 *
 * @author Cristian Pela
 * @since 0.1
 */
interface AuthTokenStorage {

    /**
     * Save token.
     *
     * @param token
     */
    fun save(token: AuthToken)

    /**
     * Load token or null if not found.
     *
     * @return AuthToken?
     */
    fun load(): AuthToken?

    /**
     * Removes the token from storage.
     *
     */
    fun clear()

    /**
     * No storage representation.
     *
     * @constructor Create empty None
     */
    object None : AuthTokenStorage {

        override fun save(token: AuthToken) {
        }

        override fun load(): AuthToken? = null

        override fun clear() {
        }
    }
}
