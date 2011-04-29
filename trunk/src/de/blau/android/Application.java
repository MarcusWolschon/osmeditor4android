package de.blau.android;

public class Application extends android.app.Application {
	public static Main mainActivity;
	public static String userAgent;
	
	@Override
	public void onCreate() {
		super.onCreate();
		String appName = getResources().getString(R.string.app_name);
		String appVersion = getResources().getString(R.string.app_version);
		userAgent = appName + "/" + appVersion;
	}
}
