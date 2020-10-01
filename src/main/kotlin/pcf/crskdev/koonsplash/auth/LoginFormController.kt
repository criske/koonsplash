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
 * Login controller.
 *
 */
abstract class LoginFormController {

    /**
     * Login form.
     */
    private var loginFormListener: LoginFormListener? = null

    /**
     * Login form submitter.
     */
    private var loginFormSubmitter: LoginFormSubmitter? = null

    /**
     * Activate the login form (as in showing the ui form).
     *
     * Is activated in two scenarios:
     * - client is first time login, then _dueTo_ is null
     * - there was a login error
     *
     * @param dueTo Throwable
     */
    abstract fun activateForm(dueTo: Throwable?)

    /**
     * Attach form.
     *
     * @param loginFormListener LoginFormListener
     */
    fun attachFormListener(loginFormListener: LoginFormListener) {
        this.loginFormListener = loginFormListener
    }

    /**
     * Detach form
     *
     */
    fun detachFormListener() {
        this.loginFormListener = null
    }

    /**
     * Attach form submitter.
     *
     * @param loginFormSubmitter
     */
    internal fun attachFormSubmitter(loginFormSubmitter: LoginFormSubmitter) {
        if (this.loginFormSubmitter == null) {
            this.loginFormSubmitter = loginFormSubmitter
        } else {
            println("Warning: LoginFormSubmitter already attached")
        }
    }

    /**
     * Submit form.
     *
     * @param email Email.
     * @param password Password
     */
    fun submit(email: String, password: String) {
        requireNotNull(this.loginFormSubmitter)
        this.loginFormSubmitter?.submit(email, password)
    }

    /**
     * Give up on introducing credentials.
     *
     * @param cause: Cause of giving up, may be null as in "unknown"
     */
    fun giveUp(cause: Throwable?) {
        requireNotNull(this.loginFormSubmitter)
        this.loginFormListener?.onGiveUp(cause)
        this.loginFormSubmitter?.giveUp(cause)
        this.detachAll()
    }

    /**
     * On login success.
     *
     * Note: is not called from main thread.
     *
     */
    internal fun onLoginSuccess() {
        this.loginFormListener?.onSuccess()
    }

    /**
     * On login failure.
     *
     * Note: is not called from main thread.
     *
     * @param cause Cause of failure.
     */
    internal fun onLoginFailure(cause: Throwable) {
        this.loginFormListener?.onFailure(cause)
    }

    /**
     * Detach all components.
     *
     */
    internal fun detachAll() {
        this.detachFormListener()
        this.loginFormSubmitter = null
    }

    /**
     * Is detached from form listener and submitter.
     *
     */
    fun isDetached() = loginFormListener == null && loginFormSubmitter == null
}
