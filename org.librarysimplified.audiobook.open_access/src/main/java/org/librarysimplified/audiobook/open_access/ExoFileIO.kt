package org.librarysimplified.audiobook.open_access

import java.io.File
import java.io.IOException
import java.security.SecureRandom

/**
 * File I/O operations.
 */

object ExoFileIO {

  /**
   * Delete the file `f` if it exists.
   *
   * @param f The file
   *
   * @throws IOException On I/O errors
   */

  @Throws(IOException::class)
  fun fileDelete(f: File) {
    if (f.exists()) {

      /**
       * This is a workaround for the broken semantics of
       * Android FAT32 filesystems. Essentially, deleting a file and then
       * recreating that file with the same name will result in EBUSY for
       * the life of the process. The entirely imaginary half existing half
       * not-existing name will disappear when the process exits. The following
       * code renames files to have random suffixes prior to being deleted, to
       * work around the issue. This is not a long term solution!
       */

      val sb = StringBuilder()
      sb.append(f.toString())
      sb.append(".")
      sb.append(randomHex(16))

      val ft = File(sb.toString())
      fileRename(f, ft)
      ft.delete()
      if (ft.exists()) {
        throw IOException(String.format("Could not delete '%s'", ft))
      }
    }
  }

  private fun randomHex(i: Int): String {
    val sr = SecureRandom()
    val bytes = ByteArray(i)
    val sb = StringBuilder(i * 2)
    sr.nextBytes(bytes)
    for (index in 0 until i) {
      sb.append(String.format("%02x", bytes[index]))
    }
    return sb.toString()
  }

  /**
   * Rename the file `from` to `to`.
   *
   * @param from The source file
   * @param to   The target file
   *
   * @throws IOException On I/O errors
   */

  @Throws(IOException::class)
  fun fileRename(
    from: File,
    to: File) {

    if (!from.renameTo(to)) {
      if (!from.isFile) {
        throw IOException(
          String.format(
            "Could not rename '%s' to '%s' ('%s' does not exist or is not a " + "file)", from, to, from))
      }

      val to_parent = to.parentFile
      if (!to_parent.isDirectory) {
        throw IOException(
          String.format(
            "Could not rename '%s' to '%s' ('%s' is not a directory)",
            from,
            to,
            to_parent))
      }

      throw IOException(
        String.format(
          "Could not rename '%s' to '%s'", from, to))
    }
  }
}
