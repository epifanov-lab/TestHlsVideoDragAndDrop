package com.example.testvideodraganddrop.player;

import android.content.Context;
import android.graphics.Point;
import android.media.MediaCodec;
import android.media.MediaCrypto;
import android.media.MediaFormat;
import android.os.Handler;

import androidx.annotation.Nullable;

import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.drm.DrmSessionManager;
import com.google.android.exoplayer2.drm.FrameworkMediaCrypto;
import com.google.android.exoplayer2.mediacodec.MediaCodecInfo;
import com.google.android.exoplayer2.mediacodec.MediaCodecSelector;
import com.google.android.exoplayer2.mediacodec.MediaCodecUtil;
import com.google.android.exoplayer2.video.MediaCodecVideoRenderer;
import com.google.android.exoplayer2.video.VideoRendererEventListener;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author Konstantin Epifanov
 * @since 06.08.2019
 */
public class CustomVideoRenderer extends MediaCodecVideoRenderer {

  private static final String TAG = "MediaCodecVideoRenderer";
  private static final String KEY_CROP_LEFT = "crop-left";
  private static final String KEY_CROP_RIGHT = "crop-right";
  private static final String KEY_CROP_BOTTOM = "crop-bottom";
  private static final String KEY_CROP_TOP = "crop-top";

  private final Supplier<Consumer<Point>> mOnOutputFormatChanged;

  public CustomVideoRenderer(Context context, MediaCodecSelector mediaCodecSelector, Supplier<Consumer<Point>> onOutputFormatChanged) {
    super(context, mediaCodecSelector);
    mOnOutputFormatChanged = onOutputFormatChanged;
  }

  public CustomVideoRenderer(Context context, MediaCodecSelector mediaCodecSelector, long allowedJoiningTimeMs, Supplier<Consumer<Point>> onOutputFormatChanged) {
    super(context, mediaCodecSelector, allowedJoiningTimeMs);
    mOnOutputFormatChanged = onOutputFormatChanged;
  }

  public CustomVideoRenderer(Context context, MediaCodecSelector mediaCodecSelector, long allowedJoiningTimeMs, @Nullable Handler eventHandler, @Nullable VideoRendererEventListener eventListener, int maxDroppedFramesToNotify, Supplier<Consumer<Point>> onOutputFormatChanged) {
    super(context, mediaCodecSelector, allowedJoiningTimeMs, eventHandler, eventListener, maxDroppedFramesToNotify);
    mOnOutputFormatChanged = onOutputFormatChanged;
  }

  public CustomVideoRenderer(Context context, MediaCodecSelector mediaCodecSelector, long allowedJoiningTimeMs, @Nullable DrmSessionManager<FrameworkMediaCrypto> drmSessionManager, boolean playClearSamplesWithoutKeys, @Nullable Handler eventHandler, @Nullable VideoRendererEventListener eventListener, int maxDroppedFramesToNotify, Supplier<Consumer<Point>> onOutputFormatChanged) {
    super(context, mediaCodecSelector, allowedJoiningTimeMs, drmSessionManager, playClearSamplesWithoutKeys, eventHandler, eventListener, maxDroppedFramesToNotify);
    mOnOutputFormatChanged = onOutputFormatChanged;
  }

  @Override
  protected void onOutputFormatChanged(MediaCodec codec, MediaFormat outputFormat) {
    super.onOutputFormatChanged(codec, outputFormat);

    boolean hasCrop = outputFormat.containsKey(KEY_CROP_RIGHT)
      && outputFormat.containsKey(KEY_CROP_LEFT) && outputFormat.containsKey(KEY_CROP_BOTTOM)
      && outputFormat.containsKey(KEY_CROP_TOP);
    int width =
      hasCrop
        ? outputFormat.getInteger(KEY_CROP_RIGHT) - outputFormat.getInteger(KEY_CROP_LEFT) + 1
        : outputFormat.getInteger(MediaFormat.KEY_WIDTH);
    int height =
      hasCrop
        ? outputFormat.getInteger(KEY_CROP_BOTTOM) - outputFormat.getInteger(KEY_CROP_TOP) + 1
        : outputFormat.getInteger(MediaFormat.KEY_HEIGHT);

    System.out.println("CustomVideoRenderer.onOutputFormatChanged: SIZE " + width + "x" + height);
    mOnOutputFormatChanged.get().accept(new Point(width, height));
  }

  @Override
  protected void configureCodec(MediaCodecInfo codecInfo, MediaCodec codec, Format format, MediaCrypto crypto, float codecOperatingRate) throws MediaCodecUtil.DecoderQueryException {
    try {
      super.configureCodec(codecInfo, codec, format, crypto, codecOperatingRate);
    } catch (Throwable e) {
      e.printStackTrace();
    }
  }

}
