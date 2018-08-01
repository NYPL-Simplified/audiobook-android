package org.nypl.audiobook.android.tests

import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.ListenableFutureTask
import org.nypl.audiobook.android.api.PlayerDownloadProviderType
import org.nypl.audiobook.android.api.PlayerDownloadProviderType.Result.SUCCEEDED
import org.nypl.audiobook.android.api.PlayerDownloadRequest

/**
 * An implementation of the {@link PlayerDownloadProviderType} interface that lies about
 * making progress and then claims success.
 */

class DishonestDownloadProvider : PlayerDownloadProviderType {

  override fun download(request: PlayerDownloadRequest): ListenableFuture<PlayerDownloadProviderType.Result> {
    return ListenableFutureTask.create({
      request.onProgress.invoke(0)
      request.onProgress.invoke(50)
      request.onProgress.invoke(100)
    }, SUCCEEDED)
  }

}
