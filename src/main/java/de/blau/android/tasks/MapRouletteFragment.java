package de.blau.android.tasks;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import de.blau.android.App;
import de.blau.android.Logic;
import de.blau.android.Main;
import de.blau.android.PostAsyncActionHandler;
import de.blau.android.R;
import de.blau.android.exception.OsmException;
import de.blau.android.osm.BoundingBox;
import de.blau.android.osm.Server;
import de.blau.android.util.GeoMath;
import de.blau.android.util.Util;

/**
 * Dialog fragment to display a MapRoulette task
 * 
 * @author Simon
 *
 */
public class MapRouletteFragment extends TaskFragment {
    private static final String DEBUG_TAG = MapRouletteFragment.class.getSimpleName();

    private static final String TAG = "fragment_maproulette";

    private static final String BUG_KEY = "bug";

    /**
     * Display a dialog for editing MapRoulette Tasks
     * 
     * @param activity the calling FragmentActivity
     * @param t Task we want to edit
     */
    public static void showDialog(@NonNull FragmentActivity activity, @NonNull Task t) {
        dismissDialog(activity);
        try {
            FragmentManager fm = activity.getSupportFragmentManager();
            MapRouletteFragment taskFragment = newInstance(t);
            taskFragment.show(fm, TAG);
        } catch (IllegalStateException isex) {
            Log.e(DEBUG_TAG, "showDialog", isex);
        }
    }

    /**
     * Dismiss the Dialog
     * 
     * @param activity the calling FragmentActivity
     */
    private static void dismissDialog(@NonNull FragmentActivity activity) {
        de.blau.android.dialogs.Util.dismissDialog(activity, TAG);
    }

    /**
     * Create a new fragment to be displayed
     * 
     * @param t Task to show
     * @return the fragment
     */
    private static MapRouletteFragment newInstance(@NonNull Task t) {
        MapRouletteFragment f = new MapRouletteFragment();

        Bundle args = new Bundle();
        args.putSerializable(BUG_KEY, t);

        f.setArguments(args);
        f.setShowsDialog(true);

        return f;
    }

    @Override
    protected <T extends Task> void update(Server server, PostAsyncActionHandler handler, T task) {
        TransferTasks.updateMapRouletteTask(getActivity(), server, (MapRouletteTask) task, false, handler);
    }

    @Override
    protected <T extends Task> ArrayAdapter<CharSequence> setupView(Bundle savedInstanceState, View v, T task) {
        title.setText(R.string.maproulette_task_title);
        commentLabel.setVisibility(View.GONE);
        comment.setVisibility(View.GONE);
        comments.setText(Util.fromHtml(((MapRouletteTask) task).getDescription()));
        MapRouletteChallenge challenge = App.getTaskStorage().getChallenges().get(((MapRouletteTask) task).getParentId());
        if (challenge != null) {
            final StringBuilder explanationsBuilder = new StringBuilder();
            //
            if (challenge.blurb != null && !"".equals(challenge.blurb)) {
                explanationsBuilder.append(challenge.blurb);
            } else if (challenge.description != null && !"".equals(challenge.description)) {
                explanationsBuilder.append(challenge.description);
            }
            //
            if (challenge.instruction != null && !"".equals(challenge.instruction)) {
                if (explanationsBuilder.length() > 0) {
                    explanationsBuilder.append("<br><br>");
                }
                explanationsBuilder.append(challenge.instruction);
            }

            if (explanationsBuilder.length() > 0) {
                TextView instructionText = new TextView(getActivity());
                instructionText.setClickable(true);
                instructionText.setOnClickListener(unused -> showAdditionalText(getActivity(), Util.fromHtml(explanationsBuilder.toString())));
                instructionText.setTextColor(ContextCompat.getColor(getActivity(), R.color.holo_blue_light));
                instructionText.setText(R.string.maproulette_task_explanations);
                elementLayout.addView(instructionText);
            }
        }
        // add a clickable link to the location
        TextView locationText = new TextView(getActivity());
        final double lon = task.getLon() / 1E7D;
        final double lat = task.getLat() / 1E7D;
        if (getActivity() instanceof Main) {
            locationText.setClickable(true);
            locationText.setOnClickListener(unused -> {
                dismiss();
                try {
                    final BoundingBox b = GeoMath.createBoundingBoxForCoordinates(lat, lon, 50);
                    App.getLogic().downloadBox(getActivity(), b, true, () -> {
                        Logic logic = App.getLogic();
                        logic.getViewBox().fitToBoundingBox(logic.getMap(), b);
                        logic.getMap().invalidate();
                    });
                } catch (OsmException e1) {
                    Log.e(DEBUG_TAG, "onCreateDialog got " + e1.getMessage());
                }
            });
        }
        locationText.setTextColor(ContextCompat.getColor(getActivity(), R.color.holo_blue_light));
        locationText.setText(getString(R.string.maproulette_task_coords, lon, lat));
        elementLayout.addView(locationText);
        return ArrayAdapter.createFromResource(getActivity(), R.array.maproulette_state, android.R.layout.simple_spinner_item);
    }

    @Override
    protected <T extends Task> void enableStateSpinner(T task) {
        state.setEnabled(!task.isNew());
    }
}
