package de.blau.android.search;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatDialog;
import android.widget.EditText;
import ch.poole.osm.josmfilterparser.Condition;
import ch.poole.osm.josmfilterparser.JosmFilterParser;
import ch.poole.osm.josmfilterparser.ParseException;
import ch.poole.osm.josmfilterparser.Type;
import de.blau.android.App;
import de.blau.android.Logic;
import de.blau.android.Main;
import de.blau.android.R;
import de.blau.android.dialogs.Progress;
import de.blau.android.dialogs.TextLineDialog;
import de.blau.android.easyedit.EasyEditManager;
import de.blau.android.osm.Node;
import de.blau.android.osm.Relation;
import de.blau.android.osm.StorageDelegator;
import de.blau.android.osm.Way;
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
        List<String> lastSearches = logic.getLastObjectSearches();
        List<Node> nodeResult = new ArrayList<>();
        List<Way> wayResult = new ArrayList<>();
        List<Relation> relationResult = new ArrayList<>();

        dialog = TextLineDialog.get(activity, R.string.search_objects_title, R.string.search_objects_hint, lastSearches, activity.getString(R.string.search),
                new TextLineDialog.TextLineInterface() {
                    @Override
                    public void processLine(EditText input) {
                        final String text = input.getText().toString();
                        if ("".equals(text)) {
                            return;
                        }
                        new AsyncTask<Void, Void, String>() {

                            @Override
                            protected void onPreExecute() {
                                Progress.showDialog(activity, Progress.PROGRESS_SEARCHING);
                            }

                            @Override
                            protected String doInBackground(Void... params) {
                                Condition condition = null;
                                try {
                                    JosmFilterParser parser = new JosmFilterParser(new ByteArrayInputStream(text.getBytes()));
                                    condition = parser.condition();
                                } catch (ParseException pex) {
                                    return pex.getMessage();
                                } catch (Error err) { // NOSONAR
                                    return err.getMessage();
                                }

                                nodeResult.clear();
                                wayResult.clear();
                                relationResult.clear();

                                Wrapper wrapper = new Wrapper(activity);
                                StorageDelegator delegator = App.getDelegator();
                                try {
                                    for (Node n : delegator.getCurrentStorage().getNodes()) {
                                        wrapper.setElement(n);
                                        if (condition.eval(Type.NODE, wrapper, n.getTags())) {
                                            nodeResult.add(n);
                                        }
                                    }
                                    for (Way w : delegator.getCurrentStorage().getWays()) {
                                        wrapper.setElement(w);
                                        if (condition.eval(Type.WAY, wrapper, w.getTags())) {
                                            wayResult.add(w);
                                        }
                                    }
                                    for (Relation r : delegator.getCurrentStorage().getRelations()) {
                                        wrapper.setElement(r);
                                        if (condition.eval(Type.RELATION, wrapper, r.getTags())) {
                                            relationResult.add(r);
                                        }
                                    }
                                } catch (Exception e) {
                                    return e.getMessage();
                                }
                                if (nodeResult.isEmpty() && wayResult.isEmpty() && relationResult.isEmpty()) {
                                    return activity.getString(R.string.toast_nothing_found);
                                }
                                return null;
                            }

                            @Override
                            protected void onPostExecute(String result) {
                                Progress.dismissDialog(activity, Progress.PROGRESS_SEARCHING);
                                if (result == null) {
                                    logic.pushObjectSearch(text);
                                    if (activity instanceof Main) {
                                        Main main = (Main) activity;
                                        EasyEditManager easyEditManager = main.getEasyEditManager();
                                        if (easyEditManager.inElementSelectedMode()) {
                                            easyEditManager.finish();
                                        }
                                        logic.deselectAll();
                                        for (Node n : nodeResult) {
                                            logic.addSelectedNode(n);
                                        }
                                        for (Way w : wayResult) {
                                            logic.addSelectedWay(w);
                                        }
                                        for (Relation r : relationResult) {
                                            logic.addSelectedRelation(r);
                                        }
                                        easyEditManager.editElements();
                                        main.zoomTo(logic.getSelectedElements());
                                        main.invalidateMap();
                                    }
                                    dismiss();
                                } else {
                                    Snack.toastTopError(activity, result);
                                }
                            }
                        }.execute();
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

}
