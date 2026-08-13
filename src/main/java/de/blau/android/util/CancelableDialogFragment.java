package de.blau.android.util;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import org.jspecify.annotations.NonNull;

import android.animation.ValueAnimator;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.core.view.ViewGroupCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import de.blau.android.Main;

/**
 * Only removes some potential code duplication
 */
public abstract class CancelableDialogFragment extends DialogFragment {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, CancelableDialogFragment.class.getSimpleName().length());
    private static final String DEBUG_TAG = CancelableDialogFragment.class.getSimpleName().substring(0, TAG_LEN);

    private boolean            restartAutolock = false;
    private WindowInsetsCompat lastInsets;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setCancelable(true);
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog == null) {
            Log.e(DEBUG_TAG, "No dialog");
            return;
        }
        Window dialogWindow = getDialog().getWindow();
        if (dialogWindow == null) {
            Log.e(DEBUG_TAG, "No window for this dialog");
            return;
        }
        View view = dialogWindow.getDecorView().findViewById(android.R.id.content);
        ViewGroupCompat.installCompatInsetsDispatch(view);

        // this is a hack to avoid layout looping when we set the margins
        ViewCompat.setOnApplyWindowInsetsListener(view, (View v, @NonNull WindowInsetsCompat windowInsets) -> {
            if (windowInsets.equals(lastInsets)) {
                return WindowInsetsCompat.CONSUMED;
            }
            lastInsets = windowInsets;
            ViewGroup.LayoutParams lp = v.getLayoutParams();
            if (lp instanceof ViewGroup.MarginLayoutParams) {
                ValueAnimator animator = ValueAnimator.ofInt(((ViewGroup.MarginLayoutParams) lp).bottomMargin,
                        ConfigurationChangeAwareActivity.getInsets(windowInsets).bottom);
                animator.addUpdateListener(animation -> {
                    int animatedValue = (int) animation.getAnimatedValue();
                    ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
                    params.bottomMargin = animatedValue;
                    v.setLayoutParams(params);
                });
                animator.start();
            }
            return WindowInsetsCompat.CONSUMED;
        });
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        FragmentActivity activity = getActivity();
        if (activity instanceof Main && restartAutolock) {
            ((Main) activity).scheduleAutoLock();
        }
    }

    /**
     * Enable restarting auto-lock
     */
    protected void enableAutolockReschedule() {
        this.restartAutolock = true;
    }
}
