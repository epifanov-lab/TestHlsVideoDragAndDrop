package com.example.testvideodraganddrop.player;

import android.content.Context;
import android.net.Uri;

import com.example.testvideodraganddrop.view.player_texture_view.PlayerTextureView;
import com.example.testvideodraganddrop.utils.reactor.Schedule;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.hls.DefaultHlsExtractorFactory;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.RawResourceDataSource;
import com.google.android.exoplayer2.upstream.TransferListener;
import com.google.android.exoplayer2.util.Util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import reactor.core.publisher.Mono;

/**
 * @author Konstantin Epifanov
 * @since 26.07.2019
 */
public class HlsMediaController {

  private static ExecutorService executor = Executors.newSingleThreadExecutor();

  public static Mono<WebkaPlayer> createPlayerAsync(Context context, String url, PlayerTextureView texture) {
    System.out.println("CREATEPLAYERASYNC: " + url);
    return Mono
      .fromCallable(() -> WebkaPlayer.EXO(context, url, texture))
      .subscribeOn(Schedule.WORK_SCHEDULER)
      .cancelOn(Schedule.WORK_SCHEDULER)
      //.publishOn(Schedule.MAIN_SCHEDULER)
      ;
  }

  public static Mono<WebkaPlayer> createPlayerAsync(Context context, int resId, PlayerTextureView texture) {
    System.out.println("CREATEPLAYERASYNC: " + resId);
    return Mono
      .fromCallable(() -> WebkaPlayer.EXO(context, resId, texture))
      .subscribeOn(Schedule.WORK_SCHEDULER)
      .cancelOn(Schedule.WORK_SCHEDULER)
      //.publishOn(Schedule.MAIN_SCHEDULER)
      ;
  }

/*  public static Mono<WebkaPlayer> createPlayerAsync(Context context, String url, PlayerTextureView texture) {
    System.out.println("CREATEPLAYERASYNC: " + url);
    return Mono
      .fromCallable(() -> ExtendedExoPlayer.create(context, url, texture))
      .subscribeOn(Schedule.WORK_SCHEDULER)
      .publishOn(Schedule.MAIN_SCHEDULER)
      ;
  }*/

public static MediaSource buildResMediaSource(Context context, int resId) {
  TransferListener listener = new DefaultBandwidthMeter.Builder(context).build();

  DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(context, listener,
    new DefaultHttpDataSourceFactory(Util.getUserAgent(context, "Webka"), listener));

  MediaSource source = new ExtractorMediaSource.Factory(dataSourceFactory)
    .createMediaSource(RawResourceDataSource.buildRawResourceUri(resId));
  return source;
}

  public static MediaSource buildHlsMediaSource(Context context, String url) {
    System.out.println("BUILDMEDIASOURCE: " + url);

    return new HlsMediaSource
      .Factory(new DefaultHttpDataSourceFactory(Util.getUserAgent(context, "polygon")))
      .setExtractorFactory(new DefaultHlsExtractorFactory())
      .createMediaSource(Uri.parse(url));
  }
}
