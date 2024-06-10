package de.blau.android.search;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.io.ByteArrayInputStream;
import java.util.List;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDialog;
import androidx.fragment.app.FragmentActivity;
import ch.poole.osm.josmfilterparser.Condition;
import ch.poole.osm.josmfilterparser.JosmFilterParser;
import ch.poole.osm.josmfilterparser.Overpass;
import ch.poole.osm.josmfilterparser.ParseException;
import de.blau.android.App;
import de.blau.android.Logic;
import de.blau.android.Main;
import de.blau.android.R;
import de.blau.android.dialogs.Progress;
import de.blau.android.dialogs.TextLineDialog;
import de.blau.android.easyedit.EasyEditManager;
import de.blau.android.osm.Node;
import de.blau.android.osm.Relation;
import de.blau.android.osm.Way;
import de.blau.android.search.Wrapper.SearchResult;
import de.blau.android.util.ExecutorTask;
import de.blau.android.util.ScreenMessage;

/**
 * Ask the user for for a JOSM filter/search expression, select and zoom to any results
 * 
 * @author simon
 *
 */
public final class Search {

    private static final int      TAG_LEN   = Math.min(LOG_TAG_LEN, Search.class.getSimpleName().length());
    protected static final String DEBUG_TAG = Search.class.getSimpleName().substring(0, TAG_LEN);

    private static final String OVERPASS_FOOTER = "\n(._;>;);\nout meta;";
    private static final String OVERPASS_HEADER = "[out:xml][timeout:90];\n";

    private static AppCompatDialog dialog;

    /**
     * Private constructor
     */
    private Search() {
        // don't allow instantiating of this class
    }

    /**
     * Show a dialog and ask the user for input
     * 
     * @param activity the calling FragmentActivity
     */
    public static void search(@NonNull final FragmentActivity activity) {
        final Logic logic = App.getLogic();
        final List<String> lastSearches = logic.getLastObjectSearches();

        dialog = TextLineDialog.get(activity, R.string.search_objects_title, R.string.search_objects_hint, R.string.search_objects_use_regexps, lastSearches,
                activity.getString(R.string.search), (input, useRegexp) -> {
                    final String text = input.getText().toString();
                    if ("".equals(text)) {
                        return;
                    }
                    new ExecutorTask<Void, Void, String>(logic.getExecutorService(), logic.getHandler()) {
                        SearchResult result;

                        @Override
                        protected void onPreExecute() {
                            Progress.showDialog(activity, Progress.PROGRESS_SEARCHING);
                        }

                        @Override
                        protected String doInBackground(Void param) {
                            Condition condition = null;
                            try {
                                JosmFilterParser parser = new JosmFilterParser(new ByteArrayInputStream(text.getBytes()));
                                condition = parser.condition(useRegexp);
                                result = new Wrapper(activity).getMatchingElementsInternal(condition);
                                if (result.isEmpty()) {
                                    return activity.getString(R.string.toast_nothing_found);
                                }
                            } catch (Exception e) {
                                return e.getMessage();
                            } catch (Error err) { // NOSONAR
                                return err.getMessage();
                            }
                            return null;
                        }

                        @Override
                        protected void onPostExecute(String errorMsg) {
                            Progress.dismissDialog(activity, Progress.PROGRESS_SEARCHING);
                            if (errorMsg == null) {
                                logic.pushObjectSearch(text);
                                if (activity instanceof Main) {
                                    selectResult((Main) activity, logic, result);
                                }
                                dismiss();
                            } else {
                                ScreenMessage.toastTopWarning(activity, errorMsg);
                            }
                        }
                    }.execute();
                }, activity.getString(R.string.search_objects_query_overpass), (input, useRegexp) -> {
                    try {
                        final String text = input.getText().toString();
                        JosmFilterParser parser = new JosmFilterParser(new ByteArrayInputStream(text.getBytes()));
                        StringBuilder result = new StringBuilder();
                        result.append(OVERPASS_HEADER);
                        result.append(Overpass.transform(parser.condition(useRegexp)));
                        result.append(OVERPASS_FOOTER);
                        Main.showOverpassConsole(activity, result.toString());
                        logic.pushObjectSearch(text);
                        dismiss();
                    } catch (UnsupportedOperationException | ParseException pex) {
                        Log.w(DEBUG_TAG, "Exception " + pex.getMessage());
                        ScreenMessage.toastTopWarning(activity, pex.getMessage());
                    } catch (Error err) { // NOSONAR
                        Log.w(DEBUG_TAG, "Error " + err.getMessage());
                        ScreenMessage.toastTopWarning(activity, err.getMessage());
                    }
                }, false);
        dialog.show();
    }

    /**
     * Dismiss the dialog
     */
    private static void dismiss() {
        if (dialog != null) {
            dialog.dismiss();
        }
    }

    /**
     * Select the elements that we found
     * 
     * @param main current instance of Main
     * @param logic current instance of Logic
     * @param result the SearchResult
     */
    public static void selectResult(@NonNull final Main main, @NonNull final Logic logic, @NonNull final SearchResult result) {
        EasyEditManager easyEditManager = main.getEasyEditManager();
        if (easyEditManager.inElementSelectedMode()) {
            easyEditManager.finish();
        }
        logic.deselectAll();
        for (Node n : result.nodes) {
            logic.addSelectedNode(n);
        }
        for (Way w : result.ways) {
            logic.addSelectedWay(w);
        }
        for (Relation r : result.relations) {
            logic.addSelectedRelation(r);
        }
        easyEditManager.editElements();
        main.zoomTo(logic.getSelectedElements());
        main.invalidateMap();
    }
}
