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

package pcf.crskdev.koonsplash.api

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runBlockingTest
import java.io.StringReader

@ExperimentalCoroutinesApi
internal class ApiCallTest : StringSpec({

    "should cancel request" {
        val call = mockk<ApiCall>()
        coEvery { call.invoke(any()) } coAnswers {
            delay(1000)
            ApiJsonResponse(mockk(), StringReader("[]"), emptyMap())
        }
        launch {
            val cancelSignal = MutableSharedFlow<Unit>()
            launch {
                delay(300)
                cancelSignal.emit(Unit)
            }
            val result = call.cancelable(cancelSignal, this)
            result.shouldBeNull()
            cancelSignal.subscriptionCount.value shouldBe 0
            this.cancel()
        }
    }

    "should not cancel request" {
        val call = mockk<ApiCall>()
        coEvery { call.invoke(any()) } coAnswers {
            ApiJsonResponse(mockk(), StringReader("[]"), emptyMap())
        }
        launch {
            val cancelSignal = MutableSharedFlow<Unit>()
            val result = call.cancelable(cancelSignal)
            result.shouldNotBeNull()
            cancelSignal.subscriptionCount.value shouldBe 0
            this.cancel()
        }
    }

    "should throw if something goes wrong with request" {
        val call = mockk<ApiCall>()
        coEvery { call.invoke(any()) } coAnswers {
            throw IllegalStateException()
        }
        shouldThrow<IllegalStateException> {
            call.cancelable(MutableSharedFlow(), this)
        }
    }

    "should cancel execute" {
        val call = mockk<ApiCall>()
        val flow = flow {
            emit(ApiCall.Status.Starting())
            for (i in 1..10) {
                emit(ApiCall.Status.Current(i, 10L))
                delay(1000)
            }
            emit(ApiCall.Status.Done(ApiJsonResponse(mockk(), StringReader("[]"), emptyMap())))
        }
        every { call.execute(any()) } returns flow

        val cancelSignal = MutableSharedFlow<Unit>()
        val result = call.cancelableExecute(cancelSignal, emptyList())
        runBlockingTest {
            launch {
                pauseDispatcher()
                launch {
                    delay(3000)
                    cancelSignal.emit(Unit)
                }
                val statuses = result.toList()
                    .groupingBy { it.javaClass.simpleName }
                    .eachCount()
                advanceTimeBy(12000)
                resumeDispatcher()
                cancelSignal.subscriptionCount.value shouldBe 0
                statuses shouldBe mapOf(
                    "Starting" to 1,
                    "Current" to 3,
                    "Canceled" to 1
                )
                cancel()
            }
        }
    }

    "should not cancel execute" {
        val call = mockk<ApiCall>()
        val transformer: (ApiCall.Response) -> String = { "" }
        val flow = flow {
            emit(ApiCall.Status.Starting())
            for (i in 1..10) {
                emit(ApiCall.Status.Current(i, 10L))
            }
            emit(ApiCall.Status.Done(""))
        }
        every { call.execute(any(), any(), transformer) } returns flow

        val cancelSignal = MutableSharedFlow<Unit>()
        val result = call.cancelableExecute(cancelSignal, emptyList(), transformer = transformer)
        launch {
            launch {
                // simulate that is used by other request too
                cancelSignal.collect {}
            }
            launch {
                val statuses = result.toList()
                    .groupingBy { it.javaClass.simpleName }
                    .eachCount()
                statuses shouldBe mapOf(
                    "Starting" to 1,
                    "Current" to 10,
                    "Done" to 1
                )
                // this subscription is done, it should remain the above open one.
                cancelSignal.subscriptionCount.value shouldBe 1
            } // collecting is done
            cancel()
        }
    }
})
