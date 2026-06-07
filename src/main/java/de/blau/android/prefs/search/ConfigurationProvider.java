package de.blau.android.prefs.search;

public class ConfigurationProvider {

	private ConfigurationProvider() {
	}

	public static Configuration getActualConfiguration() {
		return new Configuration();
	}
}
