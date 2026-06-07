package de.blau.android.prefs.search;

import java.util.Optional;

import de.KnollFrank.lib.settingssearch.db.preference.db.PreferencesDatabaseConfig;

public class PreferencesDatabaseConfigFactory {

	private static final String SEARCHABLE_PREFERENCES_DB = "searchable_preferences.db";

	private PreferencesDatabaseConfigFactory() {
	}

	public static PreferencesDatabaseConfig<Configuration> createPreferencesDatabaseConfig() {
		return new PreferencesDatabaseConfig<>(
				SEARCHABLE_PREFERENCES_DB,
				Optional.empty(),
				PreferencesDatabaseConfig.JournalMode.TRUNCATE);
	}
}
