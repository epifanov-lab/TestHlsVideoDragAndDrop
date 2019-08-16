package com.example.testvideodraganddrop.player;

import android.content.Context;
import android.graphics.Point;
import android.os.Handler;
import android.os.Looper;
import android.view.TextureView;

import androidx.annotation.Nullable;

import com.example.testvideodraganddrop.view.player_texture_view.PlayerTextureView;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.Renderer;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.analytics.AnalyticsCollector;
import com.google.android.exoplayer2.drm.DrmSessionManager;
import com.google.android.exoplayer2.drm.FrameworkMediaCrypto;
import com.google.android.exoplayer2.mediacodec.MediaCodecSelector;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.util.Util;
import com.google.android.exoplayer2.video.MediaCodecVideoRenderer;
import com.google.android.exoplayer2.video.VideoRendererEventListener;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.core.scheduler.Scheduler;

/**
 * @author Konstantin Epifanov
 * @since 29.07.2019
 */
public class CustomExoPlayer extends SimpleExoPlayer implements Consumer<Point> {

  private Scheduler.Worker mWorker = null;
  private Disposable.Swap mPlaySwap = Disposables.swap();
  private Disposable.Swap mVideoListenerSwap = Disposables.swap();
  private Disposable.Swap mSetupTextureSwap = Disposables.swap();

  private TextureView mTextureView = null;
  private Point mResolution = null;

  private boolean isLyingLooper = false;


  public static CustomExoPlayer create (Context context) {
    CustomExoPlayer[] players = new CustomExoPlayer[1];
    return players[0] = new CustomExoPlayer(context, players);
  }

  private CustomExoPlayer(Context context, CustomExoPlayer[] players) {
    super(context,

      new DefaultRenderersFactory(context) {
        @Override
        protected void buildVideoRenderers(Context context,
                                           @Nullable DrmSessionManager<FrameworkMediaCrypto> drmSessionManager,
                                           long allowedVideoJoiningTimeMs,
                                           Handler eventHandler,
                                           VideoRendererEventListener eventListener,
                                           int extensionRendererMode,
                                           ArrayList<Renderer> out) {
          out.add(

            new CustomVideoRenderer(
              context,
              MediaCodecSelector.DEFAULT, // В случае еще более слабых девайсов включаем DEFAULT_WITH_FALLBACK
              allowedVideoJoiningTimeMs,
              drmSessionManager,
              /* playClearSamplesWithoutKeys= */ false,
              eventHandler,
              eventListener,
              MAX_DROPPED_VIDEO_FRAME_COUNT_TO_NOTIFY,
              () -> players[0]
            )

          );

          if (extensionRendererMode == EXTENSION_RENDERER_MODE_OFF) {
            return;
          }

          int extensionRendererIndex = out.size();
          if (extensionRendererMode == EXTENSION_RENDERER_MODE_PREFER) {
            extensionRendererIndex--;
          }

          try {
            // Full class names used for constructor args so the LINT rule triggers if any of them move.
            // LINT.IfChange
            Class<?> clazz = Class.forName("com.google.android.exoplayer2.ext.vp9.LibvpxVideoRenderer");
            Constructor<?> constructor =
              clazz.getConstructor(
                boolean.class,
                long.class,
                Handler.class,
                VideoRendererEventListener.class,
                int.class);
            // LINT.ThenChange(../../../../../../../proguard-rules.txt)

            Renderer renderer =
              (Renderer)
                constructor.newInstance(
                  true,
                  allowedVideoJoiningTimeMs,
                  eventHandler,
                  eventListener,
                  MAX_DROPPED_VIDEO_FRAME_COUNT_TO_NOTIFY);

            out.add(extensionRendererIndex++, renderer);

          } catch (ClassNotFoundException e) {
            // Expected if the app was built without the extension.
          } catch (Exception e) {
            // The extension is present, but instantiation failed.
            throw new RuntimeException("Error instantiating VP9 extension", e);
          }
        }
      },

      new DefaultTrackSelector(),
      new DefaultLoadControl() {
        @Override
        public void onPrepared() {
          super.onPrepared();
          System.out.println("ON PREPAREED" + " " + hashCode());
        }
      },
      null,
      new DefaultBandwidthMeter.Builder().build(),
      new AnalyticsCollector.Factory(),
      new CustomWorkLooperClock(),
      Util.getLooper());
  }



/*  @Override
  public void setVideoTextureView(TextureView textureView) {
    super.setVideoTextureView(textureView);
    Schedule.trowIfNotWorkerThread();
  }*/

/*  public void setPlayWhenReadyOnWork(boolean state) {
    //Schedule.trowIfNotMainThread();
    //mPlaySwap.update(mWorker.schedule(() -> setPlayWhenReady(state)));
    mWorker.schedule(() -> setPlayWhenReady(state));
  }

  private void addVideoListenerOnWork(com.google.android.exoplayer2.video.VideoListener listener) {
    System.out.println("ADDVIDEOLISTENERONWORK: " + listener + " " + hashCode());
    //Schedule.trowIfNotMainThread();
    //mVideoListenerSwap.update(mWorker.schedule(() -> addVideoListener(listener)));
    mWorker.schedule(() -> addVideoListener(listener));
  }

  private void setVideoTextureViewOnWork(TextureView texture) {
    //Schedule.trowIfNotMainThread();
    //mSetupTextureSwap.update(mWorker.schedule(() -> this.setVideoTextureView(texture)));
    mWorker.schedule(() -> this.setVideoTextureView(texture));
  }

  private void stopOnWork() {
    System.out.println("STOPONWORK");
    mWorker.schedule(this::stop);
  }

  private void releaseOnWork() {
    System.out.println("RELEASEONWORK");
    mWorker.schedule(this::release);
  }*/

  public Optional<MediaCodecVideoRenderer> findMediaCodecVideoRenderer() {
    for (Renderer renderer : renderers)
      if (renderer instanceof MediaCodecVideoRenderer)
        return Optional.of((MediaCodecVideoRenderer) renderer);

    return Optional.empty();
  }

  @Override
  public void setPlayWhenReady(boolean playWhenReady) {
    // Да мы специально врём. Для того что бы не срать в лог при playwhenready про поток
    // врём только тогда когда текущий лупер - Майн // todo
    //We prevents invalid thread warnings for more cleaner logs
    isLyingLooper = Looper.myLooper() == Looper.getMainLooper();
    super.setPlayWhenReady(playWhenReady);
    isLyingLooper = false;
  }

  @Override
  public Looper getApplicationLooper() {
    return isLyingLooper ? Looper.myLooper() : super.getApplicationLooper();
  }

  @Override
  public void setVideoTextureView(TextureView textureView) {
    if (mTextureView != textureView) {
      mTextureView = textureView;
      invalidateTextureResolution();
    }
    super.setVideoTextureView(textureView);
  }

  @Override
  public void accept(Point point) {
    System.out.println("RESOLUTION: " + point);
    if (!Objects.equals(mResolution, point)) {
      mResolution = point;
      invalidateTextureResolution();
    }
  }

  private void invalidateTextureResolution() {
    if (mTextureView != null && mTextureView instanceof PlayerTextureView && mResolution != null) {
      ((PlayerTextureView) mTextureView).initialize(mResolution.x, mResolution.y);
    }
  }

  @Override
  public void release() {
    mTextureView = null;
    super.release();
  }

}
