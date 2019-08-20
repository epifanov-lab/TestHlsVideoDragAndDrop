package com.example.testvideodraganddrop.view.player_texture_view;

import com.example.testvideodraganddrop.player.HlsMediaController;
import com.example.testvideodraganddrop.player.WebkaPlayer;
import com.example.testvideodraganddrop.utils.reactor.Schedule;

import java.util.concurrent.atomic.AtomicLongFieldUpdater;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import reactor.core.Disposable;
import reactor.core.publisher.Mono;

import static java.util.concurrent.atomic.AtomicLongFieldUpdater.newUpdater;


/**
 * @author Konstantin Epifanov
 * @since 02.08.2019
 */
public class PlayerTexturePresenter implements Disposable {

  private final static long
    CHECKED = 1L,
    NON_CHECKED = 0L;

  private volatile long mChecked = NON_CHECKED;
  private final Disposable dChecked;

  private static final AtomicLongFieldUpdater<PlayerTexturePresenter>
    CHECKED_UPDATER = newUpdater(PlayerTexturePresenter.class, "mChecked");

  private PlayerTextureContract mContract;

  /** Socket updater. */
  private static final AtomicReferenceFieldUpdater<PlayerTexturePresenter, WebkaPlayer>
    PLAYER_UPDATER = AtomicReferenceFieldUpdater.newUpdater(PlayerTexturePresenter.class, WebkaPlayer.class, "mWebkaPlayer");

  private volatile WebkaPlayer mWebkaPlayer = WebkaPlayer.EMPTY;

  PlayerTexturePresenter(PlayerTextureContract contract) {
    mContract = contract;

    mContract.mFluxItems
      .doOnNext(s -> setPlayer(WebkaPlayer.EMPTY))
      .flatMap(Mono::justOrEmpty)
      .transform(Schedule::work_work)
      .switchMap(url -> HlsMediaController.createPlayerAsync(mContract.getContext(), url, mContract.getView()))
      .subscribe(this::setPlayer);

    dChecked = mContract.mFluxChecked
      .subscribe(this::setChecked);

    System.out.println("PRESENTER CONSTRUCTOR: " + mContract.getView());
  }

  private void setPlayer(WebkaPlayer player) {
    WebkaPlayer previous;
    do if ((previous = getPlayer()) == player) return;
    while (!PLAYER_UPDATER.compareAndSet(this, previous, player));
    previous.dispose();
    invalidate (player, getChecked());
  }

  private void setChecked(boolean value) {
    Schedule.trowIfNotMainThread();
    final long checked = value ? CHECKED : NON_CHECKED;
    long previous;
    do if ((previous = getChecked()) == checked) return;
    while (!CHECKED_UPDATER.compareAndSet(this, previous, checked));
    invalidate (getPlayer(), checked);
  }

  private WebkaPlayer getPlayer() {
    return PLAYER_UPDATER.get(this);
  }

  private long getChecked() {
    return CHECKED_UPDATER.get(this);
  }

  private void invalidate(WebkaPlayer player, long checked) {
    if (player != WebkaPlayer.EMPTY) player.setPlayWhenReady(checked == CHECKED);
  }

  private boolean isPlayerAvailable() {
    return getPlayer() != WebkaPlayer.EMPTY;
  }

  @Override
  public void dispose() {
    System.out.println("PRESENTER DESTRUCTOR");
    setPlayer(WebkaPlayer.EMPTY);
    dChecked.dispose();
  }

  @Override
  public boolean isDisposed() {
    return dChecked.isDisposed();
  }

}