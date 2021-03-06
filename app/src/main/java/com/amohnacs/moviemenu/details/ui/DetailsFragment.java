/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.amohnacs.moviemenu.details.ui;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v17.leanback.app.DetailsFragmentBackgroundController;
import android.support.v17.leanback.widget.Action;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.ClassPresenterSelector;
import android.support.v17.leanback.widget.DetailsOverviewRow;
import android.support.v17.leanback.widget.FullWidthDetailsOverviewRowPresenter;
import android.support.v17.leanback.widget.FullWidthDetailsOverviewSharedElementHelper;
import android.support.v17.leanback.widget.ImageCardView;
import android.support.v17.leanback.widget.OnActionClickedListener;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import com.amohnacs.moviemenu.R;
import com.amohnacs.moviemenu.details.DetailsPresenter;
import com.amohnacs.moviemenu.main.ui.MainActivity;
import com.amohnacs.moviemenu.model.Movie;
import com.amohnacs.moviemenu.utils.ViewUtils;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;

import static com.amohnacs.moviemenu.main.ItemMovieViewModel.GLIDE_IMAGE_ROOT;

/**
 * LeanbackDetailsFragment extends DetailsFragment, a Wrapper fragment for leanback details screens.
 * It shows a detailed view of video and its meta plus related videos.
 */
public class DetailsFragment extends android.support.v17.leanback.app.DetailsFragment {
    private static final String TAG = DetailsFragment.class.getSimpleName();

    private static final int ACTION_WATCH_TRAILER = 1;
    private static final int ACTION_RENT = 2;
    private static final int ACTION_BUY = 3;

    private static final int DETAIL_THUMB_WIDTH = 274;
    private static final int DETAIL_THUMB_HEIGHT = 274;

    //for use with related movies
    private static final int NUM_COLS = 10;

    private Movie mSelectedMovie;

    private ArrayObjectAdapter mAdapter;
    private ClassPresenterSelector mPresenterSelector;

    private DetailsFragmentBackgroundController mDetailsBackground;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate DetailsFragment");
        super.onCreate(savedInstanceState);

        mDetailsBackground = new DetailsFragmentBackgroundController(this);

        mSelectedMovie =
                (Movie) getActivity().getIntent().getSerializableExtra(DetailsActivity.MOVIE);

        if (mSelectedMovie != null) {

            mPresenterSelector = new ClassPresenterSelector();
            mAdapter = new ArrayObjectAdapter(mPresenterSelector);

            setupDetailsOverviewRow();
            setupDetailsOverviewRowPresenter();
            //setupRelatedMovieListRow();
            setAdapter(mAdapter);
            initializeBackground(mSelectedMovie);

            setOnItemViewClickedListener(new ItemViewClickedListener());

        } else {
            Intent intent = new Intent(getActivity(), MainActivity.class);
            startActivity(intent);
        }
    }

    private void initializeBackground(Movie data) {
        mDetailsBackground.enableParallax();

        Glide.with(getActivity())
                .load(GLIDE_IMAGE_ROOT + data.getBackdropPath())
                .asBitmap()
                .centerCrop()
                .error(R.drawable.default_background)
                .into(new SimpleTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(Bitmap bitmap,
                                                GlideAnimation<? super Bitmap> glideAnimation) {
                        mDetailsBackground.setCoverBitmap(bitmap);
                        mAdapter.notifyArrayItemRangeChanged(0, mAdapter.size());
                    }
                });
    }

    /**
     * Programmatically set up our view's data
     */
    private void setupDetailsOverviewRow() {
        Log.d(TAG, "doInBackground: " + mSelectedMovie.toString());

        final DetailsOverviewRow row = new DetailsOverviewRow(mSelectedMovie);
        int width = ViewUtils.convertDpToPixel(getActivity().getApplicationContext(), DETAIL_THUMB_WIDTH);
        int height = ViewUtils.convertDpToPixel(getActivity().getApplicationContext(), DETAIL_THUMB_HEIGHT);

        ArrayObjectAdapter actionAdapter = new ArrayObjectAdapter();

        row.setImageDrawable(
                ContextCompat.getDrawable(getContext(), R.drawable.default_background));

        Glide.with(getActivity())
                .load(GLIDE_IMAGE_ROOT + mSelectedMovie.getPosterPath())
                .centerCrop()
                .error(R.drawable.default_background)
                .into(new SimpleTarget<GlideDrawable>(width, height) {
                    @Override
                    public void onResourceReady(GlideDrawable resource,
                                                GlideAnimation<? super GlideDrawable>
                                                        glideAnimation) {
                        Log.d(TAG, "details overview card image url ready: " + resource);
                        row.setImageDrawable(resource);
                        mAdapter.notifyArrayItemRangeChanged(0, mAdapter.size());
                    }
                });

        actionAdapter.add(
                new Action(
                        ACTION_WATCH_TRAILER,
                        getResources().getString(R.string.watch_trailer_1),
                        getResources().getString(R.string.watch_trailer_2)));
        actionAdapter.add(
                new Action(
                        ACTION_RENT,
                        getResources().getString(R.string.rent_1),
                        getResources().getString(R.string.rent_2)));
        actionAdapter.add(
                new Action(
                        ACTION_BUY,
                        getResources().getString(R.string.buy_1),
                        getResources().getString(R.string.buy_2)));
        row.setActionsAdapter(actionAdapter);

        mAdapter.add(row);
    }

    /**
     * Binding our overview views to our business logic.
     * <p></p>
     * First, we are setting up our {@link FullWidthDetailsOverviewRowPresenter} to be aligned beneath
     * our OverView Adapter. Second, we set the hero transition from our focused and selected movie item
     *
     */
    private void setupDetailsOverviewRowPresenter() {
        FullWidthDetailsOverviewRowPresenter detailsPresenter =
                new FullWidthDetailsOverviewRowPresenter(new DetailsPresenter());
        FullWidthDetailsOverviewSharedElementHelper sharedElementHelper =
                new FullWidthDetailsOverviewSharedElementHelper();

        // Set detail background.
        detailsPresenter.setBackgroundColor(
                ContextCompat.getColor(getContext(), R.color.selected_background));
        // Hook up transition element.
        sharedElementHelper.setSharedElementEnterTransition(
                getActivity(), DetailsActivity.SHARED_ELEMENT_NAME);
        detailsPresenter.setListener(sharedElementHelper);
        detailsPresenter.setParticipatingEntranceTransition(true);

        detailsPresenter.setOnActionClickedListener(new OnActionClickedListener() {
            @Override
            public void onActionClicked(Action action) {
                if (action.getId() == ACTION_WATCH_TRAILER) {
                    /*
                    Intent intent = new Intent(getActivity(), PlaybackActivity.class);
                    intent.putExtra(DetailsActivity.MOVIE, mSelectedMovie);
                    startActivity(intent);
                    */
                    Toast.makeText(getActivity(), "Playback Coming Soon!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getActivity(), action.toString(), Toast.LENGTH_SHORT).show();
                }
            }
        });
        //we are not interacting with our movies anymore but with overview actions items
        mPresenterSelector.addClassPresenter(DetailsOverviewRow.class, detailsPresenter);
    }

    private final class ItemViewClickedListener implements OnItemViewClickedListener {
        @Override
        public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                                  RowPresenter.ViewHolder rowViewHolder, Row row) {

            if (item instanceof Movie) {

                Log.d(TAG, "Item: " + item.toString());
                Intent intent = new Intent(getActivity(), DetailsActivity.class);
                intent.putExtra(getResources().getString(R.string.movie), mSelectedMovie);

                Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(
                        getActivity(),
                        ((ImageCardView) itemViewHolder.view).getMainImageView(),
                        DetailsActivity.SHARED_ELEMENT_NAME).toBundle();

                getActivity().startActivity(intent, bundle);
            }
        }
    }
}
