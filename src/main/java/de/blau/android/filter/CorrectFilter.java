package de.blau.android.filter;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.blau.android.App;
import de.blau.android.osm.OsmElement;
import de.blau.android.validation.Validator;

/**
 * Filter for filtering on Validation status
 * 
 * @author simon
 *
 */
public class CorrectFilter extends CommonFilter {

    /**
     * 
     */
    private static final long   serialVersionUID = 1L;
    private static final String DEBUG_TAG        = CorrectFilter.class.getSimpleName().substring(0, Math.min(23, CorrectFilter.class.getSimpleName().length()));

    private transient Context   context;
    private transient Validator validator;

    /**
     * Construct a new filter
     */
    public CorrectFilter() {
        this(null);
    }

    /**
     * Construct a new filter
     * 
     * @param context an Android Context
     */
    public CorrectFilter(@Nullable Context context) {
        super();
        Log.d(DEBUG_TAG, "Constructor");
        init(context);
        //
    }

    @Override
    public void init(Context context) {
        Log.d(DEBUG_TAG, "init");
        this.context = context;
        validator = App.getDefaultValidator(context);
        clear();
    }

    @Override
    protected Include filter(@NonNull OsmElement e) {
        Include include = Include.DONT;
        if (e.hasProblem(context, validator) != Validator.OK) {
            return Include.INCLUDE;
        }
        return include;
    }
}
