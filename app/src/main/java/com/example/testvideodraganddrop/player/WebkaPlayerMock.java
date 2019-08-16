package com.example.testvideodraganddrop.player;

import android.content.Context;
import android.view.TextureView;


import com.example.testvideodraganddrop.view.player_texture_view.PlayerTextureView;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.video.VideoListener;

/**
 * @author Konstantin Epifanov
 * @since 07.08.2019
 */
final class WebkaPlayerMock implements WebkaPlayer {

  static WebkaPlayerMock create(Context context, String url, PlayerTextureView texture) {

    WebkaPlayerMock player = new WebkaPlayerMock();
    if (Thread.interrupted()) {
      player.dispose(CREATED);
      return null;
    }

    player.prepare(HlsMediaController.buildHlsMediaSource(context, url));
    if (Thread.interrupted()) {
      player.dispose(PREPARED);
      return null;
    }

    player.setVideoTexture(texture);
    if (Thread.interrupted()) {
      player.dispose(ATTACHED);
      return null;
    }

    return player;
  }

  WebkaPlayerMock() {
    System.out.println("MOCK PLAYER CONSTRUCTED: " + hashCode());
    delay();
  }

  @Override
  public void dispose() {
    dispose(ATTACHED);
  }

  @Override
  public boolean isDisposed() {
    return false;
  }

  private void dispose(int i) {
    System.out.println("DISPOSE " +  i + " " + hashCode());
    switch (i) {
      case ATTACHED:
        this.setVideoTexture(null);

      case PREPARED:
        this.stop();

      case CREATED:
        this.release();
    }
  }

  public void prepare(MediaSource source) {
    System.out.println("PREPARE " + hashCode());
    delay();
  }

  public void addVideoListener(VideoListener listener) {
    System.out.println("ADD VIDEO LISTENER " + hashCode());

  }

  public void removeVideoListener(VideoListener listener) {
    System.out.println("REMOVE VIDEO LISTENER " + hashCode());

  }

  public void setVideoTexture(TextureView texture) {
    System.out.println("SET VIDEO TEXTURE " + hashCode() + " " + texture);
    delay();
  }

  @Override
  public void setPlayWhenReady(boolean value) {
    System.out.println("SET PLAY WHEN READY " + hashCode());
  }

  public void stop() {
    System.out.println("STOP  " + hashCode());
    delay();
  }

  public void release() {
    System.out.println("RELEASE  " + hashCode());
    delay();
  }

  public static void delay() {
    try {
      Thread.sleep(400);
    } catch (InterruptedException ignored) {
      Thread.currentThread().interrupt();
      //System.out.println("IS_INTERRUPTED: " + Thread.currentThread().isInterrupted());
      //ignored.printStackTrace();
    }

  }
}
