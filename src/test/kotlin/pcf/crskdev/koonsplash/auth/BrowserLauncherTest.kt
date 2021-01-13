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

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import pcf.crskdev.koonsplash.auth.BrowserLauncher.OsFinder
import java.net.URI

internal class BrowserLauncherTest : StringSpec({

    val url = URI.create("http://localhost:3000")

    "it should launch browser on windows" {
        val osFinder = mockk<OsFinder>(relaxed = true)
        val commander = mockk<BrowserLauncher.CommandExecutor>(relaxed = true)

        every { osFinder.os() } returns OsFinder.Os.WINDOWS

        val launcher = BrowserLauncherImpl(commander, osFinder)

        launcher.launch(url)

        verify { commander.execute("cmd.exe", "/c", "start http://localhost:3000") }
    }

    "it should launch browser on linux nix" {
        val osFinder = mockk<OsFinder>(relaxed = true)
        val commander = mockk<BrowserLauncher.CommandExecutor>(relaxed = true)

        every { osFinder.os() } returns OsFinder.Os.LINUX

        val launcher = BrowserLauncherImpl(commander, osFinder)

        launcher.launch(url)

        verify { commander.execute("xdg-open http://localhost:3000") }
    }

    "it should launch browser on mac" {
        val osFinder = mockk<OsFinder>(relaxed = true)
        val commander = mockk<BrowserLauncher.CommandExecutor>(relaxed = true)

        every { osFinder.os() } returns OsFinder.Os.MAC

        val launcher = BrowserLauncherImpl(commander, osFinder)

        launcher.launch(url)

        verify { commander.execute("open http://localhost:3000") }
    }

    "it should delegate to external launcher" {
        val external = mockk<(URI) -> Unit>(relaxed = true)

        val launcher = BrowserLauncherImpl()

        launcher.launch(url, external)

        verify { external.invoke(url) }
    }

    "it should fail if os is unknown" {
        val osFinder = mockk<OsFinder>(relaxed = true)

        every { osFinder.os() } returns OsFinder.Os.UNKNOWN

        val launcher = BrowserLauncherImpl(osFinder = osFinder)

        launcher.launch(url).isFailure shouldBe true
    }

    "default os finder should find something" {

        val os = System.getProperty("os.name")

        System.setProperty("os.name", "windows")
        OsFinder.Default.os() shouldBe OsFinder.Os.WINDOWS

        System.setProperty("os.name", "nix")
        OsFinder.Default.os() shouldBe OsFinder.Os.LINUX

        System.setProperty("os.name", "nux")
        OsFinder.Default.os() shouldBe OsFinder.Os.LINUX

        System.setProperty("os.name", "mac")
        OsFinder.Default.os() shouldBe OsFinder.Os.MAC

        System.setProperty("os.name", "temple os")
        OsFinder.Default.os() shouldBe OsFinder.Os.UNKNOWN

        System.setProperty("os.name", os)
    }

    "default command executor can have error" {
        BrowserLauncher.CommandExecutor.Default.execute("foo").isFailure shouldBe true
    }

    "default command executor works" {
        val command = when (OsFinder.Default.os()) {
            OsFinder.Os.WINDOWS -> arrayOf("cmd.exe", "/c", "dir")
            OsFinder.Os.LINUX,
            OsFinder.Os.MAC -> arrayOf("pwd")
            else -> throw IllegalStateException("Unknown os")
        }
        BrowserLauncher.CommandExecutor.Default.execute(*command).isSuccess shouldBe true
    }
})
