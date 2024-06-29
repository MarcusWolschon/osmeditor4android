package de.blau.android.propertyeditor;

import java.util.List;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import de.blau.android.App;
import de.blau.android.R;
import de.blau.android.osm.OsmElement;
import de.blau.android.propertyeditor.RelationMembersFragment.Connected;
import de.blau.android.propertyeditor.RelationMembersFragment.MemberEntry;
import de.blau.android.propertyeditor.RelationMembersFragment.RelationMemberRow;

public class RelationMemberAdapter extends RecyclerView.Adapter<RelationMemberAdapter.MemberRowViewHolder> {

    private android.widget.RadioGroup.OnCheckedChangeListener groupChangeListener = null;

    private final LayoutInflater inflater;
    private final Context        ctx;
    private final TextWatcher    watcher;

    private List<MemberEntry> entries;

    private int selected = -1;

    private OnCheckedChangeListener       listener;
    private final RelationMembersFragment owner;

    public static class MemberRowViewHolder extends RecyclerView.ViewHolder {
        RelationMemberRow row;

        /**
         * Create a new ViewHolder
         * 
         * @param v the RelationMemberRow that will be displayed
         */
        public MemberRowViewHolder(@NonNull RelationMemberRow v) {
            super(v);
            row = v;
        }
    }

    /**
     * Create a new adapter
     * 
     * @param ctx an Android Context
     * @param owner the Fragment this is being used in
     * @param inflater a LayoutInflater
     * @param entries a List of MemberEntry
     * @param listener an OnCheckedChangeListener
     * @param maxStringLength the maximum string length to support
     */
    public RelationMemberAdapter(@NonNull Context ctx, @NonNull RelationMembersFragment owner, @NonNull LayoutInflater inflater,
            @NonNull List<MemberEntry> entries, @NonNull OnCheckedChangeListener listener, int maxStringLength) {
        this.ctx = ctx;
        this.owner = owner;
        this.inflater = inflater;
        this.entries = entries;
        this.listener = listener;
        this.watcher = new SanitizeTextWatcher(ctx, maxStringLength);
    }

    final OnCheckedChangeListener onCheckedChangeListener = (buttonView, isChecked) -> {
        Integer position = (Integer) buttonView.getTag();
        if (position != null) {
            RelationMemberAdapter.this.notifyItemChanged(selected);
            selected = position;
            groupChangeListener.onCheckedChanged(null, position);
        }
    };

    @Override
    public RelationMemberAdapter.MemberRowViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final RelationMemberRow row = (RelationMemberRow) inflater.inflate(viewType, parent, false);
        row.setOwner(owner);
        return new MemberRowViewHolder(row);
    }

    @Override
    public void onBindViewHolder(MemberRowViewHolder holder, int position) {
        final MemberEntry memberEntry = entries.get(position);
        holder.row.setValues(memberEntry);
        Connected connected = memberEntry.connected;
        if (connected != null) {
            holder.row.setIcon(ctx, memberEntry, connected);
        }
        holder.row.setOnCheckedChangeListener(null);
        if (memberEntry.selected) {
            holder.row.select();
        } else {
            holder.row.deselect();
        }
        OnCheckedChangeListener realListener = (CompoundButton buttonView, boolean isChecked) -> {
            memberEntry.selected = isChecked;
            listener.onCheckedChanged(buttonView, isChecked);
        };
        holder.row.setOnCheckedChangeListener(realListener);

        holder.row.setRoleWatcher(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // do nothing
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // do nothing
            }

            @Override
            public void afterTextChanged(Editable s) {
                watcher.afterTextChanged(s);
                memberEntry.setRole(s.toString());
            }
        });

        if (memberEntry.downloaded() && !memberEntry.selected) {
            holder.row.elementView.setOnClickListener((View v) -> {
                OsmElement element = App.getDelegator().getOsmElement(memberEntry.getType(), memberEntry.getRef());
                if (element != null) {
                    ((ControlListener) owner.getActivity()).addPropertyEditor(element);
                }
            });
        } else {
            holder.row.elementView.setOnClickListener(null);
        }
    }
    
    @Override
    public int getItemViewType(int position) {
        return entries.get(position).downloaded() ? R.layout.relation_member_downloaded_row : R.layout.relation_member_row;
    }

    @Override
    public int getItemCount() {
        return entries.size();
    }
}
