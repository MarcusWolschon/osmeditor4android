package de.blau.android.javascript;

import java.util.ArrayList;
import java.util.Map;

import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AlertDialog.Builder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import de.blau.android.App;
import de.blau.android.R;
import de.blau.android.util.ThemeUtils;

/**
 * Various JS related utility methods
 * @author simon
 *
 */
public class Utils {
	
	/**
	 * Evaluate JS
	 * @param ctx android context
	 * @param scriptName name for error reporting
	 * @param script the javascript
	 * @return whatever the JS returned as a string
	 */
	@Nullable
	public static String evalString(Context ctx, String scriptName, String script) {
		Log.d("javascript.Utils", "Eval " + script);
		Object result = App.getRhinoContext(ctx).evaluateString(App.getRhinoScope(ctx), script, scriptName, 1, null);
		return org.mozilla.javascript.Context.toString(result);
	}
	
	/**
	 * Evaluate JS associated with a key in a preset
	 * @param ctx android context
	 * @param scriptName name for error reporting
	 * @param script the javascript
	 * @param tags the tags of the 
	 * @param value any value associated with the key
	 * @return the value that should be assigned to the tag or null if no value should be set
	 */
	@Nullable
	public static String evalString(Context ctx, String scriptName, String script, Map<String, ArrayList<String>> tags, String value) {
		Scriptable scope = App.getRhinoScope(ctx);
		Object wrappedOut = org.mozilla.javascript.Context.javaToJS(tags, scope);
		ScriptableObject.putProperty(scope, "tags", wrappedOut);
		wrappedOut = org.mozilla.javascript.Context.javaToJS(value, scope);
		ScriptableObject.putProperty(scope, "value", wrappedOut);
		Log.d("javascript.Utils", "Eval " + script);
		Object result = App.getRhinoContext(ctx).evaluateString(App.getRhinoScope(ctx), script, scriptName, 1, null);
		if (result==null) {
			return null;
		} else {
			return org.mozilla.javascript.Context.toString(result);
		}
	}
	
	/**
	 * Display a simple console with multi-line input and output from the eval method
	 * @param ctx android context
	 * @param callback callback that actually evaluates the input
	 */
	@SuppressLint("InflateParams")
	public static void jsConsoleDialog(Context ctx, final EvalCallback callback) {
		// Create some useful objects
		final LayoutInflater inflater = ThemeUtils.getLayoutInflater(ctx);

		Builder builder = new AlertDialog.Builder(ctx);
		builder.setTitle(R.string.tag_menu_js_console);
		View v = inflater.inflate(R.layout.debug_js, null);	
		final EditText input = (EditText)v.findViewById(R.id.js_input);
		final TextView output = (TextView)v.findViewById(R.id.js_output);
		builder.setView(v);

		builder.setPositiveButton(R.string.evaluate, null);
		builder.setNegativeButton(R.string.dismiss, null);
		AlertDialog dialog = builder.create();
		dialog.setOnShowListener(new DialogInterface.OnShowListener() {

			@Override
			public void onShow(DialogInterface dialog) {

				Button button = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE);
				button.setOnClickListener(new View.OnClickListener() {

					@Override
					public void onClick(View view) {
						try {
							output.setText(callback.eval(input.getText().toString()));
						} catch (Exception ex) {
							output.setText(ex.getMessage());
						}
					}
				});
			}
		});
		dialog.show();
	}
}
