package com.example.testvideodraganddrop.player;

import android.content.Context;

import com.example.testvideodraganddrop.view.player_texture_view.PlayerTextureView;

import reactor.core.Disposable;
import reactor.core.Disposables;

/**
 * @author Konstantin Epifanov
 * @since 07.08.2019
 */
public interface WebkaPlayer extends Disposable {

  WebkaPlayer EMPTY = new EmptyPlayer();

  int CREATED = 0,
     PREPARED = 1,
     ATTACHED = 2;

  static WebkaPlayer MOCK(Context context, String url, PlayerTextureView texture) {
    return WebkaPlayerMock.create(context, url, texture);
  }

  static WebkaPlayer EXO(Context context, String url, PlayerTextureView texture) {
    return WebkaPlayerExo.create(context, url, texture);
  }

  static WebkaPlayer EXO(Context context, int resId, PlayerTextureView texture) {
    return WebkaPlayerExo.create(context, resId, texture);
  }

  void setPlayWhenReady(boolean value);



  final class EmptyPlayer implements WebkaPlayer {

    private final Disposable mDisposable = Disposables.disposed();

    @Override
    public void setPlayWhenReady(boolean value) {
      throw new IllegalStateException();
    }

    @Override
    public void dispose() {
      mDisposable.dispose();
    }

    @Override
    public boolean isDisposed() {
      return mDisposable.isDisposed();
    }
  }
}
