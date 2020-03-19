package org.librarysimplified.audiobook.tests

import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import org.librarysimplified.audiobook.api.PlayerDownloadProviderType
import org.librarysimplified.audiobook.api.PlayerDownloadRequest
import java.text.ParseException

/**
 * An implementation of the {@link PlayerDownloadProviderType} interface that fails all
 * downloads.
 */

class FailingDownloadProvider : PlayerDownloadProviderType {

  override fun download(request: PlayerDownloadRequest): ListenableFuture<Unit> {
    val future = SettableFuture.create<Unit>()
    future.setException(ParseException("Error!", 0))
    return future
  }
}
