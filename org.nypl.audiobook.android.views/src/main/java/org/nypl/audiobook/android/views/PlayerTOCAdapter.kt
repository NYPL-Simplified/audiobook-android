package org.nypl.audiobook.android.views

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.graphics.PorterDuff
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import org.joda.time.format.PeriodFormatter
import org.joda.time.format.PeriodFormatterBuilder
import org.nypl.audiobook.android.api.PlayerSpineElementDownloadStatus.PlayerSpineElementDownloadFailed
import org.nypl.audiobook.android.api.PlayerSpineElementDownloadStatus.PlayerSpineElementDownloaded
import org.nypl.audiobook.android.api.PlayerSpineElementDownloadStatus.PlayerSpineElementDownloading
import org.nypl.audiobook.android.api.PlayerSpineElementDownloadStatus.PlayerSpineElementNotDownloaded
import org.nypl.audiobook.android.api.PlayerSpineElementType

/**
 * A Recycler view adapter used to display and control the table of contents.
 */

class PlayerTOCAdapter(
  private val context: Context,
  private val spineElements: List<PlayerSpineElementType>,
  private val parameters: PlayerTOCFragmentParameters,
  private val onSelect: (PlayerSpineElementType) -> Unit)
  : RecyclerView.Adapter<PlayerTOCAdapter.ViewHolder>() {

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

  init {
    this.listener = View.OnClickListener { v -> this.onSelect(v.tag as PlayerSpineElementType) }
  }

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int): ViewHolder {

    UIThread.checkIsUIThread()

    val view =
      LayoutInflater.from(parent.context)
        .inflate(R.layout.player_toc_item_view, parent, false)

    return this.ViewHolder(view)
  }

  override fun onBindViewHolder(
    holder: ViewHolder,
    position: Int) {

    UIThread.checkIsUIThread()

    val item = this.spineElements[position]

    holder.titleText.text = item.title

    if (item.book.supportsStreaming) {
      holder.titleText.setTextColor(holder.textColorNormal)
      holder.view.setBackgroundColor(holder.backgroundColorNormal)
    } else {
      holder.titleText.setTextColor(holder.textColorDisabled)
      holder.view.setBackgroundColor(holder.backgroundDisabled)
    }

    val normalIndex = item.index + 1
    val status = item.downloadStatus
    when (status) {
      is PlayerSpineElementNotDownloaded -> {
        holder.buttonsDownloaded.visibility = INVISIBLE
        holder.buttonsDownloading.visibility = INVISIBLE
        holder.buttonsDownloadFailed.visibility = INVISIBLE

        if (item.book.supportsStreaming) {
          holder.buttonsNotDownloadedNotStreamable.visibility = INVISIBLE
          holder.buttonsNotDownloadedStreamable.visibility = VISIBLE
          holder.notDownloadedStreamableRefresh.setOnClickListener { item.downloadTask.fetch() }
          holder.notDownloadedStreamableRefresh.contentDescription =
            this.context.getString(
              R.string.audiobook_accessibility_toc_download,
              normalIndex)
        } else {
          holder.buttonsNotDownloadedNotStreamable.visibility = VISIBLE
          holder.buttonsNotDownloadedStreamable.visibility = INVISIBLE
          holder.notDownloadedNotStreamableRefresh.setOnClickListener { item.downloadTask.fetch() }
          holder.notDownloadedStreamableRefresh.contentDescription =
            this.context.getString(
              R.string.audiobook_accessibility_toc_download,
              normalIndex)
        }
      }

      is PlayerSpineElementDownloading -> {
        holder.buttonsDownloaded.visibility = INVISIBLE
        holder.buttonsDownloading.visibility = VISIBLE
        holder.buttonsDownloadFailed.visibility = INVISIBLE
        holder.buttonsNotDownloadedStreamable.visibility = INVISIBLE
        holder.buttonsNotDownloadedNotStreamable.visibility = INVISIBLE

        holder.downloadingProgress.setOnClickListener { this.onConfirmCancelDownloading(item) }
        holder.downloadingProgress.contentDescription =
          this.context.getString(R.string.audiobook_accessibility_toc_progress, normalIndex, status.percent)
        holder.downloadingProgress.visibility = VISIBLE
        holder.downloadingProgress.progress = status.percent.toFloat() * 0.01f
      }

      is PlayerSpineElementDownloaded -> {
        holder.buttonsDownloaded.visibility = VISIBLE
        holder.buttonsDownloading.visibility = INVISIBLE
        holder.buttonsDownloadFailed.visibility = INVISIBLE
        holder.buttonsNotDownloadedStreamable.visibility = INVISIBLE
        holder.buttonsNotDownloadedNotStreamable.visibility = INVISIBLE

        holder.titleText.setTextColor(holder.textColorNormal)
        holder.view.setBackgroundColor(holder.backgroundColorNormal)

        holder.downloadedDurationText.text = this.periodFormatter.print(item.duration.toPeriod())
      }

      is PlayerSpineElementDownloadFailed -> {
        holder.buttonsDownloaded.visibility = INVISIBLE
        holder.buttonsDownloading.visibility = INVISIBLE
        holder.buttonsDownloadFailed.visibility = VISIBLE
        holder.buttonsNotDownloadedStreamable.visibility = INVISIBLE
        holder.buttonsNotDownloadedNotStreamable.visibility = INVISIBLE

        holder.downloadFailedRefresh.setOnClickListener {
          item.downloadTask.cancel()
          item.downloadTask.fetch()
        }

        holder.downloadFailedRefresh.contentDescription =
          this.context.getString(R.string.audiobook_accessibility_toc_retry, normalIndex)
      }
    }

    val view = holder.view
    view.tag = item
    view.setOnClickListener(this@PlayerTOCAdapter.listener)

    if (position == this.currentSpineElement) {
      holder.isCurrent.visibility = VISIBLE
    } else {
      holder.isCurrent.visibility = INVISIBLE
    }
  }

  private fun onConfirmCancelDownloading(item: PlayerSpineElementType) {
    val dialog =
      AlertDialog.Builder(this.context)
        .setCancelable(true)
        .setMessage(R.string.audiobook_part_download_stop_confirm)
        .setPositiveButton(
          R.string.audiobook_part_download_stop,
          { _: DialogInterface, _: Int -> item.downloadTask.cancel() })
        .setNegativeButton(
          R.string.audiobook_part_download_continue,
          { _: DialogInterface, _: Int -> })
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
    val textColorNormal =
      this.view.resources.getColor(R.color.audiobook_player_toc_spine_element_fg_normal)
    val textColorDisabled =
      this.view.resources.getColor(R.color.audiobook_player_toc_spine_element_fg_disabled)

    val backgroundColorNormal =
      this.view.resources.getColor(R.color.audiobook_player_toc_spine_element_bg_normal)
    val backgroundDisabled =
      this.view.resources.getColor(R.color.audiobook_player_toc_spine_element_bg_disabled)

    val buttons =
      this.view.findViewById<ViewGroup>(R.id.player_toc_end_controls)
    val buttonsDownloadFailed =
      this.buttons.findViewById<ViewGroup>(R.id.player_toc_item_buttons_error)
    val buttonsDownloaded =
      this.buttons.findViewById<ViewGroup>(R.id.player_toc_item_buttons_downloaded)
    val buttonsNotDownloadedNotStreamable =
      this.buttons.findViewById<ViewGroup>(R.id.player_toc_item_buttons_not_downloaded_not_streamable)
    val buttonsNotDownloadedStreamable =
      this.buttons.findViewById<ViewGroup>(R.id.player_toc_item_buttons_not_downloaded_streamable)
    val buttonsDownloading =
      this.buttons.findViewById<ViewGroup>(R.id.player_toc_item_buttons_downloading)

    val titleText: TextView =
      this.view.findViewById(R.id.player_toc_item_view_title)
    val isCurrent: ImageView =
      this.view.findViewById(R.id.player_toc_item_is_current)

    val downloadFailedErrorIcon: ImageView =
      this.buttonsDownloadFailed.findViewById(R.id.player_toc_item_download_failed_error_icon)
    val downloadFailedRefresh: ImageView =
      this.buttonsDownloadFailed.findViewById(R.id.player_toc_item_download_failed_refresh)

    val downloadedDurationText: TextView =
      this.buttonsDownloaded.findViewById(R.id.player_toc_item_downloaded_duration)

    val notDownloadedStreamableRefresh: ImageView =
      this.buttonsNotDownloadedStreamable.findViewById(
        R.id.player_toc_item_not_downloaded_streamable_refresh)
    val notDownloadedStreamableProgress: PlayerCircularProgressView =
      this.buttonsNotDownloadedStreamable.findViewById(
        R.id.player_toc_item_not_downloaded_streamable_progress)

    val notDownloadedNotStreamableRefresh: ImageView =
      this.buttonsNotDownloadedNotStreamable.findViewById(
        R.id.player_toc_item_not_downloaded_not_streamable_refresh)

    val downloadingProgress: PlayerCircularProgressView =
      this.buttonsDownloading.findViewById(R.id.player_toc_item_downloading_progress)

    init {
      this.downloadingProgress.thickness = 8.0f
      this.downloadingProgress.color = this@PlayerTOCAdapter.parameters.primaryColor

      this.notDownloadedStreamableProgress.thickness = 8.0f
      this.notDownloadedStreamableProgress.color = this@PlayerTOCAdapter.parameters.primaryColor

      this.downloadFailedErrorIcon.setColorFilter(
        this@PlayerTOCAdapter.parameters.primaryColor, PorterDuff.Mode.MULTIPLY)
      this.downloadFailedRefresh.setColorFilter(
        this@PlayerTOCAdapter.parameters.primaryColor, PorterDuff.Mode.MULTIPLY)

      this.notDownloadedStreamableRefresh.setColorFilter(
        this@PlayerTOCAdapter.parameters.primaryColor, PorterDuff.Mode.MULTIPLY)

      this.notDownloadedNotStreamableRefresh.setColorFilter(
        this@PlayerTOCAdapter.parameters.primaryColor, PorterDuff.Mode.MULTIPLY)

      this.isCurrent.setColorFilter(
        this@PlayerTOCAdapter.parameters.primaryColor, PorterDuff.Mode.MULTIPLY)
    }
  }
}
