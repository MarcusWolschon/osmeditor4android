package de.blau.android.listener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.DialogInterface;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;
import de.blau.android.App;
import de.blau.android.ErrorCodes;
import de.blau.android.Logic;
import de.blau.android.R;
import de.blau.android.dialogs.ConfirmUpload;
import de.blau.android.dialogs.ErrorAlert;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Server;
import de.blau.android.osm.Tags;
import de.blau.android.tasks.TransferTasks;
import de.blau.android.util.Snack;
import de.blau.android.validation.FormValidation;

/**
 * @author mb
 * @author Simon Poole
 */
public class UploadListener implements DialogInterface.OnShowListener, View.OnClickListener {

    private final FragmentActivity     caller;
    private final EditText             commentField;
    private final EditText             sourceField;
    private final CheckBox             closeOpenChangeset;
    private final CheckBox             closeChangeset;
    private final CheckBox             requestReview;
    private final List<FormValidation> validations;
    private final List<OsmElement>     elements;

    private boolean tagsShown = false;

    /**
     * Upload the current changes
     * 
     * @param caller the instance of Main calling this
     * @param commentField an EditText containing the comment tag value
     * @param sourceField an EditText containing the source tag value
     * @param closeOpenChangeset close any open changeset first if true
     * @param closeChangeset close the changeset after upload if true
     * @param requestReview CheckBox for the review_requested tag
     * @param validations a List of validations to perform on the form fields
     * @param elements List of OsmELement to upload if null all changed elements will be uploaded
     */
    public UploadListener(@NonNull final FragmentActivity caller, @NonNull final EditText commentField, @NonNull final EditText sourceField,
            @Nullable CheckBox closeOpenChangeset, @NonNull final CheckBox closeChangeset, @NonNull CheckBox requestReview,
            @NonNull final List<FormValidation> validations, List<OsmElement> elements) {
        this.caller = caller;
        this.commentField = commentField;
        this.sourceField = sourceField;
        this.closeOpenChangeset = closeOpenChangeset;
        this.closeChangeset = closeChangeset;
        this.requestReview = requestReview;
        this.validations = validations;
        this.elements = elements;
    }

    @Override
    public void onShow(final DialogInterface dialog) {
        Button button = ((AlertDialog) dialog).getButton(DialogInterface.BUTTON_POSITIVE);
        button.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        validateFields();
        if (tagsShown || ConfirmUpload.getPage(caller) == ConfirmUpload.TAGS_PAGE) {
            ConfirmUpload.dismissDialog(caller);
            Map<String, String> extraTags = new HashMap<>();
            if (requestReview.isChecked()) {
                extraTags.put(Tags.KEY_REVIEW_REQUESTED, Tags.VALUE_YES);
            }
            final Logic logic = App.getLogic();
            final Server server = logic.getPrefs().getServer();
            if (server.isLoginSet()) {
                boolean hasDataChanges = logic.hasChanges();
                boolean hasBugChanges = !App.getTaskStorage().isEmpty() && App.getTaskStorage().hasChanges();
                if (hasDataChanges || hasBugChanges) {
                    if (hasDataChanges) {
                        logic.upload(caller, getString(commentField), getString(sourceField), closeOpenChangeset != null && closeOpenChangeset.isChecked(),
                                closeChangeset.isChecked(), extraTags, elements);
                    }
                    if (hasBugChanges) {
                        TransferTasks.upload(caller, server, null);
                    }
                    logic.checkForMail(caller, server);
                } else {
                    Snack.barInfo(caller, R.string.toast_no_changes);
                }
            } else {
                ErrorAlert.showDialog(caller, ErrorCodes.NO_LOGIN_DATA);
            }
        } else {
            ConfirmUpload.showPage(caller, ConfirmUpload.TAGS_PAGE);
            tagsShown = true;
        }
    }

    /**
     * Get the trimmed contents from an EditText
     * 
     * @param editText the EditText
     * @return a String with the EditText contents
     */
    String getString(@NonNull EditText editText) {
        return editText.getText().toString().trim();
    }

    /**
     * Run all supplied validators
     */
    private void validateFields() {
        for (FormValidation validation : validations) {
            validation.validate();
        }
    }
}
