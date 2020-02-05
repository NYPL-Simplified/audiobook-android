package org.librarysimplified.audiobook.feedbooks

import com.google.common.util.concurrent.FluentFuture
import org.librarysimplified.audiobook.api.PlayerDownloadProviderType
import org.librarysimplified.audiobook.api.extensions.PlayerExtensionType
import org.librarysimplified.audiobook.api.extensions.PlayerXDownloadSubstitution
import org.librarysimplified.audiobook.manifest.api.PlayerManifestLink
import org.slf4j.LoggerFactory
import java.util.concurrent.ExecutorService

class FeedbooksPlayerExtension : PlayerExtensionType {

  private val logger =
    LoggerFactory.getLogger(FeedbooksPlayerExtension::class.java)

  override val name: String =
    "org.librarysimplified.audiobook.feedbooks"

  override fun onDownloadLink(
    statusExecutor: ExecutorService,
    downloadProvider: PlayerDownloadProviderType,
    link: PlayerManifestLink
  ): FluentFuture<PlayerXDownloadSubstitution>? {
    return null
  }
}
