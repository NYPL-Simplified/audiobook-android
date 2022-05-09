package org.librarysimplified.audiobook.views

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.res.Resources
import android.view.LayoutInflater
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.joda.time.Duration
import org.joda.time.format.PeriodFormatter
import org.joda.time.format.PeriodFormatterBuilder
import org.librarysimplified.audiobook.api.PlayerSpineElementType

/**
 * A Recycler view adapter used to display and control the table of contents.
 */

class PlayerTOCAdapter(
  private val context: Context,
  private val spineElements: List<PlayerSpineElementType>,
  private val onSelect: (PlayerSpineElementType) -> Unit,
) :
  RecyclerView.Adapter<PlayerTOCAdapter.ViewHolder>() {

  private val listener: View.OnClickListener
  private var currentSpineElement: Int = -1

  private val periodFormatter: PeriodFormatter =
    PeriodFormatterBuilder()
      .printZeroAlways()
      .minimumPrintedDigits(2)
      .appendHours()
      .appendLiteral(":")
      .appendMinutes()
      .appendLiteral(":")
      .appendSeconds()
      .toFormatter()

  private val timeStrings: PlayerTimeStrings.SpokenTranslations

  init {
    this.timeStrings =
      PlayerTimeStrings.SpokenTranslations.createFromResources(this.context.resources)
    this.listener = View.OnClickListener { v -> this.onSelect(v.tag as PlayerSpineElementType) }
  }

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): ViewHolder {

    UIThread.checkIsUIThread()

    val view =
      LayoutInflater.from(parent.context)
        .inflate(R.layout.player_toc_item_view, parent, false)

    return this.ViewHolder(view)
  }

  override fun onBindViewHolder(
    holder: ViewHolder,
    position: Int
  ) {

    UIThread.checkIsUIThread()

    val item = this.spineElements[position]
    val normalIndex = item.index + 1

    val title = item.title ?: this.context.getString(
      R.string.audiobook_player_toc_chapter_n,
      normalIndex
    )

    holder.titleText.text = title
    holder.titleText.isEnabled = false

    holder.view.isEnabled = item.book.supportsStreaming

    var requiresDownload = false
    var failedDownload = false
    var downloading = false

    val view = holder.view
    view.tag = item
    view.setOnClickListener(this@PlayerTOCAdapter.listener)
    view.contentDescription =
      contentDescriptionOf(
        resources = context.resources,
        title = title,
        duration = item.duration,
        playing = position == this.currentSpineElement,
        requiresDownload = requiresDownload,
        failedDownload = failedDownload,
        downloading = downloading
      )

    if (position == this.currentSpineElement) {
      holder.isCurrent.visibility = VISIBLE
    } else {
      holder.isCurrent.visibility = INVISIBLE
    }
  }

  private fun contentDescriptionOf(
    resources: Resources,
    title: String,
    duration: Duration?,
    playing: Boolean,
    requiresDownload: Boolean,
    failedDownload: Boolean,
    downloading: Boolean
  ): String {

    val builder = StringBuilder(128)

    if (playing) {
      builder.append(resources.getString(R.string.audiobook_accessibility_toc_chapter_is_current))
      builder.append(" ")
    }

    builder.append(title)
    builder.append(". ")

    if (duration != null) {
      builder.append(resources.getString(R.string.audiobook_accessibility_toc_chapter_duration_is))
      builder.append(" ")
      builder.append(
        PlayerTimeStrings.hourMinuteSecondSpokenFromDuration(this.timeStrings, duration)
      )
      builder.append(". ")
    }

    if (requiresDownload) {
      builder.append(resources.getString(R.string.audiobook_accessibility_toc_chapter_requires_download))
      builder.append(".")
    }

    if (failedDownload) {
      builder.append(resources.getString(R.string.audiobook_accessibility_toc_chapter_failed_download))
      builder.append(".")
    }

    if (downloading) {
      builder.append(resources.getString(R.string.audiobook_accessibility_toc_chapter_downloading))
      builder.append(".")
    }

    return builder.toString()
  }

  private fun onConfirmCancelDownloading(item: PlayerSpineElementType) {
    val dialog =
      AlertDialog.Builder(this.context)
        .setCancelable(true)
        .setMessage(R.string.audiobook_part_download_stop_confirm)
        .setPositiveButton(
          R.string.audiobook_part_download_stop,
          { _: DialogInterface, _: Int -> item.downloadTask().cancel() }
        )
        .setNegativeButton(
          R.string.audiobook_part_download_continue,
          { _: DialogInterface, _: Int -> }
        )
        .create()
    dialog.show()
  }

  override fun getItemCount(): Int = this.spineElements.size

  fun setCurrentSpineElement(index: Int) {
    UIThread.checkIsUIThread()

    val previous = this.currentSpineElement
    this.currentSpineElement = index
    this.notifyItemChanged(index)

    if (previous != -1) {
      this.notifyItemChanged(previous)
    }
  }

  inner class ViewHolder(val view: View) : RecyclerView.ViewHolder(view) {

    val titleText: TextView =
      this.view.findViewById(R.id.player_toc_item_view_title)
    val isCurrent: ImageView =
      this.view.findViewById(R.id.player_toc_item_is_current)
  }
}
