package de.blau.android.prefs;

import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;
import de.blau.android.R;
import de.blau.android.prefs.AdvancedPrefDatabase.API;

/** A dialog preference that allows the user to set username and password in one dialog.
 * Changes get saved to the {@link AdvancedPrefDatabase} */
public class LoginDataPreference extends DialogPreference {

	private final Context context;
	private EditText userEdit;
	private EditText passwordEdit;
	
	public LoginDataPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.context = context;
		init();
	}

	public LoginDataPreference(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		this.context = context;
		init();
	}
	
	private void init() {
		super.setPersistent(false);
		super.setDialogLayoutResource(R.layout.login_edit);
	}

	
	@Override
	protected void onBindDialogView(View view) {
		super.onBindDialogView(view);
		userEdit = (EditText)view.findViewById(R.id.loginedit_editUsername);
		passwordEdit = (EditText)view.findViewById(R.id.loginedit_editPassword);
		AdvancedPrefDatabase db = new AdvancedPrefDatabase(context);
		API api = db.getCurrentAPI();
		userEdit.setText(api.user);
		passwordEdit.setText(api.pass);
	}


	@Override
	protected void onDialogClosed(boolean positiveResult) {
		if (positiveResult) {
			AdvancedPrefDatabase db = new AdvancedPrefDatabase(context);
			db.setCurrentAPILogin(userEdit.getText().toString(), passwordEdit.getText().toString());
		}
		super.onDialogClosed(positiveResult);
	}

}
