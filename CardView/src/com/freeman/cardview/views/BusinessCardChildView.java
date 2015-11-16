package com.freeman.cardview.views;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.freeman.cardview.R;
import com.freeman.cardview.helpers.BusinessCardChildViewTransform;
import com.freeman.cardview.helpers.BusinessCardViewConfig;
import com.freeman.cardview.helpers.FakeShadowDrawable;
import com.freeman.cardview.helpers.PathInterpolatorDonut;
import com.freeman.cardview.helpers.ViewHelper;
import com.freeman.cardview.utilities.DVUtils;

public class BusinessCardChildView<T> extends FrameLayout implements
		View.OnClickListener, View.OnLongClickListener {

	/**
	 * The TaskView callbacks
	 */
	interface DeckChildViewCallbacks<T> {
		public void onDeckChildViewAppIconClicked(BusinessCardChildView<T> dcv);

		public void onDeckChildViewAppInfoClicked(BusinessCardChildView<T> dcv);

		public void onDeckChildViewClicked(BusinessCardChildView<T> dcv, T key);

		public void onDeckChildViewDismissed(BusinessCardChildView<T> dcv);

		public void onDeckChildViewClipStateChanged(BusinessCardChildView<T> dcv);

		public void onDeckChildViewFocusChanged(BusinessCardChildView<T> dcv,
				boolean focused);
	}

	BusinessCardViewConfig mConfig;

	float mTaskProgress;
	ObjectAnimator mTaskProgressAnimator;
	float mMaxDimScale;
	int mDimAlpha;
	AccelerateInterpolator mDimInterpolator = new AccelerateInterpolator(1f);
	PorterDuffColorFilter mDimColorFilter = new PorterDuffColorFilter(0,
			PorterDuff.Mode.SRC_ATOP);
	Paint mDimLayerPaint = new Paint();

	T mKey;
	boolean mTaskDataLoaded;
	boolean mIsFocused;
	boolean mFocusAnimationsEnabled;
	boolean mClipViewInStack;
	AnimateableDeckChildViewBounds mViewBounds;

	View mContent;
	ImageView mThumbnailView;
	DeckChildViewCallbacks<T> mCb;

	public static final Interpolator ALPHA_IN = new PathInterpolatorDonut(0.4f,
			0f, 1f, 1f);

	public BusinessCardChildView(Context context) {
		this(context, null);
	}

	public BusinessCardChildView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public BusinessCardChildView(Context context, AttributeSet attrs,
			int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		mConfig = BusinessCardViewConfig.getInstance();
		mMaxDimScale = mConfig.taskStackMaxDim / 255f;
		mClipViewInStack = true;
		setTaskProgress(getTaskProgress());
		setDim(getDim());
		if (mConfig.fakeShadows) {
			if (DVUtils.isAboveSDKVersion(16)) {
				setBackground(new FakeShadowDrawable(context.getResources(),
						mConfig));
			} else {
				setBackgroundDrawable(new FakeShadowDrawable(
						context.getResources(), mConfig));
			}
		}
		if (DVUtils.isAboveLollipop()) {
			mViewBounds = new AnimateableDeckChildViewBounds(this,
					mConfig.taskViewRoundedCornerRadiusPx);
			setOutlineProvider(mViewBounds);
		}
	}

	/**
	 * Set callback
	 */
	void setCallbacks(DeckChildViewCallbacks<T> cb) {
		mCb = cb;
	}

	/**
	 * Resets this TaskView for reuse.
	 */
	void reset() {
		resetViewProperties();
		resetNoUserInteractionState();
		setClipViewInStack(false);
		setCallbacks(null);
	}

	/**
	 * Gets the task
	 */
	T getAttachedKey() {
		return mKey;
	}

	/**
	 * Returns the view bounds.
	 */
	AnimateableDeckChildViewBounds getViewBounds() {
		return mViewBounds;
	}

	@Override
	protected void onFinishInflate() {
		// Bind the views
		mContent = findViewById(R.id.task_view_content);
		mThumbnailView = (ImageView) findViewById(R.id.task_view_thumbnail);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int width = MeasureSpec.getSize(widthMeasureSpec);
		int height = MeasureSpec.getSize(heightMeasureSpec);

		int widthWithoutPadding = width - getPaddingLeft() - getPaddingRight();

		// Measure the content
		mContent.measure(MeasureSpec.makeMeasureSpec(widthWithoutPadding,
				MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(
				widthWithoutPadding, MeasureSpec.EXACTLY));

		// Measure the thumbnail to be square
		mThumbnailView.measure(MeasureSpec.makeMeasureSpec(widthWithoutPadding,
				MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(
				widthWithoutPadding, MeasureSpec.EXACTLY));
		setMeasuredDimension(width, height);

		if (DVUtils.isAboveLollipop()) {
			invalidateOutline();
		}
	}

	/**
	 * Synchronizes this view's properties with the task's transform
	 */
	void updateViewPropertiesToTaskTransform(
			BusinessCardChildViewTransform toTransform, int duration) {
		updateViewPropertiesToTaskTransform(toTransform, duration, null);
	}

	void updateViewPropertiesToTaskTransform(
			BusinessCardChildViewTransform toTransform, int duration,
			ValueAnimator.AnimatorUpdateListener updateCallback) {
		// Apply the transform
		toTransform.applyToTaskView(this, duration,
				mConfig.fastOutSlowInInterpolator, false, !mConfig.fakeShadows,
				updateCallback);

		// Update the task progress
		DVUtils.cancelAnimationWithoutCallbacks(mTaskProgressAnimator);
		if (duration <= 0) {
			setTaskProgress(toTransform.p);
		} else {
			if (DVUtils.isAboveSDKVersion(11)) {
				// Optimizations
				ValueAnimator.AnimatorUpdateListener mUpdateDimListener = new ValueAnimator.AnimatorUpdateListener() {
					@Override
					public void onAnimationUpdate(ValueAnimator animation) {
						setTaskProgress((Float) animation.getAnimatedValue());
					}
				};
				mTaskProgressAnimator = ObjectAnimator.ofFloat(this,
						"taskProgress", toTransform.p);
				mTaskProgressAnimator.setDuration(duration);
				mTaskProgressAnimator.addUpdateListener(mUpdateDimListener);
				mTaskProgressAnimator.start();
			}
		}
	}

	/**
	 * Resets this view's properties
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	void resetViewProperties() {
		setDim(0);
		if(DVUtils.isAboveSDKVersion(11)){
			setLayerType(View.LAYER_TYPE_NONE, null);
		}
		BusinessCardChildViewTransform.reset(this);
	}

	/**
	 * When we are un/filtering, this method will set up the transform that we
	 * are animating to, in order to hide the task.
	 */
	void prepareTaskTransformForFilterTaskHidden(
			BusinessCardChildViewTransform toTransform) {
		// Fade the view out and slide it away
		toTransform.alpha = 0f;
		toTransform.translationY += 200;
		toTransform.translationZ = 0;
	}

	/**
	 * When we are un/filtering, this method will setup the transform that we
	 * are animating from, in order to show the task.
	 */
	void prepareTaskTransformForFilterTaskVisible(
			BusinessCardChildViewTransform fromTransform) {
		// Fade the view in
		fromTransform.alpha = 0f;
	}

	/**
	 * Prepares this task view for the enter-recents animations. This is called
	 * earlier in the first layout because the actual animation into recents may
	 * take a long time.
	 */
	void prepareEnterRecentsAnimation(boolean isTaskViewLaunchTargetTask,
			boolean occludesLaunchTarget, int offscreenY) {
		if (DVUtils.isAboveSDKVersion(11)) {
			int initialDim = getDim();
			if (mConfig.launchedHasConfigurationChanged) {
				// Just load the views as-is
			} else if (mConfig.launchedFromAppWithThumbnail) {
				if (isTaskViewLaunchTargetTask) {
					// Set the dim to 0 so we can animate it in
					initialDim = 0;
				} else if (occludesLaunchTarget) {
					// Move the task view off screen (below) so we can animate
					// it in
					setTranslationY(offscreenY);
				}

			} else if (mConfig.launchedFromHome) {
				// Move the task view off screen (below) so we can animate it in
				setTranslationY(offscreenY);
				if (DVUtils.isAboveLollipop()) {
					setTranslationZ(0);
				}
				setScaleX(1f);
				setScaleY(1f);
			}
			// Apply the current dim
			setDim(initialDim);
			// Prepare the thumbnail view alpha
			// TODO
			// mThumbnailView.prepareEnterRecentsAnimation(isTaskViewLaunchTargetTask);
		} else {
			prepareEnterRecentsAnimationCompat(isTaskViewLaunchTargetTask,
					occludesLaunchTarget, offscreenY);
		}
	}

	void prepareEnterRecentsAnimationCompat(boolean isTaskViewLaunchTargetTask,
			boolean occludesLaunchTarget, int offscreenY) {
		int initialDim = getDim();
		if (mConfig.launchedHasConfigurationChanged) {
			// Just load the views as-is
		} else if (mConfig.launchedFromAppWithThumbnail) {
			if (isTaskViewLaunchTargetTask) {
				initialDim = 0;
			} else if (occludesLaunchTarget) {
				ViewHelper.setTranslationY(this, offscreenY);
			}

		} else if (mConfig.launchedFromHome) {
			// Move the task view off screen (below) so we can animate it in
			ViewHelper.setTranslationY(this, offscreenY);
			ViewHelper.setScaleX(this, 1f);
			ViewHelper.setScaleY(this, 1f);
		}
		setDim(initialDim);
	}

	/**
	 * Animates this task view as it enters recents
	 */
	void startEnterRecentsAnimation(final ViewAnimation.TaskViewEnterContext ctx) {
		Log.i(getClass().getSimpleName(), "startEnterRecentsAnimation");
		final BusinessCardChildViewTransform transform = ctx.currentTaskTransform;
		int startDelay = 0;

		if (mConfig.launchedFromHome) {
			Log.i(getClass().getSimpleName(), "mConfig.launchedFromHome false");
			// Animate the tasks up
			int frontIndex = (ctx.currentStackViewCount
					- ctx.currentStackViewIndex - 1);
			int delay = mConfig.transitionEnterFromHomeDelay + frontIndex
					* mConfig.taskViewEnterFromHomeStaggerDelay;

			setScaleX(transform.scale);
			setScaleY(transform.scale);
			if (!mConfig.fakeShadows) {
				animate().translationZ(transform.translationZ);
			}
			animate()
					.translationY(transform.translationY)
					.setStartDelay(delay)
					.setUpdateListener(ctx.updateListener)
					.setInterpolator(mConfig.quintOutInterpolator)
					.setDuration(
							mConfig.taskViewEnterFromHomeDuration + frontIndex
									* mConfig.taskViewEnterFromHomeStaggerDelay)
					.withEndAction(new Runnable() {
						@Override
						public void run() {
							// Decrement the post animation trigger
							ctx.postAnimationTrigger.decrement();
						}
					}).start();
			ctx.postAnimationTrigger.increment();
			startDelay = delay;
		}

		// Enable the focus animations from this point onwards so that they
		// aren't affected by the
		// window transitions
		postDelayed(new Runnable() {
			@Override
			public void run() {
				enableFocusAnimations();
			}
		}, startDelay);
	}

	/**
	 * Animates this task view as it leaves recents by pressing home.
	 */
	void startExitToHomeAnimation(ViewAnimation.TaskViewExitContext ctx) {
		animate().translationY(ctx.offscreenTranslationY).setStartDelay(0)
				.setUpdateListener(null)
				.setInterpolator(mConfig.fastOutLinearInInterpolator)
				.setDuration(mConfig.taskViewExitToHomeDuration)
				.withEndAction(ctx.postAnimationTrigger.decrementAsRunnable())
				.start();
		ctx.postAnimationTrigger.increment();
	}

	/**
	 * Animates this task view as it exits recents
	 */
	void startLaunchTaskAnimation(final Runnable postAnimRunnable,
			boolean isLaunchingTask, boolean occludesLaunchTarget,
			boolean lockToTask) {
		if (isLaunchingTask) {
			// Animate the thumbnail alpha back into full opacity for the window
			// animation out
			// TODO mThumbnailView.startLaunchTaskAnimation(postAnimRunnable);

			// Animate the dim
			if (mDimAlpha > 0) {
				ObjectAnimator anim = ObjectAnimator.ofInt(this, "dim", 0);
				anim.setDuration(mConfig.taskViewExitToAppDuration);
				anim.setInterpolator(mConfig.fastOutLinearInInterpolator);
				anim.start();
			}
		} else {
			// Hide the dismiss button
			// TODO mHeaderView.startLaunchTaskDismissAnimation();
			// If this is another view in the task grouping and is in front of
			// the launch task,
			// animate it away first
			if (occludesLaunchTarget) {
				animate()
						.alpha(0f)
						.translationY(
								getTranslationY()
										+ mConfig.taskViewAffiliateGroupEnterOffsetPx)
						.setStartDelay(0).setUpdateListener(null)
						.setInterpolator(mConfig.fastOutLinearInInterpolator)
						.setDuration(mConfig.taskViewExitToAppDuration).start();
			}
		}
	}

	/**
	 * Animates the deletion of this task view
	 */
	void startDeleteTaskAnimation(final Runnable r) {
		// Disabling clipping with the stack while the view is animating away
		setClipViewInStack(false);

		animate().translationX(mConfig.taskViewRemoveAnimTranslationXPx)
				.alpha(0f).setStartDelay(0).setUpdateListener(null)
				.setInterpolator(mConfig.fastOutSlowInInterpolator)
				.setDuration(mConfig.taskViewRemoveAnimDuration)
				.withEndAction(new Runnable() {
					@Override
					public void run() {
						// We just throw this into a runnable because starting a
						// view property
						// animation using layers can cause inconsisten results
						// if we try and
						// update the layers while the animation is running. In
						// some cases,
						// the runnabled passed in may start an animation which
						// also uses layers
						// so we defer all this by posting this.
						r.run();

						// Re-enable clipping with the stack (we will reuse this
						// view)
						setClipViewInStack(true);
					}
				}).start();
	}

	/**
	 * Animates this task view if the user does not interact with the stack
	 * after a certain time.
	 */
	void startNoUserInteractionAnimation() {
		// mHeaderView.startNoUserInteractionAnimation();
	}

	/**
	 * Mark this task view that the user does has not interacted with the stack
	 * after a certain time.
	 */
	void setNoUserInteractionState() {
		// mHeaderView.setNoUserInteractionState();
	}

	/**
	 * Resets the state tracking that the user has not interacted with the stack
	 * after a certain time.
	 */
	void resetNoUserInteractionState() {
		// mHeaderView.resetNoUserInteractionState();
	}

	/**
	 * Dismisses this task.
	 */
	void dismissTask() {
		// Animate out the view and call the callback
		final BusinessCardChildView<T> tv = this;
		startDeleteTaskAnimation(new Runnable() {
			@Override
			public void run() {
				if (mCb != null) {
					mCb.onDeckChildViewDismissed(tv);
				}
			}
		});
	}

	/**
	 * Returns whether this view should be clipped, or any views below should
	 * clip against this view.
	 */
	boolean shouldClipViewInStack() {
		return mClipViewInStack && (getVisibility() == View.VISIBLE);
	}

	/**
	 * Sets whether this view should be clipped, or clipped against.
	 */
	public void setClipViewInStack(boolean clip) {
		if (clip != mClipViewInStack) {
			mClipViewInStack = clip;
			if (mCb != null) {
				mCb.onDeckChildViewClipStateChanged(this);
			}
		}
	}

	/**
	 * Sets the current task progress.
	 */
	public void setTaskProgress(float p) {
		mTaskProgress = p;
		updateDimFromTaskProgress();

		if (DVUtils.isAboveLollipop()) {
			mViewBounds.setAlpha(p);
		}
	}

	/**
	 * Returns the current task progress.
	 */
	public float getTaskProgress() {
		return mTaskProgress;
	}

	/**
	 * Returns the current dim.
	 */
	public void setDim(int dim) {
		mDimAlpha = dim;
		if (mConfig.useHardwareLayers) {
			// Defer setting hardware layers if we have not yet measured, or
			// there is no dim to draw
			if (getMeasuredWidth() > 0 && getMeasuredHeight() > 0) {
				mDimColorFilter = new PorterDuffColorFilter(Color.argb(
						mDimAlpha, 0, 0, 0), PorterDuff.Mode.SRC_ATOP);
				mDimLayerPaint.setColorFilter(mDimColorFilter);
				if (DVUtils.isAboveSDKVersion(11)) {
					mContent.setLayerType(LAYER_TYPE_HARDWARE, mDimLayerPaint);
				}
			}
		} else {
			// float dimAlpha = mDimAlpha / 255.0f;
			if (mThumbnailView != null) {
				// TODO mThumbnailView.setDimAlpha(dimAlpha);
			}
		}
	}

	/**
	 * Returns the current dim.
	 */
	public int getDim() {
		return mDimAlpha;
	}

	/**
	 * Animates the dim to the task progress.
	 */
	void animateDimToProgress(int delay, int duration,
			Animator.AnimatorListener postAnimRunnable) {
		// Animate the dim into view as well
		int toDim = getDimFromTaskProgress();
		if (toDim != getDim()) {
			ObjectAnimator anim = ObjectAnimator.ofInt(
					BusinessCardChildView.this, "dim", toDim);
			anim.setStartDelay(delay);
			anim.setDuration(duration);
			if (postAnimRunnable != null) {
				anim.addListener(postAnimRunnable);
			}
			anim.start();
		}
	}

	/**
	 * Compute the dim as a function of the scale of this view.
	 */
	int getDimFromTaskProgress() {
		float dim = mMaxDimScale
				* mDimInterpolator.getInterpolation(1f - mTaskProgress);
		return (int) (dim * 255);
	}

	/**
	 * Update the dim as a function of the scale of this view.
	 */
	void updateDimFromTaskProgress() {
		setDim(getDimFromTaskProgress());
	}

	/**** View focus state ****/

	/**
	 * Sets the focused task explicitly. We need a separate flag because
	 * requestFocus() won't happen if the view is not currently visible, or we
	 * are in touch state (where we still want to keep track of focus).
	 */
	public void setFocusedTask(boolean animateFocusedState) {
		mIsFocused = true;
		if (mFocusAnimationsEnabled) {
			// Focus the header bar
			// mHeaderView.onTaskViewFocusChanged(true, animateFocusedState);
		}
		// Update the thumbnail alpha with the focus
		// TODO mThumbnailView.onFocusChanged(true);
		// Call the callback
		if (mCb != null) {
			mCb.onDeckChildViewFocusChanged(this, true);
		}
		// Workaround, we don't always want it focusable in touch mode, but we
		// want the first task
		// to be focused after the enter-recents animation, which can be
		// triggered from either touch
		// or keyboard
		setFocusableInTouchMode(true);
		requestFocus();
		setFocusableInTouchMode(false);
		invalidate();
	}

	/**
	 * Unsets the focused task explicitly.
	 */
	void unsetFocusedTask() {
		mIsFocused = false;
		if (mFocusAnimationsEnabled) {
			// Un-focus the header bar
			// mHeaderView.onTaskViewFocusChanged(false, true);
		}

		// Update the thumbnail alpha with the focus
		// TODO mThumbnailView.onFocusChanged(false);
		// Call the callback
		if (mCb != null) {
			mCb.onDeckChildViewFocusChanged(this, false);
		}
		invalidate();
	}

	/**
	 * Updates the explicitly focused state when the view focus changes.
	 */
	@Override
	protected void onFocusChanged(boolean gainFocus, int direction,
			Rect previouslyFocusedRect) {
		super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
		if (!gainFocus) {
			unsetFocusedTask();
		}
	}

	/**
	 * Returns whether we have explicitly been focused.
	 */
	public boolean isFocusedTask() {
		return mIsFocused || isFocused();
	}

	/**
	 * Enables all focus animations.
	 */
	void enableFocusAnimations() {
		boolean wasFocusAnimationsEnabled = mFocusAnimationsEnabled;
		mFocusAnimationsEnabled = true;
		if (mIsFocused && !wasFocusAnimationsEnabled) {
			// Re-notify the header if we were focused and animations were not
			// previously enabled
			// mHeaderView.onTaskViewFocusChanged(true, true);
		}
	}

	/**** TaskCallbacks Implementation ****/

	/**
	 * Binds this task view to the task
	 */
	public void onTaskBound(T key) {
		mKey = key;
	}

	private boolean isBound() {
		return mKey != null;
	}

	/**
	 * Binds this task view to the task
	 */
	public void onTaskUnbound() {
		mKey = null;
	}

	public Bitmap getThumbnail() {
		if (mThumbnailView != null) {
			// return mThumbnailView.getThumbnail();
			return mThumbnailView.getDrawingCache();
		}

		return null;
	}

	public void onDataLoaded(T key, Bitmap thumbnail, Drawable headerIcon,
			String headerTitle, int headerBgColor) {
		if (!isBound() || !mKey.equals(key))
			return;

		if (mThumbnailView != null) {
			// Bind each of the views to the new task data
			mThumbnailView.setImageBitmap(thumbnail);
			// Rebind any listeners
			// mHeaderView.mApplicationIcon.setOnClickListener(this);
			// mHeaderView.mDismissButton.setOnClickListener(this);
		}
		mTaskDataLoaded = true;
	}

	public void onDataUnloaded() {
		if (mThumbnailView != null) {
			// Unbind each of the views from the task data and remove the task
			// callback
			mThumbnailView.setImageBitmap(null);
			// mHeaderView.unbindFromTask();
			// Unbind any listeners
			// mHeaderView.mApplicationIcon.setOnClickListener(null);
			// mHeaderView.mDismissButton.setOnClickListener(null);
		}
		mTaskDataLoaded = false;
	}

	/**
	 * Enables/disables handling touch on this task view.
	 */
	public void setTouchEnabled(boolean enabled) {
		setOnClickListener(enabled ? this : null);
	}

	/**
	 * * View.OnClickListener Implementation ***
	 */

	@Override
	public void onClick(final View v) {
		final BusinessCardChildView<T> tv = this;
		final boolean delayViewClick = (v != this);
		if (delayViewClick) {
			// We purposely post the handler delayed to allow for the touch
			// feedback to draw
			postDelayed(new Runnable() {
				@Override
				public void run() {
					/*
					 * if (DVConstants.DebugFlags.App.EnableTaskFiltering && v
					 * == mHeaderView.mApplicationIcon) { if (mCb != null) {
					 * mCb.onDeckChildViewAppIconClicked(tv); } } else if (v ==
					 * mHeaderView.mDismissButton) { dismissTask(); }
					 */
				}
			}, 125);
		} else {
			if (mCb != null) {
				mCb.onDeckChildViewClicked(tv, tv.getAttachedKey());
			}
		}
	}

	/**
	 * * View.OnLongClickListener Implementation ***
	 */

	@Override
	public boolean onLongClick(View v) {
		/*
		 * if (v == mHeaderView.mApplicationIcon) { if (mCb != null) {
		 * mCb.onDeckChildViewAppInfoClicked(this); return true; } }
		 */
		return false;
	}
}
