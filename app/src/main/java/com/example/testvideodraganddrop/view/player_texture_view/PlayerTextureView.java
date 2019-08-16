package com.example.testvideodraganddrop.view.player_texture_view;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.util.AttributeSet;
import android.view.TextureView;
import android.widget.Checkable;

import androidx.annotation.NonNull;

import com.example.testvideodraganddrop.utils.RatioKeeper;

import java.util.Optional;
import java.util.function.Consumer;

import reactor.core.publisher.ReplayProcessor;


public final class PlayerTextureView extends TextureView implements Checkable, Consumer<String> {

  ReplayProcessor<Optional<String>> mProcessorItem = ReplayProcessor.cacheLast();
  ReplayProcessor<Boolean> mProcessorChecked = ReplayProcessor.cacheLastOrDefault(isChecked());

  private PlayerTextureContract contract;
  private PlayerTexturePresenter mPresenter;
  private Runnable mDisposable = null;

  private boolean isChecked;

  /** Aspect Ratio Keeper. */
  private final RatioKeeper mRatioKeeper =
    new RatioKeeper(this::setTransform);

  public PlayerTextureView(Context context) {
    this(context, null);
  }

  public PlayerTextureView(Context context, AttributeSet attrs) {
    this(context, attrs, 0, 0);
  }

  public PlayerTextureView(Context context, AttributeSet attrs, int defStyleAttr) {
    this(context, attrs, defStyleAttr, 0);
  }

  public PlayerTextureView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);
    contract = new PlayerTextureContractImpl(this);

    accept("https://devstreaming-cdn.apple.com/videos/streaming/examples/bipbop_adv_example_hevc/v3/prog_index.m3u8");
    setOnClickListener(v -> setChecked(!isChecked()));

  }

  @Override
  public void accept(String item) {
    mProcessorItem.onNext(Optional.ofNullable(item));
  }

  @Override
  public void setChecked(boolean checked) {
    System.out.println("TEXTURE CHECKED: " + checked);
    if (isChecked == checked) return;
    isChecked = checked;
    mProcessorChecked.onNext(isChecked());
  }

  @Override
  public boolean isChecked() {
    return isChecked;
  }

  @Override
  public void toggle() {
    isChecked = !isChecked;
  }

  public synchronized void initialize(int width, int height) {
    mRatioKeeper.videoSize(width, height);
  }

  /** {@inheritDoc} */
  @Override
  protected final void onSizeChanged(int nw, int nh, int ow, int oh) {
    mRatioKeeper.viewPort(nw, nh);
    super.onSizeChanged(nw, nh, ow, oh);
  }

  /** {@inheritDoc} */
  @Override
  public final void setScaleX(float value) {
    mRatioKeeper.scaleX(value);
    super.setScaleX(value);
  }

  @Override
  public final void setScaleY(float value) {
    mRatioKeeper.scaleY(value);
    super.setScaleY(value);
  }

  @Override
  public final void onVisibilityAggregated(boolean value) {
    if (mDisposable != null) {
      mDisposable.run();
      mDisposable = null;
    }

    if (!value) detach();
    super.onVisibilityAggregated(value);
    if (value)
      if (isAvailable()) attach();
      else mDisposable = attachWhenReady(this, this::attach);
  }

  private void attach() {
    if (mPresenter == null && !this.isInEditMode()) {
      mPresenter = new PlayerTexturePresenter(contract);
    }
  }

  private void detach() {
    if (mPresenter != null) {
      mPresenter.dispose();
      if (mPresenter.isDisposed()) mPresenter = null;
    }
  }

  private static Runnable attachWhenReady(@NonNull TextureView view, @NonNull Runnable runnable) {
    final boolean[] disposed = {false};
    final TextureView.SurfaceTextureListener listener = view.getSurfaceTextureListener();
    view.setSurfaceTextureListener(
      onAvailable(
        () -> view.setSurfaceTextureListener(listener),
        () -> {
          if (!disposed[0]) runnable.run();
        }
      ));
    return () -> disposed[0] = true;
  }

  @Override
  protected void onDetachedFromWindow() {
    setSurfaceTextureListener(null); // exo вешает листенер на текстуру, и обращается к нему в главном после релиза

    if (mDisposable != null) {
      mDisposable.run();
      mDisposable = null;
    }

    detach(); // todo texture undo
    super.onDetachedFromWindow();
  }

  @NonNull
  private static TextureView.SurfaceTextureListener onAvailable(@NonNull Runnable... callbacks) {

    return new TextureView.SurfaceTextureListener() {

      private void throwAsNotSupported() {
        throw new IllegalStateException("Not supported method");
      }

      @Override
      public final void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        for (final Runnable task : callbacks) task.run();
      }

      @Override
      public final void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        throwAsNotSupported();
      }

      @Override
      public final boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        throwAsNotSupported();
        return true;
      }

      @Override
      public final void onSurfaceTextureUpdated(SurfaceTexture surface) {
        throwAsNotSupported();
      }
    };

  }

}
