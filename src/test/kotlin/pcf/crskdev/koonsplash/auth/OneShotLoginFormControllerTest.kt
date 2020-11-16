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

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import io.mockk.verify

internal class OneShotLoginFormControllerTest : StringSpec({

    "should activate once" {
        val submitter = mockk<LoginFormSubmitter>(relaxed = true)
        val listener = mockk<LoginFormListener>(relaxed = true)
        val controller = OneShotLoginFormController("foo@mail.xyz", "bar").apply {
            attachFormListener(listener)
            attachFormSubmitter(submitter)
        }

        controller.activateForm(null)

        verify { submitter.submit("foo@mail.xyz", "bar") }

        val dueTo = Error("Error")
        controller.activateForm(dueTo)

        verify {
            submitter.giveUp(dueTo)
            listener.onGiveUp(dueTo)
        }

        controller.isDetached() shouldBe true
    }
})
