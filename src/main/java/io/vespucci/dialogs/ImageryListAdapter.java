package io.vespucci.dialogs;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageButton;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatRadioButton;
import androidx.recyclerview.widget.RecyclerView;
import io.vespucci.R;
import io.vespucci.resources.TileLayerSource;

public class ImageryListAdapter extends RecyclerView.Adapter<ImageryListAdapter.ImageryViewHolder> {
    private String[]                                          names;
    private String[]                                          ids;
    private String                                            currentId;
    private final LayoutParams                                buttonLayoutParams;
    private android.widget.RadioGroup.OnCheckedChangeListener groupChangeListener = null;
    private OnInfoClickListener                               infoClickListener   = null;

    public static class ImageryViewHolder extends RecyclerView.ViewHolder {
        AppCompatRadioButton button;
        ImageButton          infoButton;

        /**
         * Create a new ViewHolder
         *
         * @param v the RadioButton that will be displayed
         */
        public ImageryViewHolder(@NonNull View v) {
            super(v);
            button = v.findViewById(R.id.listItemRadioButton);
            infoButton = v.findViewById(R.id.listItemInfo);
        }
    }

    /**
     * Create a new adapter
     * 
     * @param ctx an Android Context
     * @param ids an array with imagery ids
     * @param currentId the current imagery id
     * @param isOverlay true if overlay should be displayed
     * @param buttonLayoutParams layout params for the RadioButtons
     * @param groupChangeListener a listener to call when a RadioButton has been selected
     */
    public ImageryListAdapter(@NonNull Context ctx, @NonNull String[] ids, String currentId, @Nullable boolean isOverlay,
            @NonNull LayoutParams buttonLayoutParams, @NonNull android.widget.RadioGroup.OnCheckedChangeListener groupChangeListener) {
        setIds(ctx, ids, isOverlay, false);
        this.currentId = currentId;
        this.buttonLayoutParams = buttonLayoutParams;
        this.groupChangeListener = groupChangeListener;
    }

    final OnCheckedChangeListener onCheckedChangeListener = (buttonView, isChecked) -> {
        ImageryViewHolder holder = (ImageryViewHolder) buttonView.getTag();
        if (holder != null) {
            int position = holder.getAdapterPosition();
            currentId = ids[position];
            groupChangeListener.onCheckedChanged(null, position);
        }
    };

    interface OnInfoClickListener {

        /**
         * Implements info icon click logic
         *
         * @param id the selected layer id
         */
        void onInfoClick(@NonNull String id);
    }

    /**
     * Set the listener for the info icon
     *
     * @param listener on click behaviour
     */
    public void addInfoClickListener(@NonNull OnInfoClickListener listener) {
        infoClickListener = listener;
    }

    @Override
    public ImageryListAdapter.ImageryViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final AppCompatRadioButton button = new AppCompatRadioButton(parent.getContext());
        button.setLayoutParams(buttonLayoutParams);
        View listItem = LayoutInflater.from(parent.getContext()).inflate(R.layout.imagery_layer_list_item, parent, false);
        return new ImageryViewHolder(listItem);
    }

    @Override
    public void onBindViewHolder(ImageryViewHolder holder, int position) {
        holder.button.setText(names[position]);
        holder.button.setTag(holder);
        holder.button.setOnCheckedChangeListener(null);
        holder.button.setChecked(ids[position].equals(currentId));
        holder.button.setOnCheckedChangeListener(onCheckedChangeListener);
        holder.infoButton.setOnClickListener(view -> {
            if (infoClickListener != null) {
                infoClickListener.onInfoClick(ids[position]);
            }
        });
    }

    @Override
    public int getItemCount() {
        return names.length;
    }

    /**
     * Set the ids and name arrays that are going to be display
     * 
     * @param ctx an Android context
     * @param ids the array of imagery ids
     * @param isOverlay true if this is for overlay selection
     * @param update if true this is an update of an existing adapter
     */
    void setIds(@NonNull Context ctx, @NonNull String[] ids, boolean isOverlay, boolean update) {
        this.ids = ids;
        names = isOverlay ? TileLayerSource.getOverlayNames(ctx, ids) : TileLayerSource.getNames(ctx, ids);
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