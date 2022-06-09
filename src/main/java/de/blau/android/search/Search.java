package de.blau.android.search;

import java.io.ByteArrayInputStream;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDialog;
import androidx.fragment.app.FragmentActivity;
import ch.poole.osm.josmfilterparser.Condition;
import ch.poole.osm.josmfilterparser.JosmFilterParser;
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
import de.blau.android.util.Snack;

/**
 * Ask the user for for a JOSM filter/search expression, select and zoom to any results
 * 
 * @author simon
 *
 */
public final class Search {

    protected static final String DEBUG_TAG = "Search";

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
                            } catch (ParseException pex) {
                                return pex.getMessage();
                            } catch (Error err) { // NOSONAR
                                return err.getMessage();
                            }

                            Wrapper wrapper = new Wrapper(activity);

                            try {
                                result = wrapper.getMatchingElementsInternal(condition);
                            } catch (Exception e) {
                                return e.getMessage();
                            }
                            if (result.isEmpty()) {
                                return activity.getString(R.string.toast_nothing_found);
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
                                Snack.toastTopWarning(activity, errorMsg);
                            }
                        }
                    }.execute();
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
