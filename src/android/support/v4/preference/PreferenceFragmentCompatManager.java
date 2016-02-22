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

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.support.v4.app.Fragment;
import android.util.Log;

public class PreferenceFragmentCompatManager {

	private static final String TAG = PreferenceFragmentCompatManager.class.getSimpleName();

	/**
	 * Interface definition for a callback to be invoked when a {@link Preference}
	 * in the hierarchy rooted at this {@link PreferenceScreen} is clicked.
	 */
    public interface OnPreferenceTreeClickListener {

		/**
		 * Called when a preference in the tree rooted at this
		 * {@link PreferenceScreen} has been clicked.
		 *
		 * @param preferenceScreen the {@link PreferenceScreen} that the preference is located in
		 * @param preference the preference that was clicked
		 * @return whether the click was handled
		 */
        boolean onPreferenceTreeClick(final PreferenceScreen preferenceScreen, final Preference preference);

    }

	public static PreferenceManager newInstance(final Activity activity, final int firstRequestCode) {
		try {
			final Constructor<PreferenceManager> constructor = PreferenceManager.class.getDeclaredConstructor(Activity.class, int.class);
			constructor.setAccessible(true);
			return constructor.newInstance(activity, firstRequestCode);
		}
		catch (final Exception e) {
			Log.w(TAG, "Couldn't call constructor PreferenceManager by reflection", e);
		}
		return null;
	}

	@Deprecated
	@SuppressWarnings("unused")
	public static void setFragment(final PreferenceManager manager, final PreferenceFragmentCompat fragment) {
		// STUB
    }

	@Deprecated
	@SuppressWarnings("unused")
	public static Fragment getFragment() {
		// STUB
		return null;
	}

	/**
	 * Sets the callback to be invoked when a {@link Preference} in the hierarchy rooted
	 * at this {@link PreferenceManager} is clicked.
	 *
	 * @param manager {@link PreferenceManager} to work with
	 * @param listener the callback to be invoked
	 */
	public static void setOnPreferenceTreeClickListener(final PreferenceManager manager, final OnPreferenceTreeClickListener listener) {
		try {
			final Field onPreferenceTreeClickListener = PreferenceManager.class.getDeclaredField("mOnPreferenceTreeClickListener");
			onPreferenceTreeClickListener.setAccessible(true);
			if (listener != null) {
				final Object proxy = Proxy.newProxyInstance(onPreferenceTreeClickListener.getType().getClassLoader(),
						new Class[] { onPreferenceTreeClickListener.getType() },
						new InvocationHandler() {

							@Override
							public Object invoke(final Object proxy, final Method method, final Object[] args) {
								if (method.getName().equals("onPreferenceTreeClick")) {
									return listener.onPreferenceTreeClick((PreferenceScreen) args[0], (Preference) args[1]);
								}
								else {
									return null;
								}
							}

						});
				onPreferenceTreeClickListener.set(manager, proxy);
			}
			else {
				onPreferenceTreeClickListener.set(manager, null);
			}
		}
		catch (final Exception e) {
			Log.w(TAG, "Couldn't set PreferenceManager.mOnPreferenceTreeClickListener by reflection", e);
		}
	}

	/**
	 * Inflates a preference hierarchy from the preference hierarchy of @{link Activity activities} that match the given {@link Intent}.
	 * See the {@link PreferenceManager#METADATA_KEY_PREFERENCES} key.
	 *
	 * @param manager {@link PreferenceManager} to work with
	 * @param intent to match @{link Activity activities}
	 * @param screen optional existing hierarchy to merge the new hierarchies into
	 * @return the root hierarchy (new or merged)
	 */
	public static PreferenceScreen inflateFromIntent(final PreferenceManager manager, final Intent intent, final PreferenceScreen screen) {
		try {
			final Method inflateFromIntent = PreferenceManager.class.getDeclaredMethod("inflateFromIntent", Intent.class, PreferenceScreen.class);
            inflateFromIntent.setAccessible(true);
            return (PreferenceScreen) inflateFromIntent.invoke(manager, intent, screen);
        }
		catch (final Exception e) {
			Log.w(TAG, "Couldn't call PreferenceManager.inflateFromIntent by reflection", e);
		}
		return null;
	}

	/**
	 * Inflates a preference hierarchy from XML.
	 *
	 * @param manager {@link PreferenceManager} to work with
	 * @param activity {@link Activity} to work with
	 * @param resId the resource ID of the XML to inflate
	 * @param screen optional existing hierarchy to merge the new hierarchies into
	 * @return the root hierarchy (new or merged)
	 */
	public static PreferenceScreen inflateFromResource(final PreferenceManager manager, final Activity activity, final int resId, final PreferenceScreen screen) {
		try {
			final Method inflateFromResource = PreferenceManager.class.getDeclaredMethod("inflateFromResource", Context.class, int.class, PreferenceScreen.class);
            inflateFromResource.setAccessible(true);
            return  (PreferenceScreen) inflateFromResource.invoke(manager, activity, resId, screen);
        }
		catch (final Exception e) {
			Log.w(TAG, "Couldn't call PreferenceManager.inflateFromResource by reflection", e);
		}
		return null;
	}

	/**
	 * @param manager {@link PreferenceManager} to work with
	 * @return the {@link PreferenceScreen} root of the hierarchy
	 */
	public static PreferenceScreen getPreferenceScreen(final PreferenceManager manager) {
		try {
			final Method getPreferenceScreen = PreferenceManager.class.getDeclaredMethod("getPreferenceScreen");
            getPreferenceScreen.setAccessible(true);
            return (PreferenceScreen) getPreferenceScreen.invoke(manager);
        }
		catch (final Exception e) {
			Log.w(TAG, "Couldn't call PreferenceManager.getPreferenceScreen by reflection", e);
		}
		return null;
	}

	/**
	 * Sets the root of the preference hierarchy.
	 *
	 * @param manager {@link PreferenceManager} to work with
	 * @param screen the root {@link PreferenceScreen} of the preference hierarchy
	 * @return whether the {@link PreferenceScreen} given differed from the previous one
	 */
	public static boolean setPreferences(final PreferenceManager manager, final PreferenceScreen screen) {
		try {
			final Method setPreferences = PreferenceManager.class.getDeclaredMethod("setPreferences", PreferenceScreen.class);
			setPreferences.setAccessible(true);
			return ((Boolean) setPreferences.invoke(manager, screen));
		}
		catch (final Exception e) {
			Log.w(TAG, "Couldn't call PreferenceManager.setPreferences by reflection", e);
		}
		return false;
	}

	/**
	 * Internal call from {@link PreferenceManager}.
	 */
	public static void dispatchActivityResult(final PreferenceManager manager, final int requestCode, final int resultCode, final Intent data) {
		try {
			final Method dispatchActivityResult = PreferenceManager.class.getDeclaredMethod("dispatchActivityResult", int.class, int.class, Intent.class);
            dispatchActivityResult.setAccessible(true);
            dispatchActivityResult.invoke(manager, requestCode, resultCode, data);
        }
		catch (final Exception e) {
			Log.w(TAG, "Couldn't call PreferenceManager.dispatchActivityResult by reflection", e);
		}
	}

	/**
	 * Internal call from {@link PreferenceManager}.
	 */
	public static void dispatchActivityStop(final PreferenceManager manager) {
		try {
			final Method dispatchActivityStop = PreferenceManager.class.getDeclaredMethod("dispatchActivityStop");
            dispatchActivityStop.setAccessible(true);
            dispatchActivityStop.invoke(manager);
        }
		catch (final Exception e) {
			Log.w(TAG, "Couldn't call PreferenceManager.dispatchActivityStop by reflection", e);
		}
	}

	/**
	 * Internal call from {@link PreferenceManager}.
	 */
	public static void dispatchActivityDestroy(final PreferenceManager manager) {
		try {
			final Method dispatchActivityDestroy = PreferenceManager.class.getDeclaredMethod("dispatchActivityDestroy");
			dispatchActivityDestroy.setAccessible(true);
			dispatchActivityDestroy.invoke(manager);
		}
		catch (final Exception e) {
			Log.w(TAG, "Couldn't call PreferenceManager.dispatchActivityDestroy by reflection", e);
		}
	}

}
