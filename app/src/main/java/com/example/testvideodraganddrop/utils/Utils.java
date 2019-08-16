package com.example.testvideodraganddrop.utils;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Point;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.AsyncDifferConfig;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

/**
 * @author Konstantin Epifanov
 * @since 24.06.2019
 */
public class Utils {

  /** Returns random dummy image URL */
  public static String getRandomImageUrl(int pagingOffset) {
    pagingOffset += 10;
    return "https://picsum.photos/id/" + pagingOffset + "/600/800";
  }

  public static Flux<View> toFlux(View view) {
    return Flux.create(sink -> {
      view.setOnClickListener(sink::next);

      sink.onDispose(new Disposable() {
        @Override
        public void dispose() {
          view.setOnClickListener(null);
        }

        @Override
        public boolean isDisposed() {
          return !view.hasOnClickListeners();
        }
      });
    });
  }

  public static Flux<Point> scrollEvents(RecyclerView recycler) {
    return Flux.create(sink ->
      sink.onDispose(scrollToDisposable(recycler, new RecyclerView.OnScrollListener() {
        {
          recycler.addOnScrollListener(this);
        }

        @Override
        public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
          sink.next(new Point(dx, dy));
        }

      })), FluxSink.OverflowStrategy.IGNORE);
  }

  public static Disposable scrollToDisposable(RecyclerView recycler, RecyclerView.OnScrollListener listener) {
    return Disposables.composite(() -> recycler.removeOnScrollListener(listener));
  }

  public static int getRandomColor() {
    return Color.rgb(
      (int) (Math.random() * 255),
      (int) (Math.random() * 255),
      (int) (Math.random() * 255)
    );
  }

  public static int getScreenWidthInDp(Context context) {
    Configuration config = context.getResources().getConfiguration();
    return config.screenWidthDp;
  }

  public static int getScreenHeightInDp(Context context) {
    Configuration config = context.getResources().getConfiguration();
    return config.screenHeightDp;
  }

  public static float getScreenWidthInPx(Context context) {
    Configuration config = context.getResources().getConfiguration();
    return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, config.screenWidthDp, context.getResources().getDisplayMetrics());
  }

  public static float getScreenHeightInPx(Context context) {
    Configuration config = context.getResources().getConfiguration();
    return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, config.screenHeightDp, context.getResources().getDisplayMetrics());
  }

}
