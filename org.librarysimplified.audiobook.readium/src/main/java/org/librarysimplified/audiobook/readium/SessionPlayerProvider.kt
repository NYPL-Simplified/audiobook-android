package org.librarysimplified.audiobook.readium

import android.content.Context
import androidx.media2.common.SessionPlayer
import org.librarysimplified.audiobook.api.PlayerBookID
import org.readium.r2.shared.publication.Publication

interface SessionPlayerProvider {

  fun createSessionPlayer(context: Context, publication: Publication, bookID: PlayerBookID): SessionPlayer?
}
