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

/**
 * HTTP Verbs.
 *
 * @author Cristian Pela
 * @since 0.1
 */
sealed class Verb {
    /**
     * Retrieving resources.
     *
     */
    object Get : Verb()

    /**
     *Deleting resources.
     *
     */
    object Delete : Verb()

    /**
     * Modifies the resources.
     *
     */
    sealed class Modify(vararg val formEntries: FormEntry) : Verb() {

        /**
         * Creating resources.
         *
         */
        class Post(vararg formEntries: FormEntry) : Modify(*formEntries)
        /**
         * Updating  resources.
         *
         */
        class Put(vararg formEntries: FormEntry) : Modify(*formEntries)
    }
}
