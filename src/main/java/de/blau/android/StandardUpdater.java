package de.blau.android;

import android.support.annotation.NonNull;
import de.blau.android.filter.Filter;

public class StandardUpdater implements Filter.Update {

    final Logic logic;
    final Main  main;

    /**
     * Construct a new updater
     * 
     * @param logic the current instance of Logic
     * @param main the current instance of Main
     */
    StandardUpdater(@NonNull Logic logic, @NonNull Main main) {
        this.logic = logic;
        this.main = main;
    }

    @Override
    public void execute() {
        logic.invalidateMap();
        main.scheduleAutoLock();
    }
}
