package de.blau.android.easyedit;

import java.util.List;

import android.content.Context;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.CompoundButton.OnCheckedChangeListener;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatRadioButton;
import androidx.recyclerview.widget.RecyclerView;
import de.blau.android.App;
import de.blau.android.osm.Relation;
import de.blau.android.osm.StorageDelegator;

public class RelationListAdapter extends RecyclerView.Adapter<RelationListAdapter.RadioButtonViewHolder> {
    private String[]                                          descriptions;
    private List<Long>                                        ids;
    private long                                              currentId;
    private final LayoutParams                                buttonLayoutParams;
    private android.widget.RadioGroup.OnCheckedChangeListener groupChangeListener = null;

    private int selected = -1;

    public static class RadioButtonViewHolder extends RecyclerView.ViewHolder {
        AppCompatRadioButton button;

        /**
         * Create a new ViewHolder
         * 
         * @param v the RadioButton that will be displayed
         */
        public RadioButtonViewHolder(@NonNull AppCompatRadioButton v) {
            super(v);
            button = v;
        }
    }

    /**
     * Create a new adapter
     * 
     * @param context an Android Context
     * @param ids a list of relation ids
     * @param currentId the current id
     * @param buttonLayoutParams layout params for the RadioButtons
     * @param groupChangeListener a listener to call when a RadioButton has been selected
     */
    public RelationListAdapter(@NonNull Context context, @NonNull List<Long> ids, long currentId, @NonNull LayoutParams buttonLayoutParams,
            @NonNull android.widget.RadioGroup.OnCheckedChangeListener groupChangeListener) {
        setIds(context, ids, false);
        this.currentId = currentId;
        this.buttonLayoutParams = buttonLayoutParams;
        this.groupChangeListener = groupChangeListener;
    }

    final OnCheckedChangeListener onCheckedChangeListener = (buttonView, isChecked) -> {
        Integer position = (Integer) buttonView.getTag();
        if (position != null) {
            RelationListAdapter.this.notifyItemChanged(selected);
            currentId = ids.get(position);
            selected = position;
            groupChangeListener.onCheckedChanged(null, position);
        }
    };

    @Override
    public RelationListAdapter.RadioButtonViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final AppCompatRadioButton button = new AppCompatRadioButton(parent.getContext());
        button.setLayoutParams(buttonLayoutParams);
        return new RadioButtonViewHolder(button);
    }

    @Override
    public void onBindViewHolder(RadioButtonViewHolder holder, int position) {
        holder.button.setText(descriptions[position]);
        holder.button.setTag(position);
        holder.button.setOnCheckedChangeListener(null);
        if (ids.get(position) == currentId) {
            holder.button.setChecked(true);
            selected = position;
        } else {
            holder.button.setChecked(false);
        }
        holder.button.setOnCheckedChangeListener(onCheckedChangeListener);
    }

    @Override
    public int getItemCount() {
        return descriptions.length;
    }

    /**
     * Set the ids and name arrays that are going to be display
     * 
     * @param ids the array of ids
     * @param update if true this is an update of an existing adapter
     */
    void setIds(@NonNull Context context, @NonNull List<Long> ids, boolean update) {
        this.ids = ids;
        descriptions = new String[ids.size()];
        StorageDelegator delegator = App.getDelegator();
        for (int i = 0; i < ids.size(); i++) {
            Relation r = (Relation) delegator.getOsmElement(Relation.NAME, ids.get(i));
            descriptions[i] = r != null ? r.getDescription(context) : "relation at pos " + i + " missing";
        }
        if (update) {
            notifyDataSetChanged();
        }
    }
}
