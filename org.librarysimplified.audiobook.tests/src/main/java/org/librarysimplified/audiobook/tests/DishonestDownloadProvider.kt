package org.librarysimplified.audiobook.tests

import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.ListenableFutureTask
import org.librarysimplified.audiobook.api.PlayerDownloadProviderType
import org.librarysimplified.audiobook.api.PlayerDownloadRequest

/**
 * An implementation of the {@link PlayerDownloadProviderType} interface that lies about
 * making progress and then claims success.
 */

class DishonestDownloadProvider : PlayerDownloadProviderType {

  override fun download(request: PlayerDownloadRequest): ListenableFuture<Unit> {
    return ListenableFutureTask.create(
      {
        request.onProgress.invoke(0)
        request.onProgress.invoke(50)
        request.onProgress.invoke(100)
      },
      Unit
    )
  }
}
