package io.vespucci.tasks;

import static io.vespucci.contract.Constants.LOG_TAG_LEN;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.mapbox.geojson.Feature;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import io.vespucci.R;
import io.noties.markwon.Markwon;
import io.vespucci.App;
import io.vespucci.Logic;
import io.vespucci.Main;
import io.vespucci.PostAsyncActionHandler;
import io.vespucci.exception.OsmException;
import io.vespucci.osm.BoundingBox;
import io.vespucci.osm.Node;
import io.vespucci.osm.OsmElement;
import io.vespucci.osm.Relation;
import io.vespucci.osm.Server;
import io.vespucci.osm.Way;
import io.vespucci.tasks.Task.State;
import io.vespucci.util.ClipboardUtils;
import io.vespucci.util.GeoMath;
import io.vespucci.util.ThemeUtils;
import io.vespucci.util.Util;

/**
 * Dialog fragment to display a MapRoulette task
 * 
 * @author Simon
 *
 */
public class MapRouletteFragment extends TaskFragment {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, MapRouletteFragment.class.getSimpleName().length());
    private static final String DEBUG_TAG = MapRouletteFragment.class.getSimpleName().substring(0, TAG_LEN);

    private static final String TAG = "fragment_maproulette";

    private static final String BUG_KEY = "bug";

    private static final String COPYABLE = "copyable";

    private int lightBlue;

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
        io.vespucci.dialogs.Util.dismissDialog(activity, TAG);
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
        TransferTasks.updateMapRouletteTask(getActivity(), server, App.getPreferences(getContext()).getMapRouletteServer(), (MapRouletteTask) task, false,
                handler);
    }

    @Override
    protected <T extends Task> ArrayAdapter<CharSequence> setupView(Bundle savedInstanceState, View v, T task) {
        title.setText(R.string.maproulette_task_title);
        commentLabel.setVisibility(View.GONE);
        comment.setVisibility(View.GONE);
        final MapRouletteTask mapRouletteTask = (MapRouletteTask) task;
        comments.setText(Util.fromHtml(mapRouletteTask.getChallengeName()));
        lightBlue = ContextCompat.getColor(getContext(), R.color.holo_blue_light);

        if (getContext() instanceof Main && ((Main) getContext()).isConnected()) {
            // https://maproulette.org/challenge/15908/task/75702116
            comments.setOnClickListener(unused -> getContext().startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse(prefs.getMapRouletteServer() + "challenge/" + mapRouletteTask.getParentId() + "/task/" + mapRouletteTask.getId()))));
            comments.setTextColor(lightBlue);
        }

        final Markwon markwon = Markwon.create(getContext());
        List<Feature> features = mapRouletteTask.getFeatures();
        Feature feature = features != null && !features.isEmpty() ? features.get(0) : null;
        final String taskBlurb = mapRouletteTask.getBlurb();
        if (Util.notEmpty(taskBlurb)) {
            elementLayout.addView(format(markwon, taskBlurb, feature));
        }

        MapRouletteChallenge challenge = App.getTaskStorage().getChallenges().get(mapRouletteTask.getParentId());
        if (challenge != null) {
            final List<View> views = new ArrayList<>();
            //
            final String challengeBlurb = challenge.getBlurb();
            if (notEmptyAndNotADuplicate(taskBlurb, challengeBlurb)) {
                views.add(format(markwon, challengeBlurb, feature));
            }
            final String description = challenge.getDescription();
            if (notEmptyAndNotADuplicate(challengeBlurb, description)) {
                views.add(format(markwon, description, feature));
            }
            addDocumentation(R.string.maproulette_task_explanations, views);

            final String instruction = challenge.getInstruction();
            if (notEmptyAndNotADuplicate(instruction, description) && notEmptyAndNotADuplicate(instruction, challengeBlurb)) {
                addDocumentation(R.string.maproulette_task_instructions, Util.wrapInList(format(markwon, instruction, feature)));
            }
        }

        final View ruler = inflater.inflate(R.layout.ruler, null);
        elementLayout.addView(ruler);

        // add a clickable link to the location
        elementLayout.addView(getLocationLink(task.getLon(), task.getLat()));
        return ArrayAdapter.createFromResource(getContext(), R.array.maproulette_state, android.R.layout.simple_spinner_item);
    }

    /**
     * Create a clickable TextView for a location
     * 
     * @param lonE7 WGS84 * 1E7 longitude
     * @param latE7 WGS84 * 1E7 latitude
     * @return a TextView
     */
    @NonNull
    private TextView getLocationLink(int lonE7, int latE7) {
        TextView locationText = new TextView(getContext());
        final double lon = lonE7 / 1E7D;
        final double lat = latE7 / 1E7D;
        if (getContext() instanceof Main) {
            locationText.setClickable(true);
            locationText.setOnClickListener(unused -> {
                dismiss();
                try {
                    final BoundingBox b = GeoMath.createBoundingBoxForCoordinates(lat, lon, 50);
                    App.getLogic().downloadBox(getContext(), b, true, () -> {
                        Logic logic = App.getLogic();
                        logic.getViewBox().fitToBoundingBox(logic.getMap(), b);
                        logic.getMap().invalidate();
                    });
                } catch (OsmException e1) {
                    Log.e(DEBUG_TAG, "onCreateDialog got " + e1.getMessage());
                }
            });
        }
        locationText.setTextColor(lightBlue);
        locationText.setText(getString(R.string.maproulette_task_coords, lon, lat));
        return locationText;
    }

    /**
     * Check if text is not and and not a duplicate of other
     * 
     * @param text the text
     * @param other the other, potentially duplicate, text
     * @return true if text is not empty and not a duplicate of other
     */
    private boolean notEmptyAndNotADuplicate(final String text, final String other) {
        return Util.notEmpty(other) && !other.equals(text);
    }

    /**
     * Add a documentation "link" that opens a modal
     * 
     * @param linkRes resource id for the link text
     * @param views List of Views to display
     */
    private void addDocumentation(int linkRes, @NonNull final List<View> views) {
        if (!views.isEmpty()) {
            TextView text = new TextView(getContext());
            text.setClickable(true);
            text.setOnClickListener(unused -> showAdditionalText(getContext(), Util.fromHtml(""), views));
            text.setTextColor(lightBlue);
            text.setText(linkRes);
            elementLayout.addView(text);
        }
    }

    @Override
    protected <T extends Task> void enableStateSpinner(T task) {
        state.setEnabled(!task.isNew());
    }

    @Override
    protected State pos2state(int position) {
        String[] array = getResources().getStringArray(R.array.maproulette_state_values);
        return State.valueOf(array[position]);
    }

    @Override
    protected int state2pos(State state) {
        return state2pos(getResources(), state);
    }

    /**
     * Convert State to a position
     * 
     * @param resources Android Resources
     * @param state the state
     * @return the position
     */
    static int state2pos(@NonNull Resources resources, @NonNull State state) {
        String[] array = resources.getStringArray(R.array.maproulette_state_values);
        return Arrays.asList(array).indexOf(state.name());
    }

    /**
     * Turn an input string from maproulette in to something useful
     * 
     * Note that because Markwon uses commonmark semantics, we need to enforce hard breaks
     * 
     * @param markwon the Markwon instance
     * @param input the input String
     * @param feature the GeoJSON Feature for this task if any
     * @return a LinearLayout representing the contents of input
     */
    @NonNull
    LinearLayout format(@NonNull Markwon markwon, @NonNull String input, @Nullable Feature feature) {
        return handleShortcodes(markwon.toMarkdown(deMoustache(input, feature).replaceAll("\\r?\\n", Matcher.quoteReplacement("  \n"))));
    }

    /**
     * Replace occurrences of {{property}} with the value of property in the features attributes
     * 
     * @param input the input string
     * @param feature GeoJson Feature with the properties
     * @return a String with any moustache templates replaced
     */
    @NonNull
    private String deMoustache(@NonNull String input, @Nullable Feature feature) {
        if (feature == null) {
            return input;
        }
        // primitive moustache handling
        StringBuilder builder = new StringBuilder();
        StringBuilder moustache = new StringBuilder();
        boolean preMoustache = false;
        boolean inMoustache = false;
        for (char c : input.toCharArray()) {
            switch (c) {
            case '{':
                inMoustache = preMoustache;
                preMoustache = !preMoustache;
                break;
            case '}':
                if (inMoustache) {
                    String key = moustache.toString();
                    if (feature.hasProperty(key)) {
                        builder.append(feature.getStringProperty(key));
                    }
                    moustache.setLength(0);
                    inMoustache = false;
                }
                break;
            default:
                if (inMoustache) {
                    moustache.append(c);
                } else {
                    builder.append(c);
                }
            }
        }
        return builder.toString();
    }

    private static final Pattern SHORT_CODE_PATTERN = Pattern.compile("(\\{\\{\\{[^\\}]+\\}\\}\\})|(\\[[^\\]]+\\])(?=[^(]|$)");
    private static final Pattern CODE_PATTERN       = Pattern.compile("\\[(" + COPYABLE + "|select|checkbox)\\s+\\\"([^\\\"]*)\\\"(.*)\\]");
    private static final Pattern ELEMENT_PATTERN    = Pattern.compile("(n|node|w|way|r|relation)(?:\\s|\\/)*([0-9]+)(?:\\s*,\\s*)?");

    private static final LinearLayout.LayoutParams LP = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, 1.0f);

    /**
     * Handle Maproulette short codes
     * 
     * @param input a Spanned input
     * @return a LineraLayout containing text and converted short codes
     */
    @NonNull
    private LinearLayout handleShortcodes(@NonNull Spanned input) {
        final Context ctx = getContext();
        LinearLayout layout = new LinearLayout(ctx);
        layout.setOrientation(LinearLayout.VERTICAL);
        Matcher matcher = SHORT_CODE_PATTERN.matcher(input);
        int pos = 0;
        while (matcher.find(pos)) {
            String match = matcher.group();
            int start = matcher.start();
            layout.addView(getTextView(ctx, input.subSequence(pos, start)));
            pos = matcher.end();
            // process the short codes
            Matcher codeMatcher = CODE_PATTERN.matcher(match);
            if (codeMatcher.matches()) {
                String shortcode = codeMatcher.group(1);
                if (COPYABLE.equals(shortcode)) {
                    ImageButton button = new ImageButton(ctx);
                    button.setImageResource(ThemeUtils.getResIdFromAttribute(ctx, R.attr.menu_copy));
                    CharSequence copyText = input.subSequence(start + codeMatcher.start(2), start + codeMatcher.end(2));
                    layout.addView(getTextView(ctx, copyText));
                    button.setOnClickListener((View v) -> ClipboardUtils.copyText(ctx, ctx.getString(R.string.maproulette_description, ""), copyText));
                    button.setBackgroundColor(ContextCompat.getColor(ctx, R.color.osm_green));
                    layout.addView(button);
                } else {
                    Log.w(DEBUG_TAG, "unhandled shortcode " + shortcode);
                }
            } else {
                Matcher elementMatcher = ELEMENT_PATTERN.matcher(match);
                int elementPos = 0;
                while (elementMatcher.find(elementPos)) {
                    try {
                        String elementType = normalizeType(elementMatcher.group(1));
                        long elementId = Long.parseLong(elementMatcher.group(2));
                        layout.addView(createElementLink(ctx, elementType, elementId));
                    } catch (IllegalArgumentException ex) {
                        Log.e(DEBUG_TAG, ex.getMessage());
                    }
                    elementPos = elementMatcher.end();
                }
            }
        }
        layout.addView(getTextView(ctx, pos > 0 ? input.subSequence(pos, input.length()) : input));
        return layout;
    }

    /**
     * Add text to new TextView
     * 
     * @param ctx an Android Context
     * @param input the input text
     * @return a TextView
     */
    @NonNull
    private TextView getTextView(@NonNull Context ctx, @NonNull CharSequence input) {
        TextView text = new TextView(ctx);
        text.setText(input);
        text.setLayoutParams(LP);
        text.setMovementMethod(LinkMovementMethod.getInstance());
        return text;
    }

    /**
     * Create a TextView that will download / edit the element on click
     * 
     * @param ctx an ANdroid Context
     * @param elementType the element type
     * @param elementId the element id
     * @return a TextView
     */
    @NonNull
    private TextView createElementLink(@NonNull final Context ctx, @NonNull final String elementType, @NonNull final long elementId) {
        TextView tv = new TextView(ctx);
        final OsmElement element = App.getDelegator().getOsmElement(elementType, elementId);
        if (ctx instanceof Main) { // only make clickable if in Main
            tv.setClickable(true);
            tv.setOnClickListener(unused -> {
                final Task task = getTask();
                dismiss();
                final int lonE7 = task.getLon();
                final int latE7 = task.getLat();
                final FragmentActivity activity = getActivity();
                if (activity instanceof Main) { // activity may have vanished so re-check
                    final PostAsyncActionHandler editElement = () -> {
                        OsmElement e = App.getDelegator().getOsmElement(elementType, elementId);
                        if (e != null) {
                            ((Main) activity).zoomToAndEdit(lonE7, latE7, e);
                        }
                    };
                    if (element == null) { // download
                        try {
                            BoundingBox b = GeoMath.createBoundingBoxForCoordinates(latE7 / 1E7D, lonE7 / 1E7, 50);
                            App.getLogic().downloadBox(activity, b, true, editElement);
                        } catch (OsmException e1) {
                            Log.e(DEBUG_TAG, "setupView got " + e1.getMessage());
                        }
                    } else {
                        editElement.onSuccess();
                    }
                }
            });
        }
        tv.setTextColor(lightBlue);
        tv.setText(element != null ? element.getDescription(ctx) : Util.elementTypeId(ctx, elementType, elementId));
        return tv;
    }

    /**
     * Normalize a OSM element type string
     * 
     * @param type the input string
     * @return the normalized form of the type
     */
    @NonNull
    private String normalizeType(@NonNull String type) {
        switch (type.toLowerCase()) {
        case "n":
        case "node":
            return Node.NAME;
        case "w":
        case "way":
            return Way.NAME;
        case "r":
        case "relation":
            return Relation.NAME;
        default:
            throw new IllegalArgumentException("Unknown element type " + type);
        }
    }
}
