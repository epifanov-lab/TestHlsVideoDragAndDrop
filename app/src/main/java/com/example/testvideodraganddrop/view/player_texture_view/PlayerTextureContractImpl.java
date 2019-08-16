package com.example.testvideodraganddrop.view.player_texture_view;

import android.content.Context;

/**
 * @author Konstantin Epifanov
 * @since 02.08.2019
 */
public class  PlayerTextureContractImpl extends PlayerTextureContract {

  private PlayerTextureView view;

  PlayerTextureContractImpl(PlayerTextureView view) {
      super(view.mProcessorItem, view.mProcessorChecked);
    this.view = view;
  }

  @Override
  PlayerTextureView getView() {
    return view;
  }

  @Override
  boolean isChecked() {
    return view.isChecked();
  }

  @Override
  Context getContext() {
    return view.getContext();
  }
}
