package de.blau.android.prefs.search;

import android.os.PersistableBundle;

public class ConfigurationBundleConverter implements de.KnollFrank.lib.settingssearch.db.preference.pojo.converters.ConfigurationBundleConverter<Configuration> {

	@Override
	public PersistableBundle convertForward(final Configuration configuration) {
		return new PersistableBundle();
	}

	@Override
	public Configuration convertBackward(final PersistableBundle bundle) {
		return new Configuration();
	}
}
