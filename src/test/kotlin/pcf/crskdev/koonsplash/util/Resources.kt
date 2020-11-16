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

package pcf.crskdev.koonsplash.util

import okhttp3.mockwebserver.MockResponse
import okio.Buffer
import okio.source
import java.io.File
import java.io.FileNotFoundException
import java.net.URL
import java.nio.file.Paths
import java.util.zip.CRC32

fun MockResponse.setBodyFromResource(filePath: String): MockResponse = setBody(
    Buffer().apply {
        writeAll(
            resource(filePath)
                .openStream()
                .source()
        )
    }
)

fun MockResponse.setBodyFromFile(file: File): MockResponse = setBody(
    Buffer().apply {
        writeAll(
            file.toURI()
                .toURL()
                .openStream()
                .source()
        )
    }
)

fun fileFromPath(vararg segments: String): File = File(segments.joinToString(File.separator))

fun URL.toFile(): File = Paths.get(toURI()).toFile()

fun Any.resource(filePath: String): URL =
    javaClass.classLoader.getResource(filePath) ?: throw FileNotFoundException("Resource not found $filePath")

fun File.checksum(): Long = CRC32().apply {
    val bytes = this@checksum.readBytes()
    update(bytes, 0, bytes.size)
}.value
