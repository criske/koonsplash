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
 * Auth type definitions and custom exceptions for Auth API.
 *
 * @author Cristian Pela
 * @since 0.1
 */

typealias AccessKey = String
typealias AuthorizationCode = String
typealias AuthenticityToken = String
typealias SecretKey = String

/**
 * Thrown when a login form is needed to continue the authorization process.
 *
 * @property authenticityToken Payload for csrf-token
 */
class NeedsLoginException(val authenticityToken: AuthenticityToken) : RuntimeException()

/**
 *Invalid credentials exception
 *
 * @constructor Create empty Invalid credentials exception
 */
object InvalidCredentialsException : RuntimeException()

/**
 *Thrown when give up on a call due to timeout or repeated invalid credentials.
 *
 */
object GiveUpException : RuntimeException()

/**
 * Thrown when trying to do authenticated calls when after signed out.
 *
 */
object SignedOutException : RuntimeException()
