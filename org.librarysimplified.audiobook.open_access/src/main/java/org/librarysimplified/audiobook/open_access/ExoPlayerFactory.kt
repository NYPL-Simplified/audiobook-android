package org.librarysimplified.audiobook.open_access

import android.content.Context
import androidx.media2.common.SessionPlayer
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.database.StandaloneDatabaseProvider
import com.google.android.exoplayer2.ext.media2.SessionPlayerConnector
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.upstream.cache.Cache
import com.google.android.exoplayer2.upstream.cache.CacheDataSource
import com.google.android.exoplayer2.upstream.cache.NoOpCacheEvictor
import com.google.android.exoplayer2.upstream.cache.SimpleCache
import org.librarysimplified.audiobook.player.api.PlayerBookID
import org.librarysimplified.audiobook.player.api.PlayerDownloadManagerType
import org.librarysimplified.audiobook.player.api.PlayerFactoryType
import org.librarysimplified.audiobook.player.api.PlayerType
import org.readium.navigator.media2.ExoPlayerDataSource
import org.readium.r2.shared.publication.Publication
import java.io.File

class ExoPlayerFactory(
  private val context: Context,
  private val publication: Publication,
  private val bookID: PlayerBookID
) : PlayerFactoryType {

  override fun createPlayer(): PlayerType {
   val cache = createCache()
   val sessionPlayer = createSessionPlayer(cache)
   return ExoPlayer(sessionPlayer, cache)
  }

  private fun createCache(): Cache {
    val globalCache = File(context.filesDir, "exoplayer_audio")
    val cacheDirectory = File(globalCache, bookID.value)

    return SimpleCache(
      cacheDirectory,
      NoOpCacheEvictor(),
      StandaloneDatabaseProvider(context)
    )
  }

  private fun createSessionPlayer(cache: Cache): SessionPlayer {
    val publicationDataSource = ExoPlayerDataSource.Factory(publication)

    val dataSourceFactory =
      CacheDataSource.Factory()
        .setCache(cache)
        .setUpstreamDataSourceFactory(publicationDataSource)
        // Disable writing to the cache by the player. We'll handle downloads through the
        // service.
        .setCacheWriteDataSinkFactory(null)

    val player: ExoPlayer = ExoPlayer.Builder(context)
      .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
      .setAudioAttributes(
        AudioAttributes.Builder()
          .setContentType(C.CONTENT_TYPE_MUSIC)
          .setUsage(C.USAGE_MEDIA)
          .build(),
        true
      )
      .setHandleAudioBecomingNoisy(true)
      .build()

    return SessionPlayerConnector(player)
  }


  override fun createDownloadManager(): PlayerDownloadManagerType {
    TODO("Not yet implemented")
  }
}
