package de.blau.android.propertyeditor.tagform;

import java.util.List;

import com.davemorrissey.labs.subscaleview.ImageSource;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;

import android.util.Log;
import android.widget.TextView;
import androidx.annotation.NonNull;
import de.blau.android.R;
import de.blau.android.contract.Paths;
import de.blau.android.presets.PresetListEntry;
import de.blau.android.presets.PresetListEntryWithIcon;
import de.blau.android.util.ImageLoader;
import de.blau.android.util.StringWithDescription;
import de.blau.android.util.Value;

public class ComboImageLoader extends ImageLoader {

    private static final String DEBUG_TAG = ComboImageLoader.class.getSimpleName().substring(0, Math.min(23, ComboImageLoader.class.getSimpleName().length()));

    private static final long serialVersionUID = 1L;

    private final List<Value> values; // NOSONAR
    private final String      key;

    /**
     * Construct a new loader
     * 
     * @param key the key
     * @param values a list of values
     */
    ComboImageLoader(@NonNull String key, @NonNull List<Value> values) {
        this.key = key;
        this.values = values;
    }

    @Override
    public void load(SubsamplingScaleImageView view, String uri) {
        if ("".equals(uri)) {
            view.setImage(ImageSource.resource(R.drawable.no_image));
        } else if (uri.startsWith(Paths.DELIMITER)) {
            view.setImage(ImageSource.uri(uri));
        } else {
            view.setImage(ImageSource.asset(uri));
        }
    }

    @Override
    public void setTitle(TextView title, int position) {
        final Value v = values.get(position);
        if (v instanceof StringWithDescription) {
            title.setText(((StringWithDescription) v).getDescription());
        }
    }

    @Override
    public void setDescription(TextView description, int position) {
        final Value v = values.get(position);
        if (v instanceof PresetListEntry) {
            description.setText(((PresetListEntry) v).getLongDescription());
        }
    }

    @Override
    public void onSelected(int pos) {
        final Value v = values.get(pos);
        if (v instanceof PresetListEntryWithIcon) {
            update((PresetListEntryWithIcon) v);
        }
    }

    /**
     * Update the selected value
     * 
     * @param v the selected value
     */
    private void update(final StringWithDescription v) {
        if (parentFragment instanceof TagFormFragment) {
            ((TagFormFragment) parentFragment).updateSingleValue(key, v.getValue());
            ((TagFormFragment) parentFragment).updateDialogRow(key, v);
        } else {
            Log.e(DEBUG_TAG, "caller not a TagFormFragment " + parentFragment);
        }
    }

    @Override
    public void clearSelection() {
        update(new StringWithDescription(""));
    }
}
