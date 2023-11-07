/* Port for backwards compatible version of TextfaceSpan for pre-Android 9 */
/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package de.blau.android.util;

import android.graphics.Typeface;
import android.os.Parcel;
import android.os.Process;
import android.util.ArrayMap;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;

/**
 * This class is used for Parceling Typeface object. Note: Typeface object can not be passed over the process boundary.
 *
 * @hide
 */
public class LeakyTypefaceStorage {
    private static final Object sLock = new Object();
    private static final ArrayList<Typeface> sStorage = new ArrayList<>();
    private static final ArrayMap<Typeface, Integer> sTypefaceMap = new ArrayMap<>();

    
    /**
     * Private constructor to stop instantiation
     */
    private LeakyTypefaceStorage() {
        // nothing
    }
    
    /**
     * Write typeface to parcel.
     *
     * You can't transfer Typeface to a different process. {@link readTypefaceFromParcel} will return {@code null} if
     * the {@link readTypefaceFromParcel} is called in a different process.
     *
     * @param typeface A {@link Typeface} to be written.
     * @param parcel A {@link Parcel} object.
     */
    public static void writeTypefaceToParcel(@Nullable Typeface typeface, @NonNull Parcel parcel) {
        parcel.writeInt(Process.myPid());
        synchronized (sLock) {
            final int id;
            final Integer i = sTypefaceMap.get(typeface);
            if (i != null) {
                id = i.intValue();
            } else {
                id = sStorage.size();
                sStorage.add(typeface);
                sTypefaceMap.put(typeface, id);
            }
            parcel.writeInt(id);
        }
    }

    /**
     * Read typeface from parcel.
     *
     * If the {@link Typeface} was created in another process, this method returns null.
     *
     * @param parcel A {@link Parcel} object
     * @return A {@link Typeface} object.
     */
    public static @androidx.annotation.Nullable Typeface readTypefaceFromParcel(@androidx.annotation.NonNull Parcel parcel) {
        final int pid = parcel.readInt();
        final int typefaceId = parcel.readInt();
        if (pid != Process.myPid()) {
            return null; // The Typeface was created and written in another process.
        }
        synchronized (sLock) {
            return sStorage.get(typefaceId);
        }
    }
}