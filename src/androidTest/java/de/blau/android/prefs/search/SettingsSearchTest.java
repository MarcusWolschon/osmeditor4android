package de.blau.android.prefs.search;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withClassName;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withParent;
import static androidx.test.espresso.matcher.ViewMatchers.withSubstring;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import static de.blau.android.prefs.search.ViewMatchers.childAtPosition;
import static de.blau.android.prefs.search.ViewMatchers.recyclerViewHasItem;
import static de.blau.android.prefs.search.ViewMatchers.recyclerViewHasItemCount;

import android.app.Instrumentation;
import android.view.View;

import androidx.test.core.app.ApplicationProvider;
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
	public void shouldFindStyle() {
		// Given
		final String query = "Built-in (minimal)";
		onView(preferencesButton()).perform(click());
		onView(searchButton()).perform(click());

		// When
		onView(searchEditText()).perform(replaceText(query), closeSoftKeyboard());

		// Then
		onView(searchResultsView()).check(matches(hasSearchResultWithSubstring(query)));
	}

	@Test
	public void shouldFindNewStyle() {
		// Given
		final String newStyle = "new style";
		onView(preferencesButton()).perform(click());
		addNewStyle(newStyle);
		onView(homeButton()).perform(click());
		onView(searchButton()).perform(click());

		// When
		onView(searchEditText()).perform(replaceText(newStyle), closeSoftKeyboard());

		// Then
		onView(searchResultsView()).check(matches(hasSearchResultWithSubstring(newStyle)));
	}

	@Test
	public void shouldFindEditedStyle() {
		// Given
		onView(preferencesButton()).perform(click());
		addNewStyle("new style");
		final String editedStyle = "new edited style";
		editStyle(editedStyle);
		onView(homeButton()).perform(click());
		onView(searchButton()).perform(click());

		// When
		onView(searchEditText()).perform(replaceText(editedStyle), closeSoftKeyboard());

		// Then
		onView(searchResultsView()).check(matches(hasSearchResultWithSubstring(editedStyle)));
	}

	@Test
	public void shouldNotFindDeletedStyle() {
		// Given
		final String newStyle = "new style";
		onView(preferencesButton()).perform(click());
		addNewStyle(newStyle);
		deleteNewStyle();
		onView(homeButton()).perform(click());
		onView(searchButton()).perform(click());

		// When
		onView(searchEditText()).perform(replaceText(newStyle), closeSoftKeyboard());

		// Then
		onView(searchResultsView()).check(matches(recyclerViewHasItemCount(equalTo(0))));
	}

	private static void deleteNewStyle() {
		onView(listItemMenu()).perform(click());
		onView(deleteButton()).perform(click());
		onView(yesButton()).perform(scrollTo(), click());
	}

	private static Matcher<View> listItemMenu() {
		return allOf(
				withId(R.id.listItemMenu),
				childAtPosition(
						withParent(withId(android.R.id.list)),
						2),
				isDisplayed());
	}

	private static Matcher<View> editButton() {
		return menuButtonWithText("Edit…");
	}

	private static Matcher<View> deleteButton() {
		return menuButtonWithText("Delete");
	}

	static Matcher<View> menuButtonWithText(final String text) {
		return allOf(
				withId(android.R.id.title),
				withText(text),
				childAtPosition(
						childAtPosition(
								withId(android.R.id.content),
								0),
						0),
				isDisplayed());
	}

	private static void addNewStyle(final String newStyle) {
		onView(configMapProfilePreference()).perform(actionOnItemAtPosition(1, click()));
		_addNewStyle(newStyle);
	}

	private static Matcher<View> configMapProfilePreference() {
		return allOf(
				withId(ch.poole.android.numberpickerpreference.R.id.recycler_view),
				childAtPosition(
						withId(android.R.id.list_container),
						0));
	}

	private static void editStyle(final String newStyle) {
		onView(listItemMenu()).perform(click());
		onView(editButton()).perform(click());
		onView(editName()).perform(replaceText(newStyle));
	}

	private static void _addNewStyle(final String newStyle) {
		onView(addStyleButton()).perform(click());
		enterNameAndValue(newStyle, "https://panoramax.ign.fr/api");
		onView(okButton()).perform(scrollTo(), click());
		TestUtils.textGone(
				UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()),
				ApplicationProvider.getApplicationContext().getString(R.string.progress_resource_download_message),
				30000);
	}

	private static Matcher<View> addStyleButton() {
		return allOf(
				withId(R.id.add),
				withContentDescription("Add style…"),
				childAtPosition(
						childAtPosition(
								withId(R.id.coordinator),
								0),
						1),
				isDisplayed());
	}

	private static void enterNameAndValue(final String name, final String value) {
		onView(editName()).perform(replaceText(name), closeSoftKeyboard());
		onView(editValue()).perform(replaceText(value), closeSoftKeyboard());
	}

	private static Matcher<View> editName() {
		return allOf(
				withId(R.id.listedit_editName),
				childAtPosition(
						allOf(withId(R.id.LinearLayout1),
								childAtPosition(
										withId(com.google.android.material.R.id.custom),
										0)),
						1),
				isDisplayed());
	}

	private static Matcher<View> editValue() {
		return allOf(
				withId(R.id.listedit_editValue),
				childAtPosition(
						allOf(withId(R.id.LinearLayout1),
								childAtPosition(
										withId(com.google.android.material.R.id.custom),
										0)),
						5),
				isDisplayed());
	}

	private static Matcher<View> homeButton() {
		return allOf(
				withContentDescription("Navigate up"),
				childAtPosition(
						allOf(
								withId(com.google.android.material.R.id.action_bar),
								childAtPosition(
										withId(com.google.android.material.R.id.action_bar_container),
										0)),
						1),
				isDisplayed());
	}

	private static Matcher<View> yesButton() {
		return buttonWithText("Yes");
	}

	private static Matcher<View> okButton() {
		return buttonWithText("OK");
	}

	private static Matcher<View> buttonWithText(final String text) {
		return allOf(
				withId(android.R.id.button1),
				withText(text),
				childAtPosition(
						childAtPosition(
								withId(com.google.android.material.R.id.buttonPanel),
								0),
						3));
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
