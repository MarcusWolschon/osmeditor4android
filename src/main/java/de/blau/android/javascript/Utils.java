package de.blau.android.javascript;

import java.util.ArrayList;
import java.util.Map;

import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

import com.faendir.rhino_android.RhinoAndroidHelper;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Handler;
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
import de.blau.android.Logic;
import de.blau.android.R;
import de.blau.android.dialogs.Progress;
import de.blau.android.dialogs.ProgressDialog;
import de.blau.android.osm.StorageDelegator;
import de.blau.android.util.ThemeUtils;

/**
 * Various JS related utility methods
 * 
 * @see <a href="https://developer.mozilla.org/en-US/docs/Mozilla/Projects/Rhino/Scopes_and_Contexts">https://developer.mozilla.org/en-US/docs/Mozilla/Projects/Rhino/Scopes_and_Contexts</a>
 * @see <a href="https://dxr.mozilla.org/mozilla/source/js/rhino/examples/DynamicScopes.java">https://dxr.mozilla.org/mozilla/source/js/rhino/examples/DynamicScopes.java</a>
 * @author simon
 *
 */
public class Utils {
	
	private static final String DEBUG_TAG = "javascript.Utils";

	/**
	 * Evaluate JS
	 * @param ctx android context
	 * @param scriptName name for error reporting
	 * @param script the javascript
	 * @return whatever the JS returned as a string
	 */
	@Nullable
	public static String evalString(Context ctx, String scriptName, String script) {
		Log.d(DEBUG_TAG, "Eval " + script);
		Scriptable restrictedScope = App.getRestrictedRhinoScope(ctx);
		Scriptable scope = App.getRhinoContext(ctx).newObject(restrictedScope);
        scope.setPrototype(restrictedScope);
        scope.setParentScope(null);
		Object result = App.getRhinoContext(ctx).evaluateString(scope, script, scriptName, 1, null);
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
		RhinoAndroidHelper rhinoAndroidHelper = new RhinoAndroidHelper(ctx);	
		org.mozilla.javascript.Context rhinoContext = rhinoAndroidHelper.enterContext(); 
		try {
			Scriptable restrictedScope = App.getRestrictedRhinoScope(ctx);
			Scriptable scope = rhinoContext.newObject(restrictedScope);
			scope.setPrototype(restrictedScope);
			scope.setParentScope(null);
			Object wrappedOut = org.mozilla.javascript.Context.javaToJS(tags, scope);
			ScriptableObject.putProperty(scope, "tags", wrappedOut);
			wrappedOut = org.mozilla.javascript.Context.javaToJS(value, scope);
			ScriptableObject.putProperty(scope, "value", wrappedOut);
			Log.d(DEBUG_TAG, "Eval (preset): " + script);
			Object result = App.getRhinoContext(ctx).evaluateString(scope, script, scriptName, 1, null);
			if (result==null) {
				return null;
			} else {
				return org.mozilla.javascript.Context.toString(result);
			}
		} finally {
			org.mozilla.javascript.Context.exit();
		}
	}
	
	/**
	 * Evaluate a script making the current logic available, this essentially allows access to all data
	 * @param ctx android context
	 * @param scriptName name for error reporting
	 * @param script the javascript
	 * @param logic an instance of Logic
	 * @return
	 */
	@Nullable
	public static String evalString(Context ctx, String scriptName, String script, Logic logic) {
		RhinoAndroidHelper rhinoAndroidHelper = new RhinoAndroidHelper(ctx);	
		org.mozilla.javascript.Context rhinoContext = rhinoAndroidHelper.enterContext(); 
		try {
			Scriptable restrictedScope = App.getRestrictedRhinoScope(ctx);
			Scriptable scope = rhinoContext.newObject(restrictedScope);
			scope.setPrototype(restrictedScope);
			scope.setParentScope(null);
			Object wrappedOut = org.mozilla.javascript.Context.javaToJS(logic, scope);
			ScriptableObject.putProperty(scope, "logic", wrappedOut);
			Log.d(DEBUG_TAG, "Eval (logic): " + script);
			Object result = App.getRhinoContext(ctx).evaluateString(scope, script, scriptName, 1, null);
			if (result==null) {
				return null;
			} else {
				return org.mozilla.javascript.Context.toString(result);
			}
		} finally {
			org.mozilla.javascript.Context.exit();
		}
	}
	
	/**
	 * Display a simple console with multi-line input and output from the eval method
	 * @param ctx android context
	 * @param callback callback that actually evaluates the input
	 */
	@SuppressLint("InflateParams")
	public static void jsConsoleDialog(final Activity ctx, int msgResource, final EvalCallback callback) {
		// Create some useful objects
		final LayoutInflater inflater = ThemeUtils.getLayoutInflater(ctx);

		Builder builder = new AlertDialog.Builder(ctx);
		builder.setTitle(R.string.tag_menu_js_console);
		builder.setMessage(msgResource);
		View v = inflater.inflate(R.layout.debug_js, null);	
		final EditText input = (EditText)v.findViewById(R.id.js_input);
		final TextView output = (TextView)v.findViewById(R.id.js_output);
		builder.setView(v);

		builder.setPositiveButton(R.string.evaluate, null);
		builder.setNegativeButton(R.string.dismiss, null);
		builder.setNeutralButton("Share", null);
		AlertDialog dialog = builder.create();
		final Handler handler = new Handler();
		dialog.setOnShowListener(new DialogInterface.OnShowListener() {
			@Override
			public void onShow(DialogInterface dialog) {
				Button positive = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE);
				positive.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						final AsyncTask<String, Void, String> runner = new AsyncTask<String, Void, String>() {
							final AlertDialog progress = ProgressDialog.get(ctx, Progress.PROGRESS_DOWNLOAD);
							@Override
							protected void onPreExecute() {
								progress.show();
							}
							@Override
							protected String doInBackground(String... js) {
								try {
									return callback.eval(js[0]);
								} catch (Exception ex) {
									Log.e(DEBUG_TAG, "dialog failed with " + ex);
									ex.printStackTrace();
									return ex.getMessage();
								}
							}
							@Override
							protected void onPostExecute(final String result) {
								try {
									progress.dismiss();
								} catch (Exception ex) {
									Log.e(DEBUG_TAG, "dismiss dialog failed with " + ex);
								}
								output.setText(result);
							}
						};
						runner.execute(input.getText().toString());
					}
				});
				Button neutral = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_NEUTRAL);
				Drawable share = ThemeUtils.getTintedDrawable(ctx, R.drawable.ic_share_black_36dp, R.attr.colorAccent);
				neutral.setCompoundDrawablesWithIntrinsicBounds(share, null, null, null);
				neutral.setText("");
				neutral.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						Intent sendIntent = new Intent();
						sendIntent.setAction(Intent.ACTION_SEND);
						sendIntent.putExtra(Intent.EXTRA_TEXT,input.getText().toString());
						sendIntent.setType("text/plain");
						ctx.startActivity(sendIntent);
					}
				});
			}
		});
		dialog.show();
	}
}
