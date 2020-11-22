package ru.spb.yakovlev.ratingbarsvg.ui.custom

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Shader
import android.graphics.drawable.*
import android.graphics.drawable.shapes.RectShape
import android.graphics.drawable.shapes.Shape
import android.util.AttributeSet
import android.view.Gravity
import android.view.ViewGroup
import androidx.appcompat.graphics.drawable.DrawableWrapper
import androidx.appcompat.widget.AppCompatRatingBar
import androidx.core.view.marginBottom
import androidx.core.view.marginEnd
import androidx.core.view.marginStart
import androidx.core.view.marginTop
import ru.spb.yakovlev.RatingBarSvg.R


/**
 * Custom RatingBar with vector drawable support.
 * Additional attributes can be set via constructor or
 * declared in values\attr.xml and set in layout file.
 * @param innerPadding The space between drawables, default is 2dp.
 * @param drawableHeight The height of the drawable, if not specified, then is taken from drawable settings.
 * @param drawableWidth The width of the drawable, if not specified, then is taken from drawable settings.
 */
class RatingBarSvg @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.ratingBarStyle,
    var innerPadding: Int = 2,
    var drawableHeight: Int = 0,
    var drawableWidth: Int = 0,
) : AppCompatRatingBar(context, attrs, defStyleAttr) {

    private var mSampleTile: Bitmap? = null
    private val drawableShape: Shape = RectShape()
    private var halfOfInnerPadding: Int

    init {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.RatingBarSvg)
        halfOfInnerPadding =
            (typedArray.getDimension(
                R.styleable.RatingBarSvg_innerPadding,
                innerPadding.toFloat()
            ) / 2).toInt()
        drawableHeight =
            typedArray.getDimension(R.styleable.RatingBarSvg_drawableHeight, 0f).toInt()
        drawableWidth = typedArray.getDimension(R.styleable.RatingBarSvg_drawableWidth, 0f).toInt()

        val drawable = tileify(progressDrawable, false) as LayerDrawable
        progressDrawable = drawable
        typedArray.recycle()
    }

    /**
     * Converts a drawable to a tiled version of itself. It will recursively
     * traverse layer and state list drawables.
     */
    @SuppressLint("RestrictedApi")
    private fun tileify(drawable: Drawable, clip: Boolean): Drawable {
        when (drawable) {

            is DrawableWrapper -> {
                val inner: Drawable? = drawable.wrappedDrawable
                inner?.let { drawable.wrappedDrawable = tileify(it, clip) }
                return drawable
            }

            is LayerDrawable -> {
                val numberOfLayers = drawable.numberOfLayers
                val outDrawables = arrayOfNulls<Drawable>(numberOfLayers)

                for (i in 0 until numberOfLayers) {
                    val id = drawable.getId(i)
                    outDrawables[i] = tileify(
                        drawable.getDrawable(i),
                        id == android.R.id.progress || id == android.R.id.secondaryProgress
                    )
                }

                val newBg = LayerDrawable(outDrawables)

                for (i in 0 until numberOfLayers) {
                    newBg.setId(i, drawable.getId(i))
                }
                return newBg
            }

            is BitmapDrawable -> {
                val tileBitmap = drawable.bitmap
                if (mSampleTile == null) mSampleTile = tileBitmap

                val bitmapShader = BitmapShader(
                    tileBitmap,
                    Shader.TileMode.REPEAT,
                    Shader.TileMode.CLAMP
                )

                val shapeDrawable = ShapeDrawable(drawableShape)
                    .apply {
                        paint.shader = bitmapShader
                        paint.colorFilter = drawable.paint.colorFilter
                    }

                return if (clip) ClipDrawable(
                    shapeDrawable, Gravity.START,
                    ClipDrawable.HORIZONTAL
                ) else shapeDrawable
            }

            else -> return tileify(getBitmapDrawableFromVectorDrawable(drawable), clip)
        }
    }

    private fun getBitmapDrawableFromVectorDrawable(drawable: Drawable): BitmapDrawable {
        if (drawableHeight == 0) drawableHeight = drawable.intrinsicHeight
        if (drawableWidth == 0) drawableWidth = drawable.intrinsicWidth

        val bitmap = Bitmap.createBitmap(
            drawableWidth + halfOfInnerPadding * 2, //dp between svg images
            drawableHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(
            halfOfInnerPadding,
            0,
            drawableWidth + halfOfInnerPadding,
            drawableHeight
        )
        drawable.draw(canvas)
        return BitmapDrawable(resources, bitmap)
    }

    @Synchronized
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var mHeight = measuredHeight

        val heightMS = if (heightMeasureSpec != MeasureSpec.EXACTLY) {
            mHeight = drawableHeight
            MeasureSpec.EXACTLY
        } else heightMeasureSpec


        super.onMeasure(widthMeasureSpec, heightMS)

        if (mSampleTile != null) {
            val width = mSampleTile!!.width * numStars
            setMeasuredDimension(
                resolveSizeAndState(width, widthMeasureSpec, 0),
                resolveSizeAndState(mHeight, heightMeasureSpec, 0)
            )
        }
        (layoutParams as ViewGroup.MarginLayoutParams).setMargins(
            marginStart - halfOfInnerPadding,
            marginTop,
            marginEnd,
            marginBottom
        )
    }
}