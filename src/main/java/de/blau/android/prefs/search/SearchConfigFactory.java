package de.blau.android.prefs.search;

import android.content.Context;

import androidx.annotation.IdRes;

import de.KnollFrank.lib.settingssearch.client.SearchConfig;

class SearchConfigFactory {

	private SearchConfigFactory() {
	}

	public static SearchConfig createSearchConfig(final @IdRes int fragmentContainerViewId,
	                                              final Context context) {
		return SearchConfig
				.builder(fragmentContainerViewId, context)
				.build();
	}
}
