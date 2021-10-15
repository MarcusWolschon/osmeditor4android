package de.blau.android.util;

import java.util.Locale;

import android.os.Build;
import androidx.annotation.NonNull;

public final class LocaleUtils {

    /**
     * Private constructor to prevent instantiation
     */
    private LocaleUtils() {
        // do nothing
    }

    /**
     * Returns a well-formed ITEF BCP 47 language tag representing this locale string identifier for the client's
     * current locale
     *
     * @param locale the Locale to use
     * @return String: The BCP 47 language tag for the current locale
     */
    public static String toLanguageTag(@NonNull Locale locale) {
        return toBcp47Language(locale);
    }

    /**
     * See https://stackoverflow.com/questions/29657781/how-to-i-get-the-ietf-bcp47-language-code-in-android-api-21
     * 
     * Modified from: https://github.com/apache/cordova-plugin-globalization/blob/master/src/android/Globalization.java
     * 
     * Returns a well-formed ITEF BCP 47 language tag representing this locale string identifier for the client's
     * current locale
     *
     * @param loc the Locale to use
     * @return String: The BCP 47 language tag for the current locale
     */
    public static String toBcp47Language(@NonNull Locale loc) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return loc.toLanguageTag();
        }

        // we will use a dash as per BCP 47
        final char SEP = '-';
        String language = loc.getLanguage();
        String region = loc.getCountry();
        String variant = loc.getVariant();

        // special case for Norwegian Nynorsk since "NY" cannot be a variant as per BCP 47
        // this goes before the string matching since "NY" wont pass the variant checks
        if ("no".equals(language) && "NO".equals(region) && "NY".equals(variant)) {
            language = "nn";
            region = "NO";
            variant = "";
        }

        if (language.isEmpty() || !language.matches("\\p{Alpha}{2,8}")) {
            language = "und"; // Follow the Locale#toLanguageTag() implementation
            // which says to return "und" for Undetermined
        } else if ("iw".equals(language)) {
            language = "he"; // correct deprecated "Hebrew"
        } else if ("in".equals(language)) {
            language = "id"; // correct deprecated "Indonesian"
        } else if ("ji".equals(language)) {
            language = "yi"; // correct deprecated "Yiddish"
        }

        // ensure valid country code, if not well formed, it's omitted
        if (!region.matches("\\p{Alpha}{2}|\\p{Digit}{3}")) {
            region = "";
        }

        // variant subtags that begin with a letter must be at least 5 characters long
        if (!variant.matches("\\p{Alnum}{5,8}|\\p{Digit}\\p{Alnum}{3}")) {
            variant = "";
        }

        StringBuilder bcp47Tag = new StringBuilder(language);
        if (!region.isEmpty()) {
            bcp47Tag.append(SEP).append(region);
        }
        if (!variant.isEmpty()) {
            bcp47Tag.append(SEP).append(variant);
        }

        return bcp47Tag.toString();
    }

    // The following code is
    //
    // Copyright 2014 The Chromium Authors. All rights reserved.
    // Use of this source code is governed by a BSD-style license that can be
    // found in the LICENSE file.
    // https://chromium.googlesource.com/chromium/+/refs/heads/trunk/LICENSE

    /**
     * This function creates a Locale object from xx-XX style string where xx is language code and XX is a country code.
     * This works for API level lower than 21.
     * 
     * @param languageTag the language code string
     * @return the locale that best represents the language tag.
     */
    public static Locale forLanguageTagCompat(@NonNull String languageTag) {
        String[] tag = languageTag.split("-");
        if (tag.length == 0) {
            return new Locale("");
        }
        String language = tag[0];
        if ((language.length() != 2 && language.length() != 3)) {
            return new Locale("");
        }
        if (tag.length == 1) {
            return new Locale(language);
        }
        String country = tag[1];
        if (country.length() != 2 && country.length() != 3) {
            return new Locale(language);
        }
        if (tag.length == 2) {
            return new Locale(language, country);
        }
        String variant = tag[2];
        return new Locale(language, country, variant);
    }

    /**
     * This function creates a Locale object from xx-XX style string where xx is language code and XX is a country code.
     * 
     * @param languageTag the language code string
     * @return the locale that best represents the language tag.
     */
    public static Locale forLanguageTag(@NonNull String languageTag) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return Locale.forLanguageTag(languageTag);
        }
        return forLanguageTagCompat(languageTag);
    }
}
