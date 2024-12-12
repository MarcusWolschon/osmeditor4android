package de.blau.android.layer.tasks;

import java.util.List;

import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TableLayout;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import de.blau.android.App;
import de.blau.android.R;
import de.blau.android.dialogs.LayerInfo;
import de.blau.android.dialogs.TableLayoutUtils;
import de.blau.android.tasks.MapRouletteTask;
import de.blau.android.tasks.Note;
import de.blau.android.tasks.OsmoseBug;
import de.blau.android.tasks.Task;
import de.blau.android.tasks.TaskStorage;
import de.blau.android.tasks.Todo;

public class TaskLayerInfo extends LayerInfo {
    private static final String DEBUG_TAG = TaskLayerInfo.class.getSimpleName().substring(0, Math.min(23, TaskLayerInfo.class.getSimpleName().length()));

    @Override
    protected View createView(@Nullable ViewGroup container) {
        Log.d(DEBUG_TAG, "createView");

        ScrollView sv = createEmptyView(container);
        FragmentActivity activity = getActivity();
        TableLayout tableLayout = (TableLayout) sv.findViewById(R.id.element_info_vertical_layout);
        TableLayout.LayoutParams tp = new TableLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        tp.setMargins(10, 2, 10, 2);
        tableLayout.setColumnShrinkable(1, false);

        TaskStorage taskStorage = App.getTaskStorage();
        if (taskStorage == null) {
            Log.e(DEBUG_TAG, "null TaskStorage");
            return sv;
        }
        TableLayout t2 = (TableLayout) sv.findViewById(R.id.element_info_vertical_layout_2);
        t2.addView(TableLayoutUtils.createFullRowTitle(activity, getString(R.string.data_in_memory), tp));
        t2.addView(TableLayoutUtils.createRow(activity, "", getString(R.string.total), getString(R.string.changed), tp));
        List<Task> tasks = taskStorage.getTasks();
        int osmoseCount = 0;
        int osmoseChangedCount = 0;
        int customCount = 0;
        int customChangedCount = 0;
        int noteCount = 0;
        int noteChangedCount = 0;
        int maprouletteCount = 0;
        int maprouletteChangedCount = 0;
        for (Task t : tasks) {
            boolean changed = t.hasBeenChanged();
            if (t instanceof Todo) {
                customCount++;
                if (changed || t.isClosed()) { //changed is only true when skipped
                    customChangedCount++;
                }
            } else if (t instanceof OsmoseBug) {
                osmoseCount++;
                if (changed) {
                    osmoseChangedCount++;
                }
            } else if (t instanceof Note) {
                noteCount++;
                if (changed) {
                    noteChangedCount++;
                }
            } else if (t instanceof MapRouletteTask) {
                maprouletteCount++;
                if (changed) {
                    maprouletteChangedCount++;
                }
            }
        }
        t2.addView(TableLayoutUtils.createRow(activity, getString(R.string.bugfilter_notes_entry), Integer.toString(noteCount),
                Integer.toString(noteChangedCount), tp, -1, -1));
        t2.addView(TableLayoutUtils.createRow(activity, getString(R.string.bugfilter_osmose), Integer.toString(osmoseCount),
                Integer.toString(osmoseChangedCount), tp, -1, -1));
        t2.addView(TableLayoutUtils.createRow(activity, getString(R.string.bugfilter_todo_entry), Integer.toString(customCount),
                Integer.toString(customChangedCount), tp, -1, -1));
        t2.addView(TableLayoutUtils.createRow(activity, getString(R.string.bugfilter_maproulette_entry), Integer.toString(maprouletteCount),
                Integer.toString(maprouletteChangedCount), tp, -1, -1));
        t2.addView(TableLayoutUtils.createRow(activity, getString(R.string.bounding_boxes), Integer.toString(taskStorage.getBoundingBoxes().size()), null, tp,
                -1, -1));
        return sv;
    }
}
