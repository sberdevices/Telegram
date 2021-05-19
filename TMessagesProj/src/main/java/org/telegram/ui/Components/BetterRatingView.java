package org.telegram.ui.Components;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;

/**
 * Created by grishka on 10.02.17.
 */

public class BetterRatingView extends LinearLayout {
	private final int numStars = 5;
	private int selectedRating = 0;
	private OnRatingChangeListener listener;

	private final int starSize = AndroidUtilities.dp(91);
	private final int starMargin = AndroidUtilities.dp(12);

	private final View[] stars = new View[numStars];

	public BetterRatingView(Context context) {
		super(context);
		addStars();
	}

	public BetterRatingView(Context context, @Nullable AttributeSet attrs) {
		super(context, attrs);
		addStars();
	}

	private void addStars() {
		for (int i = 0; i < stars.length; i++) {
			stars[i] = createStarView(i);
			addView(stars[i]);
		}
		updateStars();
		stars[0].requestFocus();
	}

	private View createStarView(int position) {
		LayoutParams layoutParams = new LayoutParams(starSize, starSize);
		layoutParams.setMarginStart(starMargin / 2);
		layoutParams.setMarginEnd(starMargin / 2);

		View star = new View(getContext());
		star.setFocusable(true);
		star.setForeground(ResourcesCompat.getDrawable(getResources(), R.drawable.sbdv_selector_focusable_rating_star, null));
		star.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.sbdv_feedback_rating_star, null));
		star.setLayoutParams(layoutParams);
		star.setOnClickListener(v -> {
			if (selectedRating != position + 1) {
				updateRating(position + 1);
			}
		});
		return star;
	}

	private void updateRating(int rating) {
		selectedRating = rating;
		if (listener != null) {
			listener.onRatingChanged(selectedRating);
		}
		updateStars();
	}

	private void updateStars() {
		for (int i = 0; i < stars.length; i++) {
			boolean isSelected = i + 1 <= selectedRating;
			stars[i].setSelected(isSelected);
		}
	}

	@Override
	public boolean dispatchTouchEvent(MotionEvent event) {
		float offset = AndroidUtilities.dp(-8);
		for (int i = 0; i < numStars; i++) {
			if (event.getX() > offset && event.getX() < offset + starSize + starMargin) {
				if (selectedRating != i + 1) {
					updateRating(i + 1);
					break;
				}
			}
			offset += starSize + starMargin;
		}
		return super.dispatchTouchEvent(event);
	}

	public int getRating() {
		return selectedRating;
	}

	public void setOnRatingChangeListener(OnRatingChangeListener l) {
		listener = l;
	}

	public interface OnRatingChangeListener {
		void onRatingChanged(int newRating);
	}
}
