package de.blau.android.prefs.search;

import android.app.Activity;
import android.content.Context;
import android.os.PersistableBundle;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.google.common.collect.ImmutableMap;

import java.util.Optional;
import java.util.Set;

import de.KnollFrank.lib.settingssearch.ActivityDescription;
import de.KnollFrank.lib.settingssearch.FragmentClassOfActivity;
import de.KnollFrank.lib.settingssearch.client.searchDatabaseConfig.ActivitySearchDatabaseConfigs;
import de.KnollFrank.lib.settingssearch.client.searchDatabaseConfig.SearchDatabaseConfig;
import de.KnollFrank.lib.settingssearch.db.preference.db.transformer.SearchablePreferenceScreenTreeCreator;
import de.KnollFrank.lib.settingssearch.db.preference.db.transformer.SearchablePreferenceScreenTreeTransformer;
import de.KnollFrank.lib.settingssearch.db.preference.db.transformer.TreeProcessorFactory;
import de.KnollFrank.lib.settingssearch.db.preference.pojo.TreeCreatorDescription;
import de.KnollFrank.lib.settingssearch.db.preference.pojo.TreeTransformerDescription;
import de.KnollFrank.lib.settingssearch.provider.PreferenceFragmentConnectedToPreferenceProvider;
import de.blau.android.R;
import de.blau.android.prefs.AdvancedPrefEditor;
import de.blau.android.prefs.AdvancedPrefEditorFragment;
import de.blau.android.prefs.PrefEditor;
import de.blau.android.prefs.PrefEditorFragment;
import de.blau.android.prefs.StyleConfigurationEditorActivity;

public class SearchDatabaseConfigFactory {

	private SearchDatabaseConfigFactory() {
	}

	public static SearchDatabaseConfig<Configuration> createSearchDatabaseConfig(final Context context) {
		return SearchDatabaseConfig
				.builder(
						getRootPreferenceFragment(),
						new TreeProcessorFactory<Configuration>() {

							@Override
							public SearchablePreferenceScreenTreeCreator<Configuration> createTreeCreator(final TreeCreatorDescription<Configuration> treeCreatorDescription) {
								throw new IllegalArgumentException(treeCreatorDescription.toString());
							}

							@Override
							public SearchablePreferenceScreenTreeTransformer<Configuration> createTreeTransformer(final TreeTransformerDescription<Configuration> treeTransformerDescription) {
								throw new IllegalArgumentException(treeTransformerDescription.toString());
							}
						})
				.withActivitySearchDatabaseConfigs(
						new ActivitySearchDatabaseConfigs(
								ImmutableMap
										.<Class<? extends Activity>, Class<? extends PreferenceFragmentCompat>>builder()
										.put(PrefEditor.class, PrefEditorFragment.class)
										.put(AdvancedPrefEditor.class, AdvancedPrefEditorFragment.class)
										.put(StyleConfigurationEditorActivity.class, StyleConfigurationEditorActivity.StyleConfigurationEditorProxy.class)
										.build(),
								Set.of()))
				.withPreferenceFragmentConnectedToPreferenceProvider(
						new PreferenceFragmentConnectedToPreferenceProvider() {

							@Override
							public Optional<Class<? extends PreferenceFragmentCompat>> getPreferenceFragmentConnectedToPreference(
									final Preference preference,
									final PreferenceFragmentCompat hostOfPreference) {
								// FK-TODO: refactor
								if (getConfigMapProfileKey().equals(preference.getKey())) {
									return Optional.of(StyleConfigurationEditorActivity.StyleConfigurationEditorProxy.class);
								}
								if (getConfigAdvancedPrefsKey().equals(preference.getKey())) {
									return Optional.of(AdvancedPrefEditorFragment.class);
								}
								return Optional.empty();
							}

							private String getConfigMapProfileKey() {
								return context.getString(R.string.config_mapProfile_key);
							}

							private String getConfigAdvancedPrefsKey() {
								return context.getString(R.string.config_advancedprefs_key);
							}
						})
				.build();
	}

	private static FragmentClassOfActivity<PrefEditorFragment> getRootPreferenceFragment() {
		return new FragmentClassOfActivity<>(
				PrefEditorFragment.class,
				new ActivityDescription(
						PrefEditor.class,
						new PersistableBundle()));
	}
}
