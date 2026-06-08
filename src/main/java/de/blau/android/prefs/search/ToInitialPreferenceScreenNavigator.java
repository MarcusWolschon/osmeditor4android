package de.blau.android.prefs.search;

import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import de.KnollFrank.lib.settingssearch.common.Keyboard;

class ToInitialPreferenceScreenNavigator {

	private ToInitialPreferenceScreenNavigator() {
	}

	public static void navigateToInitialPreferenceScreen(final FragmentActivity fragmentActivity) {
		fragmentActivity.runOnUiThread(() -> {
			Keyboard.hideKeyboard(fragmentActivity);
			_navigateToInitialPreferenceScreen(fragmentActivity);
		});
	}

	private static void _navigateToInitialPreferenceScreen(final FragmentActivity fragmentActivity) {
		fragmentActivity
				.getSupportFragmentManager()
				.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
	}
}
