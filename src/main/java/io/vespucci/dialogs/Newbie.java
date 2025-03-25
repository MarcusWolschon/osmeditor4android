package io.vespucci.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.appcompat.app.AppCompatDialog;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import io.vespucci.R;
import io.vespucci.App;
import io.vespucci.Authorize;
import io.vespucci.HelpViewer;
import io.vespucci.Main;
import io.vespucci.layer.tiles.MapTilesLayer;
import io.vespucci.prefs.AdvancedPrefDatabase;
import io.vespucci.prefs.Preferences;
import io.vespucci.resources.TileLayerSource;
import io.vespucci.resources.TileLayerSource.Category;
import io.vespucci.util.ImmersiveDialogFragment;
import io.vespucci.util.OnPageSelectedListener;
import io.vespucci.util.ThemeUtils;
import io.vespucci.util.Util;
import io.vespucci.views.ExtendedViewPager;

/**
 * Display a dialog giving new users minimal instructions
 *
 */
public class Newbie extends ImmersiveDialogFragment {
    private static final String DEBUG_TAG = Newbie.class.getSimpleName().substring(0, Math.min(23, Newbie.class.getSimpleName().length()));

    private static final String TAG = "fragment_newbie";

    private static final String PAGER_POS_KEY     = "pagerPos";
    private static final String AUTHORIZE_KEY     = "authorize";
    private static final String PEN_SETUP_KEY     = "penSetup";
    private static final String AUTO_DOWNLOAD_KEY = "autoDownload";
    private static final String USE_IMAGERY_KEY   = "useImagery";

    private static final int SETTINGS_PAGE_INDEX = 1;
    private static final int WELCOME_PAGE_INDEX  = 0;

    /**
     * Display a dialog giving new users minimal instructions
     * 
     * @param activity the calling Activity
     */
    public static void showDialog(@NonNull FragmentActivity activity) {
        dismissDialog(activity);
        try {
            FragmentManager fm = activity.getSupportFragmentManager();
            Newbie newbieFragment = newInstance();
            newbieFragment.show(fm, TAG);
        } catch (IllegalStateException isex) {
            Log.e(DEBUG_TAG, "showDialog", isex);
        }
    }

    /**
     * Dismiss the dialog
     * 
     * @param activity the calling Activity
     */
    private static void dismissDialog(@NonNull FragmentActivity activity) {
        io.vespucci.dialogs.Util.dismissDialog(activity, TAG);
    }

    /**
     * Get a new instance of Newbie
     * 
     * @return a new Newbie instance
     */
    @NonNull
    private static Newbie newInstance() {
        Newbie f = new Newbie();
        f.setShowsDialog(true);
        return f;
    }

    @Override
    public void onAttach(Context context) {
        Log.d(DEBUG_TAG, "onAttach");
        super.onAttach(context);
        if (!(context instanceof Main)) {
            throw new ClassCastException(context.toString() + " can only be called from Main");
        }
    }

    @NonNull
    @Override
    public AppCompatDialog onCreateDialog(Bundle savedInstanceState) {
        final FragmentActivity activity = getActivity();
        if (!(activity instanceof Main)) {
            throw new ClassCastException(activity.toString() + " can only be called from Main");
        }
        Builder builder = new AlertDialog.Builder(activity);
        builder.setIcon(null);
        builder.setTitle(R.string.welcome_title);
        final LayoutInflater inflater = ThemeUtils.getLayoutInflater(activity);
        final View layout = inflater.inflate(R.layout.welcome_tabs, null);
        final SwitchCompat displayImagery = layout.findViewById(R.id.use_imagery);
        final SwitchCompat autoDownload = layout.findViewById(R.id.auto_download);
        final SwitchCompat penSetup = layout.findViewById(R.id.pen_setup);
        final SwitchCompat authorize = layout.findViewById(R.id.authorize);
        final ExtendedViewPager pager = (ExtendedViewPager) layout.findViewById(R.id.pager);
        pager.setAdapter(new ViewPagerAdapter(activity, layout, new int[] { R.id.welcome_page, R.id.settings_page },
                new int[] { R.string.confirm_upload_edits_page, R.string.menu_tags }));
        // set saved state before the on page change listener is set
        if (savedInstanceState != null) {
            displayImagery.setChecked(savedInstanceState.getBoolean(USE_IMAGERY_KEY));
            autoDownload.setChecked(savedInstanceState.getBoolean(AUTO_DOWNLOAD_KEY));
            penSetup.setChecked(savedInstanceState.getBoolean(PEN_SETUP_KEY));
            authorize.setChecked(savedInstanceState.getBoolean(AUTHORIZE_KEY, true));
            pager.setCurrentItem(savedInstanceState.getInt(PAGER_POS_KEY, 0));
        }
        pager.addOnPageChangeListener((OnPageSelectedListener) position -> {
            AlertDialog dialog = ((AlertDialog) getDialog());
            if (dialog == null) {
                Log.e(DEBUG_TAG, "Dialog null");
                return;
            }
            Button positive = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
            Button negative = dialog.getButton(DialogInterface.BUTTON_NEGATIVE);
            positive.clearFocus();
            if (position == WELCOME_PAGE_INDEX) {
                positive.setText(R.string.next);
                positive.setOnClickListener((View v) -> pager.setCurrentItem(SETTINGS_PAGE_INDEX));
                negative.setText(R.string.skip);
                negative.setOnClickListener((View v) -> dismiss());
            }
            if (position == SETTINGS_PAGE_INDEX) {
                positive.setText(R.string.welcome_start);
                positive.setOnClickListener((View v) -> {
                    ((Main) activity).gotoCurrentLocation();
                    ((Main) activity).setFollowGPS(true);

                    Preferences prefs = App.getPreferences(activity);
                    if (displayImagery.isChecked()) {
                        setBestBackground(activity);
                    }

                    prefs.setPanAndZoomAutoDownload(autoDownload.isChecked());

                    boolean penConfig = penSetup.isChecked();
                    prefs.setLargeDragArea(!penConfig);
                    prefs.setWayNodeDragging(penConfig);
                    prefs.setDataStyle(penConfig ? Preferences.DEFAULT_PEN_MAP_STYLE : Preferences.DEFAULT_MAP_STYLE);

                    ((Main) activity).getMap().setPrefs(activity, prefs);

                    dismiss();

                    if (authorize.isChecked()) {
                        Authorize.startForResult(activity, null);
                    }
                });
                negative.setText(R.string.back);
                negative.setOnClickListener((View v) -> pager.setCurrentItem(0));
            }
        });

        String message = getString(R.string.welcome_message);
        if (((Main) activity).isFullScreen()) {
            message = message + getString(R.string.welcome_message_fullscreen);
        }
        ((TextView) layout.findViewById(R.id.welcome_message)).setText(Util.fromHtml(message));
        builder.setView(layout);

        builder.setNegativeButton(R.string.skip, null);
        builder.setPositiveButton(R.string.next, null);
        builder.setNeutralButton(R.string.read_introduction, null);
        AlertDialog dialog = builder.create();
        dialog.setOnShowListener((DialogInterface d) -> {
            Button neutral = dialog.getButton(DialogInterface.BUTTON_NEUTRAL);
            neutral.setOnClickListener((View v) -> {
                Context ctx = getActivity();
                if (ctx instanceof FragmentActivity) {
                    HelpViewer.start((FragmentActivity) ctx, R.string.help_introduction);
                    return;
                }
                Log.e(DEBUG_TAG, "Not a fragment activity");
            });
            Button positive = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
            positive.setOnClickListener((View v) -> pager.setCurrentItem(SETTINGS_PAGE_INDEX));
        });
        return dialog;
    }

    /**
     * Set the best background for the current ViewBox
     * 
     * @param activity the current activity
     */
    private void setBestBackground(@NonNull final FragmentActivity activity) {
        final String[] ids = TileLayerSource.getIds(App.getLogic().getMap().getViewBox(), true, Category.photo, null);
        if (ids.length > 0) {
            TileLayerSource tileSource = TileLayerSource.get(activity, ids[0], false);
            MapTilesLayer<?> tileLayer = ((Main) activity).getMap().getBackgroundLayer();
            tileLayer.setRendererInfo(tileSource);
            try (AdvancedPrefDatabase db = new AdvancedPrefDatabase(activity)) {
                db.setLayerContentId(tileLayer.getIndex(), tileSource.getId());
            }
        } else {
            Log.w(DEBUG_TAG, "No applicable imagery found!");
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d(DEBUG_TAG, "onSaveInstanceState");
        Dialog dialog = getDialog();
        outState.putBoolean(USE_IMAGERY_KEY, ((SwitchCompat) dialog.findViewById(R.id.use_imagery)).isChecked());
        outState.putBoolean(AUTO_DOWNLOAD_KEY, ((SwitchCompat) dialog.findViewById(R.id.auto_download)).isChecked());
        outState.putBoolean(PEN_SETUP_KEY, ((SwitchCompat) dialog.findViewById(R.id.pen_setup)).isChecked());
        outState.putBoolean(AUTHORIZE_KEY, ((SwitchCompat) dialog.findViewById(R.id.authorize)).isChecked());
        outState.putInt(PAGER_POS_KEY, ((ExtendedViewPager) dialog.findViewById(R.id.pager)).getCurrentItem());
    }
}
