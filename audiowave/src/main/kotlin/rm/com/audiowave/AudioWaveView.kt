package rm.com.audiowave

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.animation.OvershootInterpolator
import java.util.*
import kotlin.collections.ArrayList

class AudioWaveView : View {

    constructor(context: Context?) : super(context) {
        setWillNotDraw(false)
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        setWillNotDraw(false)
        inflateAttrs(attrs)
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs,
            defStyleAttr) {
        setWillNotDraw(false)
        inflateAttrs(attrs)
    }

    var onProgressListener: OnProgressListener? = null

    var onProgressChanged: (Float, Boolean) -> Unit = { _, _ -> Unit }

    var onStartTracking: (Float) -> Unit = {}

    var onStopTracking: (Float) -> Unit = {}

    val listaLineas = ArrayList<ShortArray>()

    var chunkHeight: Int = 0
        get() = if (field == 0) h else Math.abs(field)
        set(value) {
            field = value
            redrawData()
        }

    var chunkWidth: Int = dip(2)
        set(value) {
            field = Math.abs(value)
            redrawData()
        }

    var chunkSpacing: Int = dip(1)
        set(value) {
            field = Math.abs(value)
            redrawData()
        }

    var chunkRadius: Int = 0
        set(value) {
            field = Math.abs(value)
            redrawData()
        }

    var minChunkHeight: Int = dip(2)
        set(value) {
            field = Math.abs(value)
            redrawData()
        }

    var waveColor: Int = Color.BLACK
        set(value) {
            wavePaint = smoothPaint(Color.parseColor("#d3d3d3"))
            waveFilledPaint = filterPaint(Color.BLACK)
            redrawData()
        }


    var progress: Float = 0F
        set(value) {
            require(value in 0..100) { "Progress must be in 0..100" }

            field = Math.abs(value)

            onProgressListener?.onProgressChanged(field, isTouched)
            onProgressChanged(field, isTouched)

            postInvalidate()
        }

    var scaledData: ByteArray = byteArrayOf()
        set(value) {
            field = if (value.size <= chunksCount) {
                ByteArray(chunksCount).paste(value)
            } else {
                value
            }

            redrawData()
        }

    var expansionDuration: Long = 400
        set(value) {
            field = Math.max(400, value)
            expansionAnimator.duration = field
        }

    var isExpansionAnimated: Boolean = true

    var isTouchable = true

    var isTouched = false
        private set

    val chunksCount: Int
        get() = w / chunkStep

    private val chunkStep: Int
        get() = chunkWidth + chunkSpacing

    private val centerY: Int
        get() = h / 2

    private val progressFactor: Float
        get() = progress / 100F

    private val initialDelay: Long = 50

    private val expansionAnimator = ValueAnimator.ofFloat(0.0F, 1.0F).apply {
        duration = expansionDuration
        interpolator = OvershootInterpolator()
        addUpdateListener {
            redrawData(factor = it.animatedFraction)
        }
    }

    private var wavePaint = smoothPaint(Color.BLACK.withAlpha(255))
    private var waveFilledPaint = filterPaint(waveColor)
    private var waveBitmap: Bitmap? = null

    private var w: Int = 0
    private var h: Int = 0

    private var mFillPaint: Paint = Paint()
    private var mProgressPaint: Paint = Paint()

    init {
        mFillPaint.style = Paint.Style.STROKE
        mFillPaint.isAntiAlias = true
        mFillPaint.color = Color.GRAY
        mFillPaint.strokeWidth = 0f

        mProgressPaint.style = Paint.Style.STROKE
        mProgressPaint.isAntiAlias = true
        mProgressPaint.color = Color.BLACK
        mProgressPaint.strokeWidth = 0f
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        val max = Short.MAX_VALUE
        val centerY = height / 2

        Log.e("WAVE-NOR", "llenarLista ->-----------------------------------:")
        Log.e("WAVE-NOR", "llenarLista -> : centerY: $centerY")

        if (!listaLineas.isEmpty()){

            val pixelProgress = width * progress / 100
            for (i in 0 until pixelProgress.toInt() - 1){
                val linea = listaLineas[i]
                val top = centerY - ((linea[0].toFloat() / max.toFloat()) * centerY.toFloat())
                val bottom = centerY - ((linea[1].toFloat() / max.toFloat()) * centerY.toFloat())
                canvas!!.drawLine(i.toFloat(), bottom, i.toFloat(), top, mProgressPaint)
            }

            for (i in pixelProgress.toInt() until listaLineas.size){
                val linea = listaLineas[i]
                val top = centerY - ((linea[0].toFloat() / max.toFloat()) * centerY.toFloat())
                val bottom = centerY - ((linea[1].toFloat() / max.toFloat()) * centerY.toFloat())
                canvas!!.drawLine(i.toFloat(), bottom, i.toFloat(), top, mFillPaint)
            }

            /*listaLineas.forEachIndexed { i, linea ->
                val top = centerY - ((linea[0].toFloat() / max.toFloat()) * centerY.toFloat())
                val bottom = centerY - ((linea[1].toFloat() / max.toFloat()) * centerY.toFloat())

                *//*Log.e("WAVE-NOR", "llenarLista -> : linea: ${(linea[0].toFloat() / max.toFloat())}")
                Log.e("WAVE-NOR", "llenarLista -> : top: $top")
                Log.e("WAVE-NOR", "llenarLista -> : bottom: $bottom")*//*
                *//*canvas!!.drawRect(
                        rectFOf(
                                left = i,
                                top = top.toInt(),
                                right = i+1,
                                bottom = bottom.toInt()
                        ),
                        mFillPaint
                )*//*

                canvas!!.drawLine(i.toFloat(), bottom.toFloat(), i.toFloat(), top.toFloat(), mFillPaint)
            }*/
        }

    }

    // suppressed here since we allocate only once,
    // when the wave bounds have been just calculated(it happens once)
    @SuppressLint("DrawAllocation")
    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        w = right - left
        h = bottom - top

        if (waveBitmap.fits(w, h)) {
            return
        }

        if (changed) {
            waveBitmap.safeRecycle()
            waveBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)

            // absolutely ridiculous hack to draw wave in RecyclerView items
            scaledData = when (scaledData.size) {
                0 -> byteArrayOf()
                else -> scaledData
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        event ?: return super.onTouchEvent(event)

        if (!isTouchable || !isEnabled) {
            return false
        }

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                isTouched = true
                progress = event.toProgress()

                // these paired calls look ugly, but we need them for Java
                onProgressListener?.onStartTracking(progress)
                onStartTracking(progress)

                return true
            }
            MotionEvent.ACTION_MOVE -> {
                isTouched = true
                progress = event.toProgress()
                return true
            }
            MotionEvent.ACTION_UP -> {
                isTouched = false
                onProgressListener?.onStopTracking(progress)
                onStopTracking(progress)
                return false
            }
            else -> {
                isTouched = false
                return super.onTouchEvent(event)
            }
        }
    }

    @JvmOverloads
    fun setRawData(raw: ShortArray) {
        llenarLista(raw, width)
        redrawData()
    }

    fun llenarLista(data: ShortArray, sampleSize: Int) {

        Log.e("WAVE-NOR", "llenarLista -> : data: " + data.size)
        Log.e("WAVE-NOR", "llenarLista -> : sampleSize: $sampleSize")

        val groupSize = data.size / sampleSize

        for (i in 0 until sampleSize) {
            val group = Arrays.copyOfRange(data, i * groupSize, Math.min((i + 1) * groupSize, data.size))

            // Fin min & max values
            var min = java.lang.Short.MAX_VALUE
            var max = java.lang.Short.MIN_VALUE
            for (a in group) {
                min = Math.min(min.toInt(), a.toInt()).toShort()
                max = Math.max(max.toInt(), a.toInt()).toShort()
            }
            listaLineas.add(shortArrayOf(max, min))
        }
    }

    private fun MotionEvent.toProgress() = this@toProgress.x.clamp(0F, w.toFloat()) / w * 100F

    private fun redrawData(canvas: Canvas? = waveBitmap?.inCanvas(), factor: Float = 1.0F) {

        val max = Short.MAX_VALUE

        val centerY = height / 2
        listaLineas.forEachIndexed { i, linea ->
            val top = centerY - ((linea[0].toFloat() / max.toFloat()) * centerY.toFloat())
            val bottom = centerY - ((linea[1].toFloat() / max.toFloat()) * centerY.toFloat())

            canvas!!.drawRect(
                    rectFOf(
                            left = i,
                            top = top.toInt(),
                            right = i+1,
                            bottom = bottom.toInt()
                    ),
                    waveFilledPaint
            )
        }

        /* scaledData.forEachIndexed { i, chunk ->
           val chunkHeight = ((chunk.abs.toFloat() / Byte.MAX_VALUE) * chunkHeight).toInt()
           val clampedHeight = Math.max(chunkHeight, minChunkHeight)
           val heightDiff = (clampedHeight - minChunkHeight).toFloat()
           val animatedDiff = (heightDiff * factor).toInt()

           canvas.drawRoundRect(
               rectFOf(
                   left = chunkSpacing / 2 + i * chunkStep,
                   top = centerY - minChunkHeight - animatedDiff,
                   right = chunkSpacing / 2 + i * chunkStep + chunkWidth,
                   bottom = centerY + minChunkHeight + animatedDiff
               ),
               chunkRadius.toFloat(),
               chunkRadius.toFloat(),
               wavePaint
           )
         }*/

        postInvalidate()
        invalidate()
    }

    private fun animateExpansion() {
        expansionAnimator.start()
    }

    private fun inflateAttrs(attrs: AttributeSet?) {
        val resAttrs = context.theme.obtainStyledAttributes(
                attrs,
                R.styleable.AudioWaveView,
                0,
                0
        ) ?: return

        with(resAttrs) {
            chunkHeight = getDimensionPixelSize(R.styleable.AudioWaveView_chunkHeight, chunkHeight)
            chunkWidth = getDimensionPixelSize(R.styleable.AudioWaveView_chunkWidth, chunkWidth)
            chunkSpacing = getDimensionPixelSize(R.styleable.AudioWaveView_chunkSpacing,
                    chunkSpacing)
            minChunkHeight = getDimensionPixelSize(R.styleable.AudioWaveView_minChunkHeight,
                    minChunkHeight)
            chunkRadius = getDimensionPixelSize(R.styleable.AudioWaveView_chunkRadius, chunkRadius)
            isTouchable = getBoolean(R.styleable.AudioWaveView_touchable, isTouchable)
            waveColor = getColor(R.styleable.AudioWaveView_waveColor, waveColor)
            progress = getFloat(R.styleable.AudioWaveView_progress, progress)
            isExpansionAnimated = getBoolean(R.styleable.AudioWaveView_animateExpansion,
                    isExpansionAnimated)
            recycle()
        }
    }
}