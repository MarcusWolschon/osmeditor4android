package de.blau.android.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.app.LocaleConfig;
import android.content.Context;
import android.os.Build;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.core.os.LocaleListCompat;
import de.blau.android.R;

public final class LocaleUtils {

    private static final String DEBUG_TAG = LocaleUtils.class.getSimpleName().substring(0, Math.min(23, LocaleUtils.class.getSimpleName().length()));

    private static final String ANDROID_LOCALE_SEPARATOR = "_";

    // list of languages that use Latin script from https://gist.github.com/phil-brown/8056700
    private static Set<String> latin = new HashSet<>(Arrays.asList("aa", "ace", "ach", "ada", "af", "agq", "ak", "ale", "amo", "an", "arn", "arp", "arw", "asa",
            "ast", "ay", "az", "bal", "ban", "bas", "bbc", "bem", "bez", "bi", "bik", "bin", "bku", "bla", "bm", "bqv", "br", "bs", "buc", "bug", "bya", "ca",
            "cad", "car", "cay", "cch", "ceb", "cgg", "ch", "chk", "chn", "cho", "chp", "chy", "co", "cpe", "cs", "csb", "cy", "da", "dak", "dav", "de", "del",
            "den", "dgr", "din", "dje", "dsb", "dua", "dyu", "ebu", "ee", "efi", "eka", "en", "eo", "es", "et", "eu", "ewo", "fan", "ff", "fi", "fil", "fiu",
            "fj", "fo", "fon", "fr", "frr", "frs", "fur", "fy", "ga", "gaa", "gag", "gay", "gcr", "gd", "gil", "gl", "gn", "gor", "grb", "gsw", "guz", "gv",
            "gwi", "ha", "hai", "haw", "hil", "hmn", "hnn", "ho", "hop", "hr", "hsb", "ht", "hu", "hup", "hz", "ia", "iba", "ibb", "id", "ig", "ii", "ik",
            "ilo", "is", "it", "iu", "jmc", "jv", "kab", "kac", "kaj", "kam", "kcg", "kde", "kea", "kfo", "kg", "kha", "khq", "ki", "kj", "kl", "kln", "kmb",
            "kos", "kpe", "kr", "kri", "krl", "ksb", "ksf", "ksh", "ku", "kut", "kw", "ky", "la", "lag", "lam", "lb", "lg", "li", "ln", "lol", "loz", "lt",
            "lu", "lua", "lui", "lun", "luo", "lut", "luy", "lv", "mad", "mak", "man", "mas", "mdh", "mdr", "men", "mer", "mfe", "mg", "mgh", "mh", "mi", "mic",
            "min", "moh", "mos", "ms", "mt", "mua", "mus", "mwl", "na", "nap", "naq", "nb", "nd", "nds", "ng", "nia", "niu", "nl", "nmg", "nn", "nr", "nso",
            "nus", "nv", "ny", "nym", "nyn", "nyo", "nzi", "oc", "om", "osa", "pag", "pam", "pap", "pau", "pl", "pon", "prg", "pt", "qu", "raj", "rap", "rar",
            "rcf", "rej", "rm", "rn", "ro", "rof", "rup", "rw", "rwk", "sad", "saf", "saq", "sas", "sat", "sbp", "sc", "scn", "sco", "se", "see", "seh", "ses",
            "sg", "sga", "sid", "sk", "sl", "sm", "sma", "smi", "smj", "smn", "sms", "sn", "snk", "so", "son", "sq", "sr", "srn", "srr", "ss", "ssy", "st",
            "su", "suk", "sus", "sv", "sw", "swb", "swc", "tbw", "tem", "teo", "ter", "tet", "tiv", "tk", "tkl", "tli", "tmh", "tn", "to", "tog", "tpi", "tr",
            "tru", "trv", "ts", "tsg", "tsi", "tum", "tvl", "twq", "ty", "tzm", "udm", "uli", "umb", "uz", "ve", "vi", "vo", "vot", "vun", "wa", "wae", "wak",
            "war", "was", "wo", "xh", "xog", "yao", "yap", "yav", "yo", "za", "zap", "zu", "zun", "zza", "sbp", "sc", "scn", "sco", "se", "see", "seh", "ses",
            "sg", "sga", "sid", "sk", "sl", "sm", "sma", "smi", "smj", "smn", "sms", "sn", "snk", "so", "son", "sq", "sr", "srn", "srr", "ss", "ssy", "st",
            "su", "suk", "sus", "sv", "sw", "swb", "swc", "tbw", "tem", "teo", "ter", "tet", "tiv", "tk", "tkl", "tli", "tmh", "tn", "to", "tog", "tpi", "tr",
            "tru", "trv", "ts", "tsg", "tsi", "tum", "tvl", "twq", "ty", "tzm", "udm", "uli", "umb", "uz", "ve", "vi", "vo", "vot", "vun", "wa", "wae", "wak",
            "war", "was", "wo", "xh", "xog", "yao", "yap", "yav", "yo", "za", "zap", "zu", "zun"));

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
        return loc.toLanguageTag();
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
        return Locale.forLanguageTag(languageTag);
    }

    /**
     * Determine if the Locale uses Latin script
     * 
     * @param locale the Locale to check
     * @return true if the Locale uses Latin script and we are running at least on SDK 21 / Lollipop
     */
    public static boolean usesLatinScript(@NonNull Locale locale) {
        return latin.contains(locale.getLanguage());
    }

    /**
     * Get a list of supported locales for the app
     * 
     * For devices prior to Android 13 this reads and parses locales_config.xml directly, note that since we are using
     * automatic generation of the file it has a different name.
     * 
     * @param context an Android Context
     * @return a LocaleListCompat
     */
    public static LocaleListCompat getSupportedLocales(@NonNull Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return LocaleListCompat.wrap(new LocaleConfig(context).getSupportedLocales());
        }
        List<String> locales = new ArrayList<>();
        try {
            XmlPullParser parser = context.getResources().getXml(R.xml._generated_res_locale_config);
            while (parser.getEventType() != XmlPullParser.END_DOCUMENT) {
                if (parser.getEventType() == XmlPullParser.START_TAG && "locale".equals(parser.getName())) {
                    locales.add(parser.getAttributeValue(0));
                }
                parser.next();
            }
        } catch (XmlPullParserException | IOException e) {
            Log.e(DEBUG_TAG, "Error reading locales_config " + e.getMessage());
        }
        return LocaleListCompat.forLanguageTags(String.join(",", locales));
    }

    /**
     * Construct a Locale from an Android format locale string
     * 
     * @param localeString the Android format locale string
     * @return a Locale
     */
    @NonNull
    public static Locale localeFromAndroidLocaleTag(@NonNull String localeString) {
        String[] code = localeString.split(ANDROID_LOCALE_SEPARATOR);
        return code.length == 1 ? new Locale(code[0]) : new Locale(code[0], code[1]);
    }
}
