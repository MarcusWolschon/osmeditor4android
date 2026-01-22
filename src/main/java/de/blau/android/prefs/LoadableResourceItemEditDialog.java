package de.blau.android.prefs;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.net.MalformedURLException;
import java.net.URL;

import android.app.Dialog;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDialog;
import androidx.fragment.app.FragmentActivity;
import de.blau.android.R;
import de.blau.android.contract.Schemes;
import de.blau.android.prefs.URLListEditActivity.ListEditItem;
import de.blau.android.util.CancelableDialogFragment;
import de.blau.android.util.FragmentUtil;
import de.blau.android.util.ReadFile;
import de.blau.android.util.ScreenMessage;
import de.blau.android.util.SelectFile;
import de.blau.android.util.ThemeUtils;
import de.blau.android.util.Util;

/**
 * ListItemEdit dialog for an item which can be loaded locally or downloaded
 */
public abstract class LoadableResourceItemEditDialog extends CancelableDialogFragment {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, LoadableResourceItemEditDialog.class.getSimpleName().length());
    private static final String DEBUG_TAG = LoadableResourceItemEditDialog.class.getSimpleName().substring(0, TAG_LEN);

    static final String ITEM_EDIT_DIALOG_TAG = "item_edit_dialog";
    static final String ITEM_KEY             = "item";

    private static final String LOCALHOST = "localhost";

    private final int layoutRes;

    LoadableResourceItemEditDialog(int layoutRes) {
        this.layoutRes = layoutRes;
    }

    @Override
    public AppCompatDialog onCreateDialog(Bundle savedInstanceState) {
        ListEditItem item = Util.getSerializeable(getArguments(), ITEM_KEY, ListEditItem.class);
        final AlertDialog.Builder builder = ThemeUtils.getAlertDialogBuilder(getContext());
        final LayoutInflater inflater = ThemeUtils.getLayoutInflater(getContext());
        final View mainView = inflater.inflate(layoutRes, null);
        final TextView editName = (TextView) mainView.findViewById(R.id.listedit_editName);
        final TextView editValue = (TextView) mainView.findViewById(R.id.listedit_editValue);
        final TextView versionLabel = (TextView) mainView.findViewById(R.id.listedit_labelVersion);
        final TextView version = (TextView) mainView.findViewById(R.id.listedit_version);
        final ImageButton fileButton = (ImageButton) mainView.findViewById(R.id.listedit_file_button);

        final URLListEditActivity activity = (URLListEditActivity) getActivity();

        final boolean itemExists = item != null;
        if (itemExists) {
            editName.setText(item.name);
            editValue.setText(item.value);
        } else if (activity.isAddingViaIntent()) {
            String tmpName = activity.getIntent().getExtras().getString(URLListEditActivity.EXTRA_NAME);
            String tmpValue = activity.getIntent().getExtras().getString(URLListEditActivity.EXTRA_VALUE);
            editName.setText(tmpName == null ? "" : tmpName);
            editValue.setText(tmpValue == null ? "" : tmpValue);
        }
        aditionalFieldsSetup(activity, mainView, itemExists);

        if (itemExists && item.value3 != null) {
            version.setText(item.value3);
        } else {
            versionLabel.setVisibility(View.GONE);
            version.setVisibility(View.GONE);
        }
        if (itemExists && isDefault(item)) {
            // name and value are not editable
            editName.setInputType(InputType.TYPE_NULL);
            editName.setBackground(null);
            editValue.setEnabled(false);
            fileButton.setEnabled(false);
        }

        activity.setViewAndButtons(builder, mainView);

        final AlertDialog dialog = builder.create();

        fileButton.setOnClickListener(v -> SelectFile.read(activity, R.string.config_presetsPreferredDir_key, new ReadFile() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean read(FragmentActivity currentActivity, Uri fileUri) {
                final Dialog dialog = FragmentUtil.findDialogByTag(currentActivity, ITEM_EDIT_DIALOG_TAG);
                if (dialog == null) {
                    Log.e(DEBUG_TAG, "Dialog is null");
                    return false;
                }
                final TextView editValue = (TextView) dialog.findViewById(R.id.listedit_editValue);

                editValue.setText(fileUri.toString());
                SelectFile.savePref(new Preferences(currentActivity), R.string.config_presetsPreferredDir_key, fileUri);
                return true;
            }
        }));

        // overriding the handlers
        dialog.setOnShowListener((DialogInterface d) -> {
            dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(v -> {
                String name = editName.getText().toString().trim();
                String itemURL = editValue.getText().toString().trim();
                URLListEditActivity.changeBackgroundColor(editValue, URLListEditActivity.VALID_COLOR);
                // validate entries
                boolean validURL = Patterns.WEB_URL.matcher(itemURL).matches();
                URL url = null;
                try {
                    url = new URL(itemURL);
                } catch (MalformedURLException e) {
                    validURL = false;
                }

                // save or display toast, exception for localhost is needed for testing
                if (validURL || itemURL.startsWith(Schemes.FILE) || itemURL.startsWith(Schemes.CONTENT) || (url != null && LOCALHOST.equals(url.getHost()))
                        || (itemExists && item.id.equals(URLListEditActivity.LISTITEM_ID_DEFAULT))) {
                    finishItem(activity, item, name, itemURL);
                    dialog.dismiss();
                } else {
                    // if garbage value entered show toasts
                    ScreenMessage.barError(activity, R.string.toast_invalid_url);
                    URLListEditActivity.changeBackgroundColor(editValue, URLListEditActivity.ERROR_COLOR);
                }
            });
            dialog.getButton(DialogInterface.BUTTON_NEGATIVE).setOnClickListener(v -> dialog.dismiss());
        });
        return dialog;
    }

    /**
     * Check if this is a default item
     * 
     * @param item the Item
     * @return true is a default
     */
    abstract boolean isDefault(ListEditItem item);

    /**
     * Setup any non base fields
     * 
     * @param activity the activity
     * @param layout the layout
     * @param itemExists if this is a new item or an existing one
     */
    protected void aditionalFieldsSetup(@NonNull FragmentActivity activity, @NonNull View layout, boolean itemExists) {
        // nothing
    }

    /**
     * Finish adding or updating the item
     * 
     * @param activity the activity
     * @param item the Item
     * @param name the name
     * @param url the url
     */
    abstract void finishItem(@NonNull URLListEditActivity activity, @Nullable ListEditItem item, @NonNull String name, @NonNull String url);
}