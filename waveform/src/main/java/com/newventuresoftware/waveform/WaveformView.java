package com.newventuresoftware.waveform;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.v4.content.ContextCompat;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.newventuresoftware.waveform.utils.SamplingUtils;
import com.newventuresoftware.waveform.utils.TextUtils;

import java.util.ArrayList;

/**
 * TODO: document your custom view class.
 */
public class WaveformView extends View {
    public static final int MODE_PLAYBACK = 2;

    private TextPaint mTextPaint;
    private Paint mStrokePaint, mFillPaint, mMarkerPaint;

    // Used in draw
    private Rect drawRect;

    private int width, height;
    private int mMode, mSampleRate, mChannels;
    private short[] mSamples;
    private short[][] extremes;
    private ArrayList<short[]> listaLineas = new ArrayList<>();
    float centerY;
    float max = Short.MAX_VALUE;

    public WaveformView(Context context) {
        super(context);
        init(context, null, 0);
    }

    public WaveformView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0);
    }

    public WaveformView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs, defStyle);
    }

    private void init(Context context, AttributeSet attrs, int defStyle) {
        // Load attributes
        final TypedArray a = getContext().obtainStyledAttributes(
                attrs, R.styleable.WaveformView, defStyle, 0);

        mMode = a.getInt(R.styleable.WaveformView_mode, MODE_PLAYBACK);

        float strokeThickness = a.getFloat(R.styleable.WaveformView_waveformStrokeThickness, 1f);
        int mStrokeColor = a.getColor(R.styleable.WaveformView_waveformColor,
                ContextCompat.getColor(context, R.color.default_waveform));
        int mFillColor = a.getColor(R.styleable.WaveformView_waveformFillColor,
                ContextCompat.getColor(context, R.color.default_waveformFill));
        int mMarkerColor = a.getColor(R.styleable.WaveformView_playbackIndicatorColor,
                ContextCompat.getColor(context, R.color.default_playback_indicator));
        int mTextColor = a.getColor(R.styleable.WaveformView_timecodeColor,
                ContextCompat.getColor(context, R.color.default_timecode));

        a.recycle();

        mTextPaint = new TextPaint();
        mTextPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setTextAlign(Paint.Align.CENTER);
        mTextPaint.setColor(mTextColor);
        mTextPaint.setTextSize(TextUtils.getFontSize(getContext(),
                android.R.attr.textAppearanceSmall));

        mStrokePaint = new Paint();
        mStrokePaint.setColor(mStrokeColor);
        mStrokePaint.setStyle(Paint.Style.STROKE);
        mStrokePaint.setStrokeWidth(strokeThickness);
        mStrokePaint.setAntiAlias(true);

        mFillPaint = new Paint();
        mFillPaint.setStyle(Paint.Style.STROKE);
        mFillPaint.setAntiAlias(true);
        mFillPaint.setColor(mFillColor);
        mFillPaint.setStrokeWidth(0);

        mMarkerPaint = new Paint();
        mMarkerPaint.setStyle(Paint.Style.STROKE);
        mMarkerPaint.setStrokeWidth(0);
        mMarkerPaint.setAntiAlias(true);
        mMarkerPaint.setColor(mMarkerColor);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        width = getMeasuredWidth();
        height = getMeasuredHeight();
        drawRect = new Rect(0, 0, width, height);

    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // draw maximums
        if (extremes != null) {
            for (int x = 0; x < width; x++) {
                short sample = extremes[x][0];
                float y = centerY - ((sample / max) * centerY);

                canvas.drawLine(x, centerY, x, y, mFillPaint);
            }

            for (int x = 0; x < width / 2; x++) {
                short sample = extremes[x][0];
                float y = centerY - ((sample / max) * centerY);

                canvas.drawLine(x, centerY, x, y, mStrokePaint);
            }

            for (int x = width - 1; x >= 0; x--) {
                short sample = extremes[x][1];
                float y = centerY - ((sample / max) * centerY);

                canvas.drawLine(x, centerY, x, y, mFillPaint);
            }
        }

        if (!listaLineas.isEmpty()) {

            for (int x = 0; x < listaLineas.size(); x++) {
                short[] linea = listaLineas.get(x);
                float y1 = centerY - ((linea[0] / max) * centerY);
                float y2 = centerY - ((linea[1] / max) * centerY);

                // top
                canvas.drawLine(x, y2, x, y1, mFillPaint);

            }

            /*for (int x = 0; x < width; x++) {
                short sample = extremes[x][0];
                float y = centerY - ((sample / max) * centerY);

                canvas.drawLine(x,centerY, x,y, mFillPaint);
            }

            for (int x = 0; x < width / 2; x++) {
                short sample = extremes[x][0];
                float y = centerY - ((sample / max) * centerY);

                canvas.drawLine(x,centerY, x,y, mStrokePaint);
            }

            for (int x = width - 1; x >= 0; x--) {
                short sample = extremes[x][1];
                float y = centerY - ((sample / max) * centerY);

                canvas.drawLine(x,centerY, x,y, mFillPaint);
            }*/
        }

    }

    public short[] getSamples() {
        return mSamples;
    }

    public void setSamples(short[] samples) {
        mSamples = samples;
        onSamplesChanged();
    }


    public int getChannels() {
        return mChannels;
    }

    public void setChannels(int channels) {
        mChannels = channels;
    }


    private void onSamplesChanged() {
        Log.e("SAMPLES SIZE", "samples size: " + mSamples.length);
        if (mMode == MODE_PLAYBACK) {
            centerY = height / 2f;
            if (mSamples.length > 0 && width > 0) {
                listaLineas = SamplingUtils.getLineas(mSamples, width);
                //extremes = SamplingUtils.getExtremes(mSamples, width);
            }
        }
    }


}
