package de.blau.android.prefs.keyboard;

import java.util.Arrays;
import java.util.List;

import android.content.Context;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import de.blau.android.R;
import de.blau.android.prefs.keyboard.Shortcuts.Modifier;
import de.blau.android.prefs.keyboard.Shortcuts.Shortcut;

/**
 * RecyclerView adapter for displaying and editing keyboard shortcuts
 */
public class ShortcutsAdapter extends RecyclerView.Adapter<ShortcutsAdapter.ViewHolder> {

    /**
     * Interface for listening to shortcut changes
     */
    public interface OnShortcutChangedListener {
        /**
         * Called when a shortcut is changed
         * 
         * @param item the modified shortcut item
         */
        void onShortcutChanged(@NonNull Shortcut item);
    }

    private List<Shortcut>            shortcuts;
    private Context                   context;
    private OnShortcutChangedListener listener;

    private List<String> actionRefs;
    private List<String> actionDescriptions;

    public ShortcutsAdapter(@NonNull Context context, @NonNull List<Shortcut> shortcuts, @NonNull OnShortcutChangedListener listener) {
        this.context = context;
        this.shortcuts = shortcuts;
        this.listener = listener;

        Resources res = context.getResources();
        actionRefs = Arrays.asList(res.getStringArray(R.array.shortcut_actions_keys));
        actionDescriptions = Arrays.asList(res.getStringArray(R.array.shortcut_actions));
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.keyboard_shortcut_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Shortcut item = shortcuts.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return shortcuts.size();
    }

    /**
     * ViewHolder for keyboard shortcut items
     */
    public class ViewHolder extends RecyclerView.ViewHolder {

        private TextView    actionDescription;
        private Spinner     modifierSpinner;
        private EditText    keyCharacterEdit;
        private ImageButton saveButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            actionDescription = itemView.findViewById(R.id.action_description);
            modifierSpinner = itemView.findViewById(R.id.modifier_spinner);
            keyCharacterEdit = itemView.findViewById(R.id.key_character_edit);
            saveButton = itemView.findViewById(R.id.save_button);

        }

        public void bind(@NonNull Shortcut item) {
            actionDescription.setText(actionDescriptions.get(actionRefs.indexOf(item.getActionRef())));
            keyCharacterEdit.setText(String.valueOf(item.getCharacter()));

            // Set up modifier spinner
            ModifierSpinnerAdapter spinnerAdapter = new ModifierSpinnerAdapter(context,
                    new String[] { Modifier.NONE.toString(), Modifier.ALT.toString(), Modifier.CTRL.toString(), Modifier.META.toString() });
            modifierSpinner.setAdapter(spinnerAdapter);
            modifierSpinner.setSelection(item.getModifier().ordinal());

            saveButton.setOnClickListener(v -> {
                Modifier selectedModifier = Modifier.valueOf((String) modifierSpinner.getSelectedItem());
                String keyChar = keyCharacterEdit.getText().toString();

                if (keyChar.length() != 1) {
                    keyCharacterEdit.setError(context.getString(R.string.keyboard_shortcut_key_required));
                    return;
                }
                char character = keyChar.toLowerCase().charAt(0);
                for (Shortcut s : shortcuts) {
                    if (s.getCharacter() == character && s.getModifier() == selectedModifier && !s.getActionRef().equals(item.getActionRef())) {
                        keyCharacterEdit.setError(context.getString(R.string.keyboard_shortcut_duplicate));
                        return;
                    }
                }

                item.setModifier(selectedModifier);
                item.setCharacter(character);
                listener.onShortcutChanged(item);
            });
        }
    }

    /**
     * Custom spinner adapter for modifiers
     */
    private static class ModifierSpinnerAdapter extends android.widget.ArrayAdapter<String> {
        public ModifierSpinnerAdapter(@NonNull Context context, @NonNull String[] objects) {
            super(context, android.R.layout.simple_spinner_item, objects);
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        }
    }
}