package de.blau.android.prefs.search;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withClassName;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withSubstring;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;

import static de.blau.android.prefs.search.ViewMatchers.childAtPosition;
import static de.blau.android.prefs.search.ViewMatchers.recyclerViewHasItem;

import android.app.Instrumentation;
import android.view.View;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.GrantPermissionRule;
import androidx.test.uiautomator.UiDevice;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import de.blau.android.Main;
import de.blau.android.R;
import de.blau.android.TestUtils;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class SettingsSearchTest {

	@Rule
	public ActivityScenarioRule<Main> mActivityScenarioRule = new ActivityScenarioRule<>(Main.class);

	@Rule
	public GrantPermissionRule mGrantPermissionRule =
			GrantPermissionRule.grant(
					"android.permission.ACCESS_FINE_LOCATION",
					"android.permission.ACCESS_MEDIA_LOCATION",
					"android.permission.READ_MEDIA_IMAGES",
					"android.permission.POST_NOTIFICATIONS");

	@Before
	public void setup() {
		final Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
		final UiDevice device = UiDevice.getInstance(instrumentation);
		TestUtils.grantPermissons(device);
		TestUtils.dismissStartUpDialogs(device, instrumentation.getTargetContext());
	}

	@Test
	public void shouldSearchAndFindBuiltinStyle() {
		// Given
		navigateToSearch();
		final String query = "Built-in (minimal)";

		// When
		onView(searchEditText()).perform(replaceText(query), closeSoftKeyboard());

		// Then
		onView(searchResultsView()).check(matches(hasSearchResultWithSubstring(query)));
	}

	private static void navigateToSearch() {
		onView(preferencesButton()).perform(click());
		onView(searchButton()).perform(click());
	}

	private static Matcher<View> preferencesButton() {
		return allOf(
				withId(R.id.menu_config),
				withContentDescription("Preferences"),
				childAtPosition(
						allOf(
								withId(R.id.bottomToolbar),
								childAtPosition(
										withId(R.id.bottomBar),
										0)),
						3),
				isDisplayed());
	}

	private static Matcher<View> searchButton() {
		return allOf(
				withContentDescription("Find"),
				childAtPosition(
						childAtPosition(
								withId(com.google.android.material.R.id.action_bar),
								3),
						0),
				isDisplayed());
	}

	private static Matcher<View> searchEditText() {
		return allOf(
				withClassName(is("android.widget.SearchView$SearchAutoComplete")),
				childAtPosition(
						allOf(
								withClassName(is("android.widget.LinearLayout")),
								childAtPosition(
										withClassName(is("android.widget.LinearLayout")),
										1)),
						0),
				isDisplayed());
	}

	private static Matcher<View> searchResultsView() {
		return allOf(
				withId(de.KnollFrank.lib.settingssearch.R.id.searchResults),
				childAtPosition(
						withClassName(is("android.widget.RelativeLayout")),
						0));
	}

	private static Matcher<View> hasSearchResultWithSubstring(final String substring) {
		return recyclerViewHasItem(hasDescendant(withSubstring(substring)));
	}
}
