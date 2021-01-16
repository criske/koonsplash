/*
 *  Copyright (c) 2021. Pela Cristian
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

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Auth context
 *
 * @author Cristian Pela
 * @since 0.1
 */
internal interface AuthContext {

    /**
     * Access key which is constant across the lib.
     */
    val accessKey: AccessKey

    /**
     * Get token or null if token not present.
     *
     * @return AuthToken or null
     */
    suspend fun getToken(): AuthToken?

    /**
     * As clearable context.
     *
     * @return Clear
     */
    fun asClearable(): ClearableAuthContext {
        throw UnsupportedOperationException("This is not a ClearableAuthContext but $this")
    }

    /**
     * None.
     *
     */
    class None(override val accessKey: AccessKey) : AuthContext {
        override suspend fun getToken(): AuthToken? = null
    }
}

/**
 * Makes sure that doesn't have access to ClearableAuthContext
 *
 */
internal fun AuthContext.safe() =
    if (this is ClearableAuthContext) {
        object : AuthContext {
            override val accessKey: AccessKey
                get() = this@safe.accessKey

            override suspend fun getToken(): AuthToken? = this@safe.getToken()
        }
    } else {
        this
    }

/**
 * Clearable Auth context.
 *
 * @author Cristian Pela
 * @since 0.1
 */
internal interface ClearableAuthContext : AuthContext {

    /**
     * Has token set.
     * @return Boolean.
     */
    suspend fun hasToken(): Boolean

    /**
     * Clear the saved token.
     *
     */
    suspend fun clear()

    /**
     * Set a new token.
     *
     * @param authToken
     */
    suspend fun reset(authToken: AuthToken)

    override fun asClearable(): ClearableAuthContext = this
}

/**
 * In memory cached auth context.
 * @property storage Storage.
 * @property accessKey Access key.
 * @author Cristian Pela
 * @since 0.1
 */
internal class CachedAuthContext(
    private val storage: AuthTokenStorage,
    override val accessKey: AccessKey
) : ClearableAuthContext {

    /**
     * State.
     */
    private var state: State

    /**
     * State lock.
     */
    private val mutex = Mutex()

    init {
        state = storage.load()?.let { State.LoggedIn(it) } ?: State.LoggedOut
    }

    override suspend fun hasToken() = mutex.withLock {
        this.state is State.LoggedIn
    }

    override suspend fun clear() {
        mutex.withLock {
            this.state = State.LoggedOut
            this.storage.clear()
        }
    }

    override suspend fun reset(authToken: AuthToken) {
        mutex.withLock {
            this.state = State.LoggedIn(authToken)
            this.storage.save(authToken)
        }
    }

    override suspend fun getToken(): AuthToken? {
        val state = mutex.withLock { this.state }
        return when (state) {
            is State.LoggedIn -> state.token
            State.LoggedOut -> throw SignedOutException
        }
    }

    /**
     * State ADT.
     *
     */
    internal sealed class State {
        class LoggedIn(val token: AuthToken) : State()
        object LoggedOut : State()
    }
}
