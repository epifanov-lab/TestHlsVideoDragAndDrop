package com.example.testvideodraganddrop.view.player_texture_view;

import android.content.Context;

import java.util.Optional;

import reactor.core.publisher.Flux;

/**
 * @author Konstantin Epifanov
 * @since 02.08.2019
 */
public abstract class PlayerTextureContract {

  final Flux<Optional<String>> mFluxItems;
  final Flux<Boolean> mFluxChecked;

  PlayerTextureContract(Flux<Optional<String>> fluxItems, Flux<Boolean> fluxChecked) {
    mFluxItems = fluxItems;
    mFluxChecked = fluxChecked;
  }

  abstract PlayerTextureView getView();

  abstract boolean isChecked();

  abstract Context getContext();
}