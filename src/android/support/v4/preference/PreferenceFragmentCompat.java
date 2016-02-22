/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.support.v4.preference;

import java.lang.reflect.Method;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import de.blau.android.R;

public abstract class PreferenceFragmentCompat extends Fragment implements PreferenceFragmentCompatManager.OnPreferenceTreeClickListener {

    private static final String TAG = PreferenceFragmentCompat.class.getSimpleName();
    /**
     * The starting request code given out to preference framework.
     */
    private static final int FIRST_REQUEST_CODE = 100;
    private static final int MSG_BIND_PREFERENCES = 1;
    private Handler mHandler = new Handler() {

        @Override
        public void handleMessage(final Message msg) {
            switch (msg.what) {
                case MSG_BIND_PREFERENCES:
                    bindPreferences();
                    break ;
            }
        }

    };
    final private Runnable mRequestFocus = new Runnable() {

        @Override
        public void run() {
            if (mList == null) {
                Log.w(TAG, "ListView was NULL");
                return ;
            }
            mList.focusableViewAvailable(mList);
        }

    };

    private PreferenceManager mPreferenceManager;
    private ListView mList;
    private boolean mHavePrefs;
    private boolean mInitDone;

    /**
     * Interface that PreferenceFragment's containing activity should
     * implement to be able to process preference items that wish to
     * switch to a new fragment.
     */
    public interface OnPreferenceStartFragmentCallback {

        /**
         * Called when the user has clicked on a Preference that has
         * a fragment class name associated with it.  The implementation
         * to should instantiate and switch to an instance of the given
         * fragment.
         */
        boolean onPreferenceStartFragment(final PreferenceFragmentCompat caller, final Preference pref);

    }

    @Override
	public void onCreate(final Bundle paramBundle) {
		super.onCreate(paramBundle);
		mPreferenceManager = PreferenceFragmentCompatManager.newInstance(getActivity(), FIRST_REQUEST_CODE);
        // PreferenceFragmentCompatManager.setFragment(mPreferenceManager, this);
	}

    @Override
	public View onCreateView(final LayoutInflater paramLayoutInflater, final ViewGroup paramViewGroup, final Bundle paramBundle) {
		return paramLayoutInflater.inflate(R.layout.preference_list_fragment, paramViewGroup, false);
	}

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (mHavePrefs) {
            bindPreferences();
        }
        mInitDone = true;
        if (savedInstanceState != null) {
            final Bundle container = savedInstanceState.getBundle(PreferenceManager.METADATA_KEY_PREFERENCES);
            if (container != null) {
                final PreferenceScreen preferenceScreen = getPreferenceScreen();
                if (preferenceScreen != null) {
                    preferenceScreen.restoreHierarchyState(container);
                }
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        PreferenceFragmentCompatManager.setOnPreferenceTreeClickListener(mPreferenceManager, this);
    }

    @Override
	public void onStop() {
		super.onStop();
		PreferenceFragmentCompatManager.dispatchActivityStop(mPreferenceManager);
		PreferenceFragmentCompatManager.setOnPreferenceTreeClickListener(mPreferenceManager, null);
	}

    @Override
	public void onDestroyView() {
		mList = null;
		mHandler.removeCallbacks(mRequestFocus);
		mHandler.removeMessages(MSG_BIND_PREFERENCES);
		super.onDestroyView();
	}

    @Override
	public void onDestroy() {
		super.onDestroy();
		PreferenceFragmentCompatManager.dispatchActivityDestroy(mPreferenceManager);
	}

    @Override
    public void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
        final PreferenceScreen preferenceScreen = getPreferenceScreen();
        if (preferenceScreen != null) {
            final Bundle container = new Bundle();
            preferenceScreen.saveHierarchyState(container);
            outState.putBundle(PreferenceManager.METADATA_KEY_PREFERENCES, container);
        }
    }

    @Override
	public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
        PreferenceFragmentCompatManager.dispatchActivityResult(mPreferenceManager, requestCode, resultCode, data);
	}

    /**
     * @return {@link PreferenceManager} used by this {@link Fragment}
     */
    @SuppressWarnings("unused")
    public PreferenceManager getPreferenceManager() {
        return mPreferenceManager;
    }

    /**
     * @param preferenceScreen root {@link PreferenceScreen} of the preference hierarchy
     */
    public void setPreferenceScreen(final PreferenceScreen preferenceScreen) {
        if (PreferenceFragmentCompatManager.setPreferences(mPreferenceManager, preferenceScreen) && preferenceScreen != null) {
            mHavePrefs = true;
            if (mInitDone) {
                postBindPreferences();
            }
        }
    }

    /**
     * @return root {@link PreferenceScreen} of the preference hierarchy
     */
    public PreferenceScreen getPreferenceScreen() {
        return PreferenceFragmentCompatManager.getPreferenceScreen(mPreferenceManager);
    }

    /**
     * @param intent to query @{link Activity activities} to inflate preferences matching it
     */
    @SuppressWarnings("unused")
    public void addPreferencesFromIntent(final Intent intent) {
        requirePreferenceManager();
        setPreferenceScreen(PreferenceFragmentCompatManager.inflateFromIntent(mPreferenceManager, intent, getPreferenceScreen()));
    }

    /**
     * @param preferencesResId XML resource ID to inflate from
     */
    public void addPreferencesFromResource(final int preferencesResId) {
        requirePreferenceManager();
        setPreferenceScreen(PreferenceFragmentCompatManager.inflateFromResource(mPreferenceManager, getActivity(), preferencesResId, getPreferenceScreen()));
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("SimplifiableIfStatement")
    @Override
    public boolean onPreferenceTreeClick(final PreferenceScreen preferenceScreen, final Preference preference) {
        // if (preference.getFragment() != null &&
        if (getActivity() instanceof OnPreferenceStartFragmentCallback) {
            return ((OnPreferenceStartFragmentCallback) getActivity()).onPreferenceStartFragment(this, preference);
        }
        return false;
    }

    /**
     * @param key of the preference
     * @return {@link Preference} matching the key or null
     */
    public @Nullable Preference findPreference(final CharSequence key) {
        if (mPreferenceManager == null) {
            Log.w(TAG, "PreferenceManager was NULL");
            return null;
        }
        return mPreferenceManager.findPreference(key);
    }

    /**
     * @return {@link ListView} containing all preferences
     */
    public ListView getListView() {
        ensureList();
        return mList;
    }

    private void requirePreferenceManager() throws RuntimeException {
        if (mPreferenceManager == null) {
            throw new RuntimeException("This should be called after super.onCreate.");
        }
    }

    private void postBindPreferences() {
        if (mHandler.hasMessages(MSG_BIND_PREFERENCES)) {
            return ;
        }
        mHandler.obtainMessage(MSG_BIND_PREFERENCES).sendToTarget();
    }

    private void bindPreferences() {
        final PreferenceScreen preferenceScreen = getPreferenceScreen();
        if (preferenceScreen != null) {
            preferenceScreen.bind(getListView());
        }
        else {
            return ;
        }
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.GINGERBREAD_MR1) {
            // https://github.com/android/platform_frameworks_base/commit/2d43d283fc0f22b08f43c6db4da71031168e7f59
            getListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {

                @Override
                public void onItemClick(final AdapterView<?> parent, final View view, int position, final long id) {
                    // If the list has headers, subtract them from the index.
                    if (parent instanceof ListView) {
                        position -= ((ListView) parent).getHeaderViewsCount();
                    }
                    try {
                        final Object item = preferenceScreen.getRootAdapter().getItem(position);
                        if (! (item instanceof Preference)) {
                            return ;
                        }
                        final Preference preference = (Preference) item;
                        final Method performClick = Preference.class.getDeclaredMethod("performClick", PreferenceScreen.class);
                        performClick.setAccessible(true);
                        performClick.invoke(preference, preferenceScreen);
                    }
                    catch (final Exception e) {
                        Log.e(TAG, "Couldn't call Preference.performClick by reflection", e);
                    }
                }

            });
        }
    }

    private void ensureList() throws RuntimeException {
        final View root = getView();
        if (root == null) {
            throw new IllegalStateException("Content view not yet created");
        }
        final View listView = root.findViewById(android.R.id.list);
        if (listView == null) {
            throw new RuntimeException("Your content must have a ListView whose id attribute is 'android.R.id.list'");
        }
        if (! (listView instanceof ListView)) {
            throw new RuntimeException("Content has view with id attribute 'android.R.id.list' that is not a ListView class");
        }
        mList = (ListView) listView;
        mList.setOnKeyListener(new OnKeyListener() {

            @Override
            public boolean onKey(final View v, final int keyCode, final KeyEvent event) {
                final Object selectedItem = mList.getSelectedItem();
                if (selectedItem instanceof Preference) {
                    @SuppressWarnings("unused")
                    final View selectedView = mList.getSelectedView();
                    // return ((Preference) selectedItem).onKey(selectedView, keyCode, event);
                    return false;
                }
                return false;
            }

        });
        mHandler.post(mRequestFocus);
    }

}