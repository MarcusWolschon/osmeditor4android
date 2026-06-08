package de.blau.android.prefs.search;

import androidx.annotation.IdRes;
import androidx.fragment.app.FragmentActivity;

import de.KnollFrank.lib.settingssearch.client.SearchConfig;
import de.KnollFrank.lib.settingssearch.common.Keyboard;

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

	private static void navigateToInitialPreferenceScreen(final FragmentActivity fragmentActivity) {
		fragmentActivity.runOnUiThread(() -> {
			Keyboard.hideKeyboard(fragmentActivity);
			fragmentActivity.getOnBackPressedDispatcher().onBackPressed();
		});
	}
}
