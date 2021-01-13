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

import java.io.IOException
import java.lang.IllegalStateException
import java.net.URI
import java.nio.charset.Charset

/**
 * Desktop url browser launcher.
 *
 * @author Cristian Pela
 * @since 0.1
 */
internal interface BrowserLauncher {

    /**
     * If `externalLauncher` is null, it will try to launch the url using the OS default browser.
     *
     * Note: this mainly will work if OS is a desktop one (win, linux, mac).
     * On mobile (android), user should handle the launch intent themselves by providing the `externalLauncher`.
     *
     * @param url
     * @param externalLauncher
     * @return Result success if launch was ok
     */
    fun launch(url: URI, externalLauncher: ((URI) -> Unit)? = null): Result<Unit>

    /**
     * Checks the current OS name.
     *
     * @constructor Create empty Os finder
     */
    interface OsFinder {

        /**
         * Operating system
         *
         */
        enum class Os {
            WINDOWS, LINUX, MAC, UNKNOWN
        }

        /**
         * Os type.
         *
         * @return Os.
         */
        fun os(): Os

        /**
         * Default implementation.
         *
         */
        object Default : OsFinder {
            override fun os(): Os {
                val os = System.getProperty("os.name").toLowerCase()
                return when {
                    os.contains("mac") -> Os.MAC
                    os.contains("nix") || os.contains("nux") -> Os.LINUX
                    os.contains("win") -> Os.WINDOWS
                    else -> Os.UNKNOWN
                }
            }
        }
    }

    /**
     * Os runtime command executor.
     *
     * @constructor Create empty Command executor
     */
    interface CommandExecutor {

        /**
         * Execute commands.
         *
         * @param commands
         * @return Result success if execution was ok.
         */
        fun execute(vararg commands: String): Result<Unit>

        /**
         * Default implementation.
         *
         */
        object Default : CommandExecutor {

            override fun execute(vararg commands: String): Result<Unit> {
                return try {
                    val process: Process = ProcessBuilder(commands.toList()).start()
                    process.waitFor()
                    val error = process.errorStream.use { it.readBytes().toString(Charset.defaultCharset()) }
                    if (error.isNotBlank()) {
                        Result.failure(IllegalStateException(error))
                    } else {
                        Result.success(Unit)
                    }
                } catch (ex: IOException) {
                    Result.failure(ex)
                }
            }
        }
    }
}

/**
 * Desktop url browser launcher impl.
 *
 * @author Cristian Pela
 * @since 0.1
 */
internal class BrowserLauncherImpl(
    private val commander: BrowserLauncher.CommandExecutor = BrowserLauncher.CommandExecutor.Default,
    private val osFinder: BrowserLauncher.OsFinder = BrowserLauncher.OsFinder.Default
) : BrowserLauncher {

    override fun launch(url: URI, externalLauncher: ((URI) -> Unit)?): Result<Unit> {
        if (externalLauncher != null) {
            externalLauncher.invoke(url)
        } else {
            val urlStr = url.toString()
            val os = this.osFinder.os()
            val commands = when (os) {
                BrowserLauncher.OsFinder.Os.MAC -> arrayOf("open ${urlStr.replace("&", "\\&")}")
                BrowserLauncher.OsFinder.Os.LINUX -> arrayOf("xdg-open $urlStr")
                BrowserLauncher.OsFinder.Os.WINDOWS -> arrayOf("cmd.exe", "/c", "start ${urlStr.replace("&", "^&")}")
                else -> emptyArray()
            }
            if (commands.isEmpty()) {
                return Result.failure(IllegalStateException("Can't launch link for operating system $os"))
            }
            return this.commander.execute(*commands)
        }
        return Result.success(Unit)
    }
}
