package de.blau.android.javascript;

import android.content.Context;
import android.util.Log;
import de.blau.android.App;

public class Utils {
	
	public static String evalString(Context ctx, String scriptName, String script) {
		Log.d("javascript.Utils", "Eval " + script);
		try {
			Object result = App.getRhinoContext(ctx).evaluateString(App.getRhinoScope(ctx), script, scriptName, 1, null);
			return org.mozilla.javascript.Context.toString(result);
		} catch (Exception ex) {
			return ex.getMessage(); // this is not a good idea
		}
	}
}
