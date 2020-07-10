package org.librarysimplified.audiobook.views

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff.Mode.CLEAR
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

/**
 * A circular progress view. Renders similarly to the circular android indefinite progress
 * spinner with the main difference that the circle does not perpetually animate, and progressively
 * more of the circle is drawn as the progress value nears {@code 1.0}.
 */

class PlayerCircularProgressView(context: Context, attrs: AttributeSet) : View(context, attrs) {

  private val defaultBgColor = "#cccccc"
  private val defaultFgColor = "#ff00ff"

  private val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    this.color = Color.parseColor(defaultFgColor)
    this.isAntiAlias = true
  }

  private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    this.color = Color.parseColor(defaultBgColor)
    this.isAntiAlias = true
  }

  private val cutPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    this.color = Color.parseColor("#00000000")
    this.xfermode = PorterDuffXfermode(CLEAR)
    this.isAntiAlias = true
  }

  private val emptyPaint = Paint()
  private val rectOuter = RectF()
  private val rectInner = RectF()
  private lateinit var image: Bitmap
  private lateinit var imageCanvas: Canvas

  override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
    this.image = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_4444)
    this.imageCanvas = Canvas(this.image)
    this.rectOuter.set(0.0f, 0.0f, w.toFloat(), h.toFloat())
    this.updateInnerRect(w, h)
  }

  private fun updateInnerRect(w: Int, h: Int) {
    this.rectInner.set(
      this.thicknessValue,
      this.thicknessValue,
      w.toFloat() - this.thicknessValue,
      h.toFloat() - this.thicknessValue
    )
  }

  private var progressValue = 0.0f

  /**
   * The current progress value in the range {@code [0.0, 1.0]}
   */

  var progress: Float
    get() =
      this.progressValue
    set(value) {
      this.progressValue = Math.min(1.0f, Math.max(0.0f, value))
      this.invalidate()
    }

  private var colorValue: Int = Color.parseColor(defaultFgColor)

  /**
   * The current colour value.
   *
   * @see Color.parseColor
   */

  var color: Int
    get() =
      this.colorValue
    set(value) {
      this.colorValue = value
      this.arcPaint.color = value
      this.invalidate()
    }

  private var thicknessValue = 16.0f

  /**
   * The current thickness in pixels.
   */

  var thickness: Float
    get() =
      this.thicknessValue
    set(value) {
      this.thicknessValue = Math.max(1.0f, value)
      this.updateInnerRect(this.width, this.height)
      this.invalidate()
    }

  private var unfilledColorValue: Int = Color.parseColor(defaultBgColor)

  /**
   * The current background colour value.
   *
   * @see Color.parseColor
   */

  var unfilledColor: Int
    get() =
      this.colorValue
    set(value) {
      this.unfilledColorValue = value
      this.bgPaint.color = value
      this.invalidate()
    }

  override fun onDraw(canvas: Canvas?) {
    super.onDraw(canvas)

    if (canvas != null) {
      this.imageCanvas.drawColor(Color.TRANSPARENT, CLEAR)

      this.imageCanvas.drawArc(
        this.rectOuter,
        -90.0f,
        360.0f,
        true,
        this.bgPaint
      )

      this.imageCanvas.drawArc(
        this.rectOuter,
        -90.0f,
        this.progress * 360.0f,
        true,
        this.arcPaint
      )

      this.imageCanvas.drawArc(
        this.rectInner,
        -90.0f,
        360.0f,
        true,
        this.cutPaint
      )

      canvas.drawBitmap(this.image, 0.0f, 0.0f, this.emptyPaint)
    }
  }
}
