package org.librarysimplified.audiobook.tests.sandbox

import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * A randomly shuffled list of Lorem Ipsum strings.
 */

class SandboxLoremIpsum private constructor(
  val lines: List<String>
) {

  companion object {

    /**
     * Create a new shuffled list of strings.
     */

    fun create(): SandboxLoremIpsum {
      return SandboxLoremIpsum::class.java.getResourceAsStream(
        "/org/librarysimplified/audiobook/tests/sandbox/lorem.txt").use { stream ->
        BufferedReader(InputStreamReader(stream, "UTF-8")).useLines { lines ->
          val shuffled = lines.toMutableList()
          shuffled.shuffle()
          SandboxLoremIpsum(shuffled)
        }
      }
    }
  }
}
