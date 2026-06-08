package de.blau.android.prefs.search;

import java.util.Optional;

import de.KnollFrank.lib.settingssearch.db.preference.db.PreferencesDatabaseConfig;

public class PreferencesDatabaseConfigFactory {

	private PreferencesDatabaseConfigFactory() {
	}

	public static PreferencesDatabaseConfig<Configuration> createPreferencesDatabaseConfig() {
		return new PreferencesDatabaseConfig<>(
				"searchable_preferences.db",
				Optional.empty(),
				PreferencesDatabaseConfig.JournalMode.TRUNCATE);
	}
}
