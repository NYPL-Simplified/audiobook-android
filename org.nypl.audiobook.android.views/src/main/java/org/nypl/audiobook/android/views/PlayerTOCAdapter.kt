package org.nypl.audiobook.android.views

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
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
    holder.durationText.text = this.periodFormatter.print(item.duration.toPeriod())

    val status = item.downloadStatus
    when (status) {
      is PlayerSpineElementNotDownloaded -> {
        if (!item.book.supportsStreaming) {
          holder.view.setBackgroundColor(holder.backgroundDisabled)
          holder.durationText.setTextColor(holder.textColorDisabled)
          holder.titleText.setTextColor(holder.textColorDisabled)
        } else {
          holder.view.setBackgroundColor(holder.backgroundColorNormal)
          holder.durationText.setTextColor(holder.textColorNormal)
          holder.titleText.setTextColor(holder.textColorNormal)
        }

        holder.operationButton.visibility = VISIBLE
        holder.operationButton.setImageResource(R.drawable.download)
        holder.operationButton.setOnClickListener({ item.downloadTask.fetch() })

        holder.downloadProgress.setOnClickListener({ })
        holder.downloadProgress.visibility = INVISIBLE
        holder.downloadProgress.progress = 0.0f
        holder.downloadProgressText.visibility = INVISIBLE
      }

      is PlayerSpineElementDownloading -> {
        if (!item.book.supportsStreaming) {
          holder.view.setBackgroundColor(holder.backgroundDisabled)
          holder.durationText.setTextColor(holder.textColorDisabled)
          holder.titleText.setTextColor(holder.textColorDisabled)
        } else {
          holder.view.setBackgroundColor(holder.backgroundColorNormal)
          holder.durationText.setTextColor(holder.textColorNormal)
          holder.titleText.setTextColor(holder.textColorNormal)
        }

        holder.operationButton.visibility = INVISIBLE
        holder.operationButton.setOnClickListener({ })

        holder.downloadProgress.setOnClickListener({ this.onConfirmCancelDownloading(item) })
        holder.downloadProgress.visibility = VISIBLE
        holder.downloadProgress.progress = status.percent.toFloat() * 0.01f
        holder.downloadProgressText.visibility = VISIBLE
        holder.downloadProgressText.text = status.percent.toString()
      }

      is PlayerSpineElementDownloaded -> {
        holder.view.setBackgroundColor(holder.backgroundColorNormal)
        holder.durationText.setTextColor(holder.textColorNormal)
        holder.titleText.setTextColor(holder.textColorNormal)

        holder.operationButton.visibility = VISIBLE
        holder.operationButton.setImageResource(R.drawable.trash)
        holder.operationButton.setOnClickListener({ this.onConfirmDelete(item) })

        holder.downloadProgress.setOnClickListener({ })
        holder.downloadProgress.visibility = INVISIBLE
        holder.downloadProgressText.visibility = INVISIBLE
      }

      is PlayerSpineElementDownloadFailed -> {
        holder.view.setBackgroundColor(holder.backgroundColorNormal)
        holder.durationText.setTextColor(holder.textColorNormal)
        holder.titleText.setTextColor(holder.textColorNormal)

        holder.operationButton.visibility = VISIBLE
        holder.operationButton.setImageResource(R.drawable.error)
        holder.operationButton.setOnClickListener({ item.downloadTask.delete() })

        holder.downloadProgress.setOnClickListener({ })
        holder.downloadProgress.visibility = INVISIBLE
        holder.downloadProgressText.visibility = INVISIBLE
      }
    }

    val view = holder.view
    view.tag = item
    view.setOnClickListener(this@PlayerTOCAdapter.listener)

    if (position == this.currentSpineElement) {
      holder.border.visibility = VISIBLE
    } else {
      holder.border.visibility = INVISIBLE
    }
  }

  private fun onConfirmDelete(item: PlayerSpineElementType) {
    val dialog =
      AlertDialog.Builder(this.context)
        .setCancelable(true)
        .setMessage(R.string.audiobook_part_delete_confirm)
        .setPositiveButton(
          R.string.audiobook_part_delete,
          { _: DialogInterface, _: Int -> item.downloadTask.delete() })
        .setNegativeButton(
          R.string.audiobook_part_delete_keep,
          { _: DialogInterface, _: Int -> })
        .create()
    dialog.show()
  }

  private fun onConfirmCancelDownloading(item: PlayerSpineElementType) {
    val dialog =
      AlertDialog.Builder(this.context)
        .setCancelable(true)
        .setMessage(R.string.audiobook_part_download_stop_confirm)
        .setPositiveButton(
          R.string.audiobook_part_download_stop,
          { _: DialogInterface, _: Int -> item.downloadTask.delete() })
        .setNegativeButton(
          R.string.audiobook_part_download_continue,
          { _: DialogInterface, _: Int -> })
        .create()
    dialog.show()
  }

  override fun getItemCount(): Int = this.spineElements.size

  fun setCurrentSpineElement(index: Int) {
    UIThread.checkIsUIThread()

    val previous = currentSpineElement
    this.currentSpineElement = index
    this.notifyItemChanged(index)

    if (previous != -1) {
      this.notifyItemChanged(previous)
    }
  }

  inner class ViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
    val textColorNormal =
      view.resources.getColor(R.color.audiobook_player_toc_spine_element_fg_normal)
    val textColorDisabled =
      view.resources.getColor(R.color.audiobook_player_toc_spine_element_fg_disabled)

    val backgroundColorNormal =
      view.resources.getColor(R.color.audiobook_player_toc_spine_element_bg_normal)
    val backgroundDisabled =
      view.resources.getColor(R.color.audiobook_player_toc_spine_element_bg_disabled)

    val titleText: TextView =
      this.view.findViewById(R.id.player_toc_item_view_title)
    val border: ImageView =
      this.view.findViewById(R.id.player_toc_item_view_border)
    val durationText: TextView =
      this.view.findViewById(R.id.player_toc_item_view_duration)
    val downloadProgress: PlayerCircularProgressView =
      this.view.findViewById(R.id.player_toc_item_view_progress)
    val downloadProgressText: TextView =
      this.view.findViewById(R.id.player_toc_item_view_progress_text)
    val operationButton: ImageView =
      this.view.findViewById(R.id.player_toc_item_view_operation)

    init {
      this.downloadProgress.thickness = 8.0f
      this.downloadProgress.color = context.resources.getColor(R.color.audiobook_player_progress_color)
    }
  }
}
