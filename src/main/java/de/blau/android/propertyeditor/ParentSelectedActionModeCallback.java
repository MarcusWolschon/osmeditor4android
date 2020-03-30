package de.blau.android.propertyeditor;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.appcompat.view.ActionMode;
import android.view.Menu;
import android.widget.LinearLayout;
import de.blau.android.R;

public class ParentSelectedActionModeCallback extends SelectedRowsActionModeCallback {

    /**
     * ActionModeCallback for when a parent relation has been selected
     * 
     * @param caller the calling Fragment
     * @param rows the Layout holding the rows
     */
    public ParentSelectedActionModeCallback(@NonNull Fragment caller, @NonNull LinearLayout rows) {
        super(caller, rows);
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        super.onCreateActionMode(mode, menu);
        mode.setTitle(R.string.tag_action_parents_title);
        return true;
    }
}
