package io.vespucci;

import java.io.Serializable;

import android.util.Log;
import android.view.MenuItem;
import androidx.annotation.NonNull;

/**
 * On some devices Android tries to serialize the bottom menu, causing a runtime crash if we create the listener in any
 * form in Main, this is an attempt at a workaround.
 * 
 * @author simon
 *
 */
public class BottomBarClickListener implements androidx.appcompat.widget.ActionMenuView.OnMenuItemClickListener, Serializable {
    private static final long serialVersionUID = 1L;
    transient Main            main;

    /**
     * Construct a new listener
     * 
     * @param main the current instance of main
     */
    BottomBarClickListener(@NonNull Main main) {
        this.main = main;
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        if (main != null) {
            return main.onOptionsItemSelected(item);
        }
        Log.d("MyOnMenuClickListner", "main is null");
        return true;
    }
}
