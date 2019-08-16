package com.example.testvideodraganddrop.player;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.TextureView;

import androidx.core.util.Pools;


import com.example.testvideodraganddrop.view.player_texture_view.PlayerTextureView;
import com.example.testvideodraganddrop.utils.reactor.Schedule;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.video.MediaCodecVideoRenderer;

import java.util.Optional;

import static com.example.testvideodraganddrop.player.HlsMediaController.buildHlsMediaSource;
import static com.example.testvideodraganddrop.player.HlsMediaController.buildResMediaSource;
import static java.util.Optional.ofNullable;

/**
 * @author Konstantin Epifanov
 * @since 07.08.2019
 */

/*
* Оптимизация в случае проблем:
* Увеличить делэй в RadioRecyclerView
* Уменьшить спидлимит в RadioRecyclerView
* Запросить меньший резолюшн HLS ( 480 )
* Включение софтварных кодеков ( MediaCodecSelector.DEFAULT_WITH_FALLBACK )
* ---
* Если остануться проблемы с застравшим звуком - продублировать setPlayWhenReady перед стопом
* ---
* Если ничего не поможет: переходим на один плэер прогреваемый в момент чекеда.
* т.е. видимость текстуры смещается с активэйтеда в чекеды.
 * */
final class WebkaPlayerExo implements WebkaPlayer {

  private static final Pools.SynchronizedPool<CustomExoPlayer> EXO_POOL = new Pools.SynchronizedPool<>(10); // todo

  private final CustomExoPlayer mInternalPlayer;

  private final Handler mWorkHandler = new Handler(Schedule.WORK_LOOPER);

  WebkaPlayerExo(CustomExoPlayer player) {
    this.mInternalPlayer = player;
    mInternalPlayer.setVolume(1f);
  }

  @Override
  public void dispose() {
    if (isMainThread()) {
      //Schedule.WORK_EXECUTOR.execute(() -> dispose(ATTACHED));
      mWorkHandler.postAtFrontOfQueue(() -> dispose(ATTACHED));
    } else dispose(ATTACHED);
  }

  @Override
  public void setPlayWhenReady(boolean value) {
    //Schedule.trowIfNotMainThread();


    //mInternalPlayer.setPlayWhenReady(value);

    System.out.println("SET_PLAY_WHEN_READY: " + value);

    mWorkHandler.postAtFrontOfQueue(() -> mInternalPlayer.setPlayWhenReady(value));

    /*if (isMainThread()) Schedule.WORK_EXECUTOR.execute(() -> mInternalPlayer.setPlayWhenReady(value));
    else mInternalPlayer.setPlayWhenReady(value);*/
  }

  @Override
  public boolean isDisposed() {
    return isDisposed();
  }

  private void dispose(int i) {
    switch (i) {
      case ATTACHED:
        this.setVideoTextureView(null);

      case PREPARED:
        this.stop();

      case CREATED:
        this.release();
    }
  }

  static WebkaPlayerExo create(Context context, String url, PlayerTextureView texture) {
    return create(context, buildHlsMediaSource(context, url), texture);
  }

  static WebkaPlayerExo create(Context context, int resId, PlayerTextureView texture) {
    return create(context, buildResMediaSource(context, resId), texture);
  }

  private static WebkaPlayerExo create(Context context, MediaSource source, PlayerTextureView texture) {
    Schedule.trowIfNotWorkerThread();

    System.out.println("PLAYER - CREATE START");

    final CustomExoPlayer ext = ofNullable(EXO_POOL.acquire())
      .orElseGet(() -> CustomExoPlayer.create(context));

    WebkaPlayerExo player = new WebkaPlayerExo(ext);

    if (Thread.interrupted()) {
      player.dispose(CREATED);
      return null;
    }

    System.out.println("PLAYER - CREATE PREPARE" + " " + player.hashCode());

    player.prepare(source);
    if (Thread.interrupted()) {
      player.dispose(PREPARED);
      return null;
    }

    System.out.println("PLAYER - CREATE ADD TEXTURE" + " " + player.hashCode());

    setupVideoTexture(texture, player.mInternalPlayer);
    if (Thread.interrupted()) {
      player.dispose(ATTACHED);
      return null;
    }

    System.out.println("PLAYER - CREATE END" + " " + player.hashCode());

    return player;
  }

  private void prepare(MediaSource mediaSource) {
    System.out.println("PLAYER - PREPARE: " + mediaSource + " " + hashCode());
    Schedule.trowIfNotWorkerThread();

    /*try {
      Thread.sleep(1000);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }*/

    mInternalPlayer.setPlayWhenReady(false);
    mInternalPlayer.prepare(mediaSource);
  }

  private void setVideoTextureView(TextureView texture) {
    System.out.println("PLAYER - SET_VIDEO_TEXTURE: " + hashCode() + " " + texture);
    Schedule.trowIfNotWorkerThread();
    mInternalPlayer.setVideoTextureView(texture);
  }

  private void stop() {
    System.out.println("PLAYER - STOP" + " " + hashCode());
    Schedule.trowIfNotWorkerThread();
    mInternalPlayer.stop();
  }

  private void release() {
    System.out.println("PLAYER - RELEASE" + " " + hashCode());
    Schedule.trowIfNotWorkerThread();
    if (!EXO_POOL.release(mInternalPlayer))
      mInternalPlayer.release();
  }


  private static void setupVideoTexture(PlayerTextureView texture, CustomExoPlayer player) {

    final Optional<MediaCodecVideoRenderer> videoRenderer = player.findMediaCodecVideoRenderer();

    //videoRenderer.ifPresent(renderer -> player.setVideoTextureView(texture));

    try {
      player.setVideoTextureView(texture);
      System.out.println("WebkaPlayerExo.setupVideoTexture");
    } catch (NullPointerException e) {
      e.printStackTrace();
    }

    /*videoRenderer.map(renderer -> ((CustomVideoRenderer) renderer).mFormatProcessor.subscribe(new Consumer<Point>() {
      @Override
      public void accept(Point point) {
        player.addVideoListenerOnWork(new VideoListener() {
                                  @Override
                                  public void onRenderedFirstFrame() {
                                    texture.initialize(point.x, point.y);
                                    player.removeVideoListener(this);

                                      *//*
                                      Bitmap bitmap = texture.getBitmap();
                                      if (bitmap != null)
                                      ScreenshotsController.put(context(), url, bitmap);
                                      *//*
                                  }
                                }
        );
      }
    }))
      .orElseGet(Disposables::single);*/
  }

  private boolean isMainThread() {
    return Thread.currentThread() == Looper.getMainLooper().getThread();
  }
}
