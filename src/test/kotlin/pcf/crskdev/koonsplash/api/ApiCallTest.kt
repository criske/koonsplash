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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runBlockingTest
import java.io.StringReader

@ExperimentalCoroutinesApi
internal class ApiCallTest : StringSpec({

    "should cancel request" {
        val call = mockk<ApiCall>()
        coEvery { call.invoke(any()) } coAnswers {
            delay(5000)
            ApiJsonResponse(mockk(), mockk(), emptyMap())
        }

        val channel = Channel<Unit>()
        launch {
            delay(100)
            channel.offer(Unit)
        }

        val result = call.cancelable(channel.receiveAsFlow(), this)
        result.shouldBeNull()
    }

    "should not cancel request" {
        val call = mockk<ApiCall>()
        coEvery { call.invoke(any()) } coAnswers {
            ApiJsonResponse(mockk(), StringReader("[]"), emptyMap())
        }

        val result = call.cancelable(emptyFlow(), this)
        result.shouldNotBeNull()
    }

    "should throw if something goes wrong with request" {
        val call = mockk<ApiCall>()
        coEvery { call.invoke(any()) } coAnswers {
            throw IllegalStateException()
        }

        shouldThrow<IllegalStateException> {
            call.cancelable(emptyFlow(), this)
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

        runBlockingTest {
            val channel = Channel<Unit>()
            val result = call.cancelableExecute(
                channel.receiveAsFlow(),
                Dispatchers.Unconfined,
                emptyList()
            )

            pauseDispatcher()
            launch {
                delay(3000)
                channel.offer(Unit)
            }
            val statuses = result.toList()
                .groupingBy { it.javaClass.simpleName }
                .eachCount()
            advanceTimeBy(12000)
            resumeDispatcher()
            statuses shouldBe mapOf(
                "Starting" to 1,
                "Current" to 3,
                "Canceled" to 1
            )
        }
    }

    "should not cancel execute" {
        val call = mockk<ApiCall>()
        val transformer: (ApiCall.Response) -> String = { "" }
        val flow = flow {
            emit(ApiCall.Status.Starting())
            for (i in 1..10) {
                emit(ApiCall.Status.Current(i, 10L))
                delay(1000)
            }
            emit(ApiCall.Status.Done(""))
        }
        every { call.execute(any(), any(), transformer) } returns flow

        runBlockingTest {
            val result = call.cancelableExecute(
                emptyFlow(),
                Dispatchers.Unconfined,
                emptyList(),
                transformer = transformer
            )
            pauseDispatcher()
            val statuses = result.toList()
                .groupingBy { it.javaClass.simpleName }
                .eachCount()
            advanceTimeBy(12000)
            resumeDispatcher()
            statuses shouldBe mapOf(
                "Starting" to 1,
                "Current" to 10,
                "Done" to 1
            )
        }
    }
})
