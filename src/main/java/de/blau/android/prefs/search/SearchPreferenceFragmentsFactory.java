package de.blau.android.prefs.search;

import androidx.annotation.IdRes;
import androidx.fragment.app.FragmentActivity;

import de.KnollFrank.lib.settingssearch.client.SearchPreferenceFragments;
import de.KnollFrank.lib.settingssearch.client.searchDatabaseConfig.SearchDatabaseConfig;
import de.KnollFrank.lib.settingssearch.common.Locales;
import de.KnollFrank.lib.settingssearch.db.preference.db.PreferencesDatabase;

public class SearchPreferenceFragmentsFactory {

    private SearchPreferenceFragmentsFactory() {
    }

    public static SearchPreferenceFragments<Configuration> createSearchPreferenceFragments(
            final @IdRes int fragmentContainerViewId,
            final FragmentActivity activity,
            final PreferencesDatabase<Configuration> preferencesDatabase,
            final Configuration configuration,
            final SearchDatabaseConfig<Configuration> searchDatabaseConfig) {
        return SearchPreferenceFragments
                .builder(
                        searchDatabaseConfig,
                        SearchConfigFactory.createSearchConfig(fragmentContainerViewId, activity),
                        Locales.getCurrentLocale(activity.getResources().getConfiguration().getLocales()),
                        activity,
                        preferencesDatabase,
                        new ConfigurationBundleConverter().convertForward(configuration),
                        new ConfigurationBundleConverter())
                .build();
    }
}
