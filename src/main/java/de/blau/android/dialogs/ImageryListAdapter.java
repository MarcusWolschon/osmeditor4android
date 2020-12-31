package de.blau.android.dialogs;

import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.CompoundButton.OnCheckedChangeListener;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatRadioButton;
import androidx.recyclerview.widget.RecyclerView;
import de.blau.android.resources.TileLayerSource;

public class ImageryListAdapter extends RecyclerView.Adapter<ImageryListAdapter.ImageryViewHolder> {
    private String[]                                          names;
    private String[]                                          ids;
    private String                                            currentId;
    private final LayoutParams                                buttonLayoutParams;
    private android.widget.RadioGroup.OnCheckedChangeListener groupChangeListener = null;

    private int selected = -1;

    public static class ImageryViewHolder extends RecyclerView.ViewHolder {
        AppCompatRadioButton button;

        /**
         * Create a new ViewHolder
         * 
         * @param v the RadioButton that will be displayed
         */
        public ImageryViewHolder(@NonNull AppCompatRadioButton v) {
            super(v);
            button = v;
        }
    }

    /**
     * Create a new adapter
     * 
     * @param names an array with imagery names
     * @param currentId an array with imagery ids
     * @param isOverlay the current imagery id
     * @param buttonLayoutParams layout params for the RadioButtons
     * @param groupChangeListener a listener to call when a RadioButton has been selected
     */
    public ImageryListAdapter(@NonNull String[] ids, String currentId, @Nullable boolean isOverlay, @NonNull LayoutParams buttonLayoutParams,
            @NonNull android.widget.RadioGroup.OnCheckedChangeListener groupChangeListener) {
        setIds(ids, isOverlay, false);
        this.currentId = currentId;
        this.buttonLayoutParams = buttonLayoutParams;
        this.groupChangeListener = groupChangeListener;
    }

    final OnCheckedChangeListener onCheckedChangeListener = (buttonView, isChecked) -> {
        Integer position = (Integer) buttonView.getTag();
        if (position != null) {
            ImageryListAdapter.this.notifyItemChanged(selected);
            currentId = ids[position];
            selected = position;
            groupChangeListener.onCheckedChanged(null, position);
        }
    };

    @Override
    public ImageryListAdapter.ImageryViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final AppCompatRadioButton button = new AppCompatRadioButton(parent.getContext());
        button.setLayoutParams(buttonLayoutParams);
        return new ImageryViewHolder(button);
    }

    @Override
    public void onBindViewHolder(ImageryViewHolder holder, int position) {
        holder.button.setText(names[position]);
        holder.button.setTag(position);
        holder.button.setOnCheckedChangeListener(null);
        if (ids[position].equals(currentId)) {
            holder.button.setChecked(true);
            selected = position;
        } else {
            holder.button.setChecked(false);
        }
        holder.button.setOnCheckedChangeListener(onCheckedChangeListener);
    }

    @Override
    public int getItemCount() {
        return names.length;
    }

    /**
     * Set the ids and name arrays that are going to be display
     * 
     * @param ids the array of imagery ids
     * @param true if this is for overlay selection
     * @param update if true this is an update of an existing adapter
     */
    void setIds(@NonNull String[] ids, boolean isOverlay, boolean update) {
        this.ids = ids;
        names = isOverlay ? TileLayerSource.getOverlayNames(ids) : TileLayerSource.getNames(ids);
        if (update) {
            notifyDataSetChanged();
        }
    }

    /**
     * Set a new OnCheckedChangeListener
     * 
     * @param groupChangeListener the listener
     */
    void setOnCheckedChangeListener(@NonNull android.widget.RadioGroup.OnCheckedChangeListener groupChangeListener) {
        this.groupChangeListener = groupChangeListener;
    }
}
