package de.blau.android.prefs.search;

import static de.blau.android.prefs.search.ToInitialPreferenceScreenNavigator.navigateToInitialPreferenceScreen;

import androidx.annotation.IdRes;
import androidx.fragment.app.FragmentActivity;

import de.KnollFrank.lib.settingssearch.client.SearchConfig;

class SearchConfigFactory {

	private SearchConfigFactory() {
	}

	public static SearchConfig createSearchConfig(final @IdRes int fragmentContainerViewId,
	                                              final FragmentActivity fragmentActivity) {
		return SearchConfig
				.builder(
						fragmentContainerViewId,
						fragmentActivity,
						() -> navigateToInitialPreferenceScreen(fragmentActivity))
				.build();
	}
}
