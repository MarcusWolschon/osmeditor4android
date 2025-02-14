package de.blau.android.util;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.app.LocaleConfig;
import android.content.Context;
import android.os.Build;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.os.LocaleListCompat;
import de.blau.android.R;

public final class LocaleUtils {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, LocaleUtils.class.getSimpleName().length());
    private static final String DEBUG_TAG = LocaleUtils.class.getSimpleName().substring(0, TAG_LEN);

    private static final String              GREEK                 = "Grek";
    private static final String              RUNIC                 = "Runr";
    private static final String              GOTHIC                = "Goth";
    private static final String              ARMENIAN              = "Armn";
    private static final String              THAI                  = "Thai";
    private static final Map<String, String> THAI_SCRIPT_MAP       = getScriptsMap("", THAI);
    private static final String              HEBREW                = "Hebr";
    private static final Map<String, String> HEBREW_SCRIPT_MAP     = getScriptsMap("", HEBREW);
    private static final String              DEVANAGARI            = "Deva";
    private static final Map<String, String> DEVANAGARI_SCRIPT_MAP = getScriptsMap("", DEVANAGARI);
    private static final String              ARABIC                = "Arab";
    private static final Map<String, String> ARABIC_SCRIPT_MAP     = getScriptsMap("", ARABIC);
    private static final String              KANA                  = "Kana";
    private static final String              AVESTAN               = "Avst";
    private static final String              CYRILLIC              = "Cyrl";
    private static final Map<String, String> CYRILLIC_SCRIPT_MAP   = getScriptsMap("", CYRILLIC);
    private static final String              LATIN                 = "Latn";
    private static final Map<String, String> LATIN_SCRIPT_MAP      = getScriptsMap("", LATIN);
    private static final Map<String, String> BENG_SCRIPT_MAP       = getScriptsMap("", "Beng");
    private static final Map<String, String> ETHI_SCRIPT_MAP       = getScriptsMap("", "Ethi");
    private static final Map<String, String> EMPTY_SCRIPT_MAP      = getScriptsMap("", "");

    private static final String ANDROID_LOCALE_SEPARATOR = "_";

    /*
     * The following code is
     * 
     * 
     * Copyright 2013 Phil Brown
     *
     * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
     * with the License. You may obtain a copy of the License at
     * 
     * http://www.apache.org/licenses/LICENSE-2.0
     * 
     * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
     * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for
     * the specific language governing permissions and limitations under the License.
     *
     *
     * Get Script name by Locale <br>
     * 
     * @author Phil Brown
     * 
     * @since 9:47:09 AM Dec 20, 2013
     *
     */
    private static Map<String, Map<String, String>> scriptsByLocale = new HashMap<>();

    @NonNull
    private static Map<String, String> getScriptsMap(@NonNull String... keyValuePairs) {
        Map<String, String> languages = new HashMap<>();
        for (int i = 0; i < keyValuePairs.length; i += 2) {
            languages.put(keyValuePairs[i], keyValuePairs[i + 1]);
        }
        return languages;
    }

    static {
        scriptsByLocale.put("aa", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("ab", CYRILLIC_SCRIPT_MAP);
        scriptsByLocale.put("abq", CYRILLIC_SCRIPT_MAP);
        scriptsByLocale.put("abr", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("ace", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("ach", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("ada", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("ady", CYRILLIC_SCRIPT_MAP);
        scriptsByLocale.put("ae", getScriptsMap("", AVESTAN));
        scriptsByLocale.put("af", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("agq", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("aii", CYRILLIC_SCRIPT_MAP);
        scriptsByLocale.put("ain", getScriptsMap("", KANA));
        scriptsByLocale.put("ak", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("akk", getScriptsMap("", "Xsux"));
        scriptsByLocale.put("ale", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("alt", CYRILLIC_SCRIPT_MAP);
        scriptsByLocale.put("am", ETHI_SCRIPT_MAP);
        scriptsByLocale.put("amo", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("an", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("anp", DEVANAGARI_SCRIPT_MAP);
        scriptsByLocale.put("aoz", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("ar", getScriptsMap("", ARABIC, "IR", "Syrc"));
        scriptsByLocale.put("arc", getScriptsMap("", "Armi"));
        scriptsByLocale.put("arn", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("arp", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("arw", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("as", BENG_SCRIPT_MAP);
        scriptsByLocale.put("asa", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("ast", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("atj", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("av", CYRILLIC_SCRIPT_MAP);
        scriptsByLocale.put("awa", DEVANAGARI_SCRIPT_MAP);
        scriptsByLocale.put("ay", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("az", getScriptsMap("", LATIN, "AZ", CYRILLIC, "IR", ARABIC));
        scriptsByLocale.put("ba", CYRILLIC_SCRIPT_MAP);
        scriptsByLocale.put("bal", getScriptsMap("", ARABIC, "IR", LATIN, "PK", LATIN));
        scriptsByLocale.put("ban", getScriptsMap("", LATIN, "ID", "Bali"));
        scriptsByLocale.put("bap", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("bas", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("bax", getScriptsMap("", "Bamu"));
        scriptsByLocale.put("bbc", getScriptsMap("", LATIN, "ID", "Batk"));
        scriptsByLocale.put("bbj", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("bci", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("be", CYRILLIC_SCRIPT_MAP);
        scriptsByLocale.put("bej", ARABIC_SCRIPT_MAP);
        scriptsByLocale.put("bem", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("bew", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("bez", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("bfd", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("bfq", getScriptsMap("", "Taml"));
        scriptsByLocale.put("bft", ARABIC_SCRIPT_MAP);
        scriptsByLocale.put("bfy", DEVANAGARI_SCRIPT_MAP);
        scriptsByLocale.put("bg", CYRILLIC_SCRIPT_MAP);
        scriptsByLocale.put("bgc", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("bgx", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("bh", DEVANAGARI_SCRIPT_MAP);
        scriptsByLocale.put("bhb", DEVANAGARI_SCRIPT_MAP);
        scriptsByLocale.put("bhi", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("bhk", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("bho", DEVANAGARI_SCRIPT_MAP);
        scriptsByLocale.put("bi", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("bik", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("bin", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("bjj", DEVANAGARI_SCRIPT_MAP);
        scriptsByLocale.put("bjn", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("bkm", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("bku", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("bla", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("blt", getScriptsMap("", "Tavt"));
        scriptsByLocale.put("bm", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("bmq", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("bn", BENG_SCRIPT_MAP);
        scriptsByLocale.put("bo", getScriptsMap("", "Tibt"));
        scriptsByLocale.put("bqi", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("bqv", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("br", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("bra", DEVANAGARI_SCRIPT_MAP);
        scriptsByLocale.put("brh", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("brx", DEVANAGARI_SCRIPT_MAP);
        scriptsByLocale.put("bs", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("bss", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("bto", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("btv", DEVANAGARI_SCRIPT_MAP);
        scriptsByLocale.put("bua", CYRILLIC_SCRIPT_MAP);
        scriptsByLocale.put("buc", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("bug", getScriptsMap("", LATIN, "ID", "Bugi"));
        scriptsByLocale.put("bum", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("bvb", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("bya", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("byn", ETHI_SCRIPT_MAP);
        scriptsByLocale.put("byv", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("bze", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("bzx", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("ca", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("cad", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("car", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("cay", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("cch", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("ccp", BENG_SCRIPT_MAP);
        scriptsByLocale.put("ce", CYRILLIC_SCRIPT_MAP);
        scriptsByLocale.put("ceb", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("cgg", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("ch", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("chk", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("chm", CYRILLIC_SCRIPT_MAP);
        scriptsByLocale.put("chn", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("cho", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("chp", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("chr", getScriptsMap("", "Cher"));
        scriptsByLocale.put("chy", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("cja", ARABIC_SCRIPT_MAP);
        scriptsByLocale.put("cjm", getScriptsMap("", "Cham"));
        scriptsByLocale.put("cjs", CYRILLIC_SCRIPT_MAP);
        scriptsByLocale.put("ckb", ARABIC_SCRIPT_MAP);
        scriptsByLocale.put("ckt", CYRILLIC_SCRIPT_MAP);
        scriptsByLocale.put("co", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("cop", ARABIC_SCRIPT_MAP);
        scriptsByLocale.put("cpe", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("cr", getScriptsMap("", "Cans"));
        scriptsByLocale.put("crh", CYRILLIC_SCRIPT_MAP);
        scriptsByLocale.put("crj", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("crk", getScriptsMap("", "Cans"));
        scriptsByLocale.put("crl", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("crm", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("crs", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("cs", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("csb", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("csw", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("cu", getScriptsMap("", "Glag"));
        scriptsByLocale.put("cv", CYRILLIC_SCRIPT_MAP);
        scriptsByLocale.put("cy", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("da", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("daf", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("dak", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("dar", CYRILLIC_SCRIPT_MAP);
        scriptsByLocale.put("dav", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("dcc", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("de", getScriptsMap("", LATIN, "AT", LATIN, "CH", LATIN, "BR", RUNIC, "KZ", RUNIC, "US", RUNIC));
        scriptsByLocale.put("del", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("den", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("dgr", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("din", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("dje", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("dng", CYRILLIC_SCRIPT_MAP);
        scriptsByLocale.put("doi", ARABIC_SCRIPT_MAP);
        scriptsByLocale.put("dsb", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("dtm", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("dua", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("dv", getScriptsMap("", "Thaa"));
        scriptsByLocale.put("dyo", ARABIC_SCRIPT_MAP);
        scriptsByLocale.put("dyu", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("dz", getScriptsMap("", "Tibt"));
        scriptsByLocale.put("ebu", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("ee", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("efi", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("egy", getScriptsMap("", "Egyp"));
        scriptsByLocale.put("eka", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("eky", getScriptsMap("", "Kali"));
        scriptsByLocale.put("el", getScriptsMap("", GREEK));
        scriptsByLocale.put("en", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("eo", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("es", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("et", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("ett", getScriptsMap("", "Ital"));
        scriptsByLocale.put("eu", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("evn", CYRILLIC_SCRIPT_MAP);
        scriptsByLocale.put("ewo", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("fa", ARABIC_SCRIPT_MAP);
        scriptsByLocale.put("fan", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("ff", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("ffm", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("fi", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("fil", getScriptsMap("", LATIN, "US", "Tglg"));
        scriptsByLocale.put("fiu", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("fj", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("fo", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("fon", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("fr", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("frr", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("frs", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("fud", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("fuq", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("fur", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("fuv", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("fy", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("ga", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("gaa", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("gag", getScriptsMap("", LATIN, "MD", CYRILLIC));
        scriptsByLocale.put("gay", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("gba", ARABIC_SCRIPT_MAP);
        scriptsByLocale.put("gbm", DEVANAGARI_SCRIPT_MAP);
        scriptsByLocale.put("gcr", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("gd", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("gez", ETHI_SCRIPT_MAP);
        scriptsByLocale.put("ggn", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("gil", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("gjk", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("gju", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("gl", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("gld", CYRILLIC_SCRIPT_MAP);
        scriptsByLocale.put("glk", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("gn", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("gon", getScriptsMap("", "Telu"));
        scriptsByLocale.put("gor", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("gos", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("got", getScriptsMap("", GOTHIC));
        scriptsByLocale.put("grb", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("grc", getScriptsMap("", "Cprt"));
        scriptsByLocale.put("grt", BENG_SCRIPT_MAP);
        scriptsByLocale.put("gsw", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("gu", getScriptsMap("", "Gujr"));
        scriptsByLocale.put("gub", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("guz", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("gv", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("gvr", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("gwi", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("ha", getScriptsMap("", ARABIC, "NE", LATIN, "GH", LATIN));
        scriptsByLocale.put("hai", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("haw", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("haz", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("he", HEBREW_SCRIPT_MAP);
        scriptsByLocale.put("hi", DEVANAGARI_SCRIPT_MAP);
        scriptsByLocale.put("hil", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("hit", getScriptsMap("", "Xsux"));
        scriptsByLocale.put("hmn", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("hnd", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("hne", DEVANAGARI_SCRIPT_MAP);
        scriptsByLocale.put("hnn", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("hno", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("ho", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("hoc", DEVANAGARI_SCRIPT_MAP);
        scriptsByLocale.put("hoj", DEVANAGARI_SCRIPT_MAP);
        scriptsByLocale.put("hop", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("hr", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("hsb", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("ht", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("hu", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("hup", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("hy", getScriptsMap("", ARMENIAN));
        scriptsByLocale.put("hz", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("ia", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("iba", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("ibb", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("id", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("ig", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("ii", getScriptsMap("", "Yiii", "CN", LATIN));
        scriptsByLocale.put("ik", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("ikt", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("ilo", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("inh", CYRILLIC_SCRIPT_MAP);
        scriptsByLocale.put("is", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("it", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("iu", getScriptsMap("", "Cans", "CA", LATIN));
        scriptsByLocale.put("ja", getScriptsMap("", "Jpan"));
        scriptsByLocale.put("jmc", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("jml", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("jpr", HEBREW_SCRIPT_MAP);
        scriptsByLocale.put("jrb", HEBREW_SCRIPT_MAP);
        scriptsByLocale.put("jv", getScriptsMap("", LATIN, "ID", "Java"));
        scriptsByLocale.put("ka", getScriptsMap("", "Geor"));
        scriptsByLocale.put("kaa", CYRILLIC_SCRIPT_MAP);
        scriptsByLocale.put("kab", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("kac", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("kaj", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("kam", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("kao", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("kbd", CYRILLIC_SCRIPT_MAP);
        scriptsByLocale.put("kca", CYRILLIC_SCRIPT_MAP);
        scriptsByLocale.put("kcg", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("kck", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("kde", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("kdt", THAI_SCRIPT_MAP);
        scriptsByLocale.put("kea", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("kfo", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("kfr", DEVANAGARI_SCRIPT_MAP);
        scriptsByLocale.put("kfy", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("kg", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("kge", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("kgp", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("kha", getScriptsMap("", LATIN, "IN", "Beng"));
        scriptsByLocale.put("khb", getScriptsMap("", "Talu"));
        scriptsByLocale.put("khn", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("khq", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("kht", getScriptsMap("", "Mymr"));
        scriptsByLocale.put("khw", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("ki", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("kj", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("kjg", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("kjh", CYRILLIC_SCRIPT_MAP);
        scriptsByLocale.put("kk", getScriptsMap("", ARABIC, "KZ", CYRILLIC, "TR", CYRILLIC));
        scriptsByLocale.put("kkj", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("kl", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("kln", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("km", getScriptsMap("", "Khmr"));
        scriptsByLocale.put("kmb", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("kn", getScriptsMap("", "Knda"));
        scriptsByLocale.put("ko", getScriptsMap("", "Kore"));
        scriptsByLocale.put("koi", CYRILLIC_SCRIPT_MAP);
        scriptsByLocale.put("kok", DEVANAGARI_SCRIPT_MAP);
        scriptsByLocale.put("kos", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("kpe", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("kpy", CYRILLIC_SCRIPT_MAP);
        scriptsByLocale.put("kr", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("krc", CYRILLIC_SCRIPT_MAP);
        scriptsByLocale.put("kri", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("krl", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("kru", DEVANAGARI_SCRIPT_MAP);
        scriptsByLocale.put("ks", ARABIC_SCRIPT_MAP);
        scriptsByLocale.put("ksb", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("ksf", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("ksh", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("ku", getScriptsMap("", LATIN, "LB", ARABIC));
        scriptsByLocale.put("kum", CYRILLIC_SCRIPT_MAP);
        scriptsByLocale.put("kut", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("kv", CYRILLIC_SCRIPT_MAP);
        scriptsByLocale.put("kvr", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("kvx", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("kw", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("kxm", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("kxp", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("ky", getScriptsMap("", CYRILLIC, "CN", ARABIC, "TR", LATIN));
        scriptsByLocale.put("kyu", getScriptsMap("", "Kali"));
        scriptsByLocale.put("la", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("lad", HEBREW_SCRIPT_MAP);
        scriptsByLocale.put("lag", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("lah", ARABIC_SCRIPT_MAP);
        scriptsByLocale.put("laj", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("lam", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("lb", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("lbe", CYRILLIC_SCRIPT_MAP);
        scriptsByLocale.put("lbw", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("lcp", THAI_SCRIPT_MAP);
        scriptsByLocale.put("lep", getScriptsMap("", "Lepc"));
        scriptsByLocale.put("lez", CYRILLIC_SCRIPT_MAP);
        scriptsByLocale.put("lg", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("li", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("lif", DEVANAGARI_SCRIPT_MAP);
        scriptsByLocale.put("lis", getScriptsMap("", "Lisu"));
        scriptsByLocale.put("ljp", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("lki", ARABIC_SCRIPT_MAP);
        scriptsByLocale.put("lkt", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("lmn", getScriptsMap("", "Telu"));
        scriptsByLocale.put("lmo", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("ln", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("lo", getScriptsMap("", "Laoo"));
        scriptsByLocale.put("lol", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("loz", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("lrc", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("lt", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("lu", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("lua", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("lui", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("lun", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("luo", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("lus", BENG_SCRIPT_MAP);
        scriptsByLocale.put("lut", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("luy", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("luz", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("lv", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("lwl", THAI_SCRIPT_MAP);
        scriptsByLocale.put("mad", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("maf", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("mag", DEVANAGARI_SCRIPT_MAP);
        scriptsByLocale.put("mai", DEVANAGARI_SCRIPT_MAP);
        scriptsByLocale.put("mak", getScriptsMap("", LATIN, "ID", "Bugi"));
        scriptsByLocale.put("man", getScriptsMap("", LATIN, "GN", "Nkoo"));
        scriptsByLocale.put("mas", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("maz", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("mdf", CYRILLIC_SCRIPT_MAP);
        scriptsByLocale.put("mdh", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("mdr", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("mdt", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("men", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("mer", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("mfa", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("mfe", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("mg", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("mgh", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("mgp", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("mgy", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("mh", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("mi", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("mic", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("min", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("mk", CYRILLIC_SCRIPT_MAP);
        scriptsByLocale.put("ml", getScriptsMap("", "Mlym"));
        scriptsByLocale.put("mn", getScriptsMap("", CYRILLIC, "CN", "Mong"));
        scriptsByLocale.put("mnc", getScriptsMap("", "Mong"));
        scriptsByLocale.put("mni", getScriptsMap("", "Beng", "IN", "Mtei"));
        scriptsByLocale.put("mns", CYRILLIC_SCRIPT_MAP);
        scriptsByLocale.put("mnw", getScriptsMap("", "Mymr"));
        scriptsByLocale.put("moe", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("moh", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("mos", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("mr", DEVANAGARI_SCRIPT_MAP);
        scriptsByLocale.put("mrd", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("mrj", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("ms", getScriptsMap("", ARABIC, "MY", LATIN, "SG", LATIN));
        scriptsByLocale.put("mt", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("mtr", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("mua", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("mus", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("mvy", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("mwk", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("mwl", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("mwr", DEVANAGARI_SCRIPT_MAP);
        scriptsByLocale.put("mxc", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("my", getScriptsMap("", "Mymr"));
        scriptsByLocale.put("myv", CYRILLIC_SCRIPT_MAP);
        scriptsByLocale.put("myx", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("myz", getScriptsMap("", "Mand"));
        scriptsByLocale.put("na", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("nap", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("naq", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("nb", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("nbf", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("nch", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("nd", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("ndc", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("nds", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("ne", DEVANAGARI_SCRIPT_MAP);
        scriptsByLocale.put("new", DEVANAGARI_SCRIPT_MAP);
        scriptsByLocale.put("ng", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("ngl", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("nhe", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("nhw", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("nia", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("nij", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("niu", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("nl", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("nmg", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("nn", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("nnh", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("nod", getScriptsMap("", "Lana"));
        scriptsByLocale.put("noe", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("nog", CYRILLIC_SCRIPT_MAP);
        scriptsByLocale.put("nqo", getScriptsMap("", "Nkoo"));
        scriptsByLocale.put("nr", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("nsk", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("nso", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("nus", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("nv", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("ny", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("nym", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("nyn", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("nyo", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("nzi", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("oc", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("oj", getScriptsMap("", "Cans"));
        scriptsByLocale.put("om", getScriptsMap("", LATIN, "ET", "Ethi"));
        scriptsByLocale.put("or", getScriptsMap("", "Orya"));
        scriptsByLocale.put("os", CYRILLIC_SCRIPT_MAP);
        scriptsByLocale.put("osa", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("osc", getScriptsMap("", "Ital"));
        scriptsByLocale.put("otk", getScriptsMap("", "Orkh"));
        scriptsByLocale.put("pa", getScriptsMap("", "Guru", "PK", ARABIC));
        scriptsByLocale.put("pag", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("pal", getScriptsMap("", "Phli"));
        scriptsByLocale.put("pam", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("pap", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("pau", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("peo", getScriptsMap("", "Xpeo"));
        scriptsByLocale.put("phn", getScriptsMap("", "Phnx"));
        scriptsByLocale.put("pi", DEVANAGARI_SCRIPT_MAP);
        scriptsByLocale.put("pko", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("pl", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("pon", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("pra", getScriptsMap("", "Brah"));
        scriptsByLocale.put("prd", ARABIC_SCRIPT_MAP);
        scriptsByLocale.put("prg", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("prs", ARABIC_SCRIPT_MAP);
        scriptsByLocale.put("ps", ARABIC_SCRIPT_MAP);
        scriptsByLocale.put("pt", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("puu", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("qu", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("raj", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("rap", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("rar", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("rcf", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("rej", getScriptsMap("", LATIN, "ID", "Rjng"));
        scriptsByLocale.put("ria", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("rif", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("rjs", DEVANAGARI_SCRIPT_MAP);
        scriptsByLocale.put("rkt", BENG_SCRIPT_MAP);
        scriptsByLocale.put("rm", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("rmf", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("rmo", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("rmt", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("rn", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("rng", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("ro", getScriptsMap("", LATIN, "RS", CYRILLIC));
        scriptsByLocale.put("rob", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("rof", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("rom", CYRILLIC_SCRIPT_MAP);
        scriptsByLocale.put("ru", CYRILLIC_SCRIPT_MAP);
        scriptsByLocale.put("rue", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("rup", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("rw", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("rwk", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("ryu", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("sa", DEVANAGARI_SCRIPT_MAP);
        scriptsByLocale.put("sad", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("saf", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("sah", CYRILLIC_SCRIPT_MAP);
        scriptsByLocale.put("sam", HEBREW_SCRIPT_MAP);
        scriptsByLocale.put("saq", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("sas", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("sat", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("saz", getScriptsMap("", "Saur"));
        scriptsByLocale.put("sbp", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("sc", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("sck", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("scn", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("sco", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("scs", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("sd", getScriptsMap("", ARABIC, "IN", DEVANAGARI));
        scriptsByLocale.put("sdh", ARABIC_SCRIPT_MAP);
        scriptsByLocale.put("se", getScriptsMap("", LATIN, "NO", CYRILLIC));
        scriptsByLocale.put("see", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("sef", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("seh", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("sel", CYRILLIC_SCRIPT_MAP);
        scriptsByLocale.put("ses", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("sg", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("sga", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("shi", getScriptsMap("", "Tfng"));
        scriptsByLocale.put("shn", getScriptsMap("", "Mymr"));
        scriptsByLocale.put("si", getScriptsMap("", "Sinh"));
        scriptsByLocale.put("sid", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("sk", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("skr", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("sl", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("sm", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("sma", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("smi", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("smj", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("smn", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("sms", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("sn", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("snk", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("so", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("son", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("sou", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("sq", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("sr", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("srn", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("srr", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("srx", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("ss", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("ssy", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("st", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("su", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("suk", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("sus", getScriptsMap("", LATIN, "GN", ARABIC));
        scriptsByLocale.put("sv", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("sw", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("swb", getScriptsMap("", ARABIC, "YT", LATIN));
        scriptsByLocale.put("swc", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("swv", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("sxn", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("syi", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("syl", getScriptsMap("", "Beng", "BD", "Sylo"));
        scriptsByLocale.put("syr", getScriptsMap("", "Syrc"));
        scriptsByLocale.put("ta", getScriptsMap("", "Taml"));
        scriptsByLocale.put("tab", CYRILLIC_SCRIPT_MAP);
        scriptsByLocale.put("taj", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("tbw", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("tcy", getScriptsMap("", "Knda"));
        scriptsByLocale.put("tdd", getScriptsMap("", "Tale"));
        scriptsByLocale.put("tdg", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("tdh", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("te", getScriptsMap("", "Telu"));
        scriptsByLocale.put("tem", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("teo", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("ter", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("tet", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("tg", getScriptsMap("", CYRILLIC, "PK", ARABIC));
        scriptsByLocale.put("th", THAI_SCRIPT_MAP);
        scriptsByLocale.put("thl", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("thq", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("thr", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("ti", ETHI_SCRIPT_MAP);
        scriptsByLocale.put("tig", ETHI_SCRIPT_MAP);
        scriptsByLocale.put("tiv", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("tk", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("tkl", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("tkt", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("tli", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("tmh", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("tn", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("to", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("tog", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("tpi", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("tr", getScriptsMap("", LATIN, "DE", ARABIC, "MK", ARABIC));
        scriptsByLocale.put("tru", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("trv", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("ts", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("tsf", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("tsg", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("tsi", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("tsj", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("tt", CYRILLIC_SCRIPT_MAP);
        scriptsByLocale.put("ttj", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("tts", THAI_SCRIPT_MAP);
        scriptsByLocale.put("tum", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("tut", CYRILLIC_SCRIPT_MAP);
        scriptsByLocale.put("tvl", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("twq", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("ty", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("tyv", CYRILLIC_SCRIPT_MAP);
        scriptsByLocale.put("tzm", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("ude", CYRILLIC_SCRIPT_MAP);
        scriptsByLocale.put("udm", getScriptsMap("", CYRILLIC, "RU", LATIN));
        scriptsByLocale.put("ug", getScriptsMap("", ARABIC, "KZ", CYRILLIC, "MN", CYRILLIC));
        scriptsByLocale.put("uga", getScriptsMap("", "Ugar"));
        scriptsByLocale.put("uk", CYRILLIC_SCRIPT_MAP);
        scriptsByLocale.put("uli", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("umb", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("und", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("unr", getScriptsMap("", "Beng", "NP", DEVANAGARI));
        scriptsByLocale.put("unx", BENG_SCRIPT_MAP);
        scriptsByLocale.put("ur", ARABIC_SCRIPT_MAP);
        scriptsByLocale.put("uz", getScriptsMap("", LATIN, "AF", ARABIC, "CN", CYRILLIC));
        scriptsByLocale.put("vai", getScriptsMap("", "Vaii"));
        scriptsByLocale.put("ve", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("vi", getScriptsMap("", LATIN, "US", "Hani"));
        scriptsByLocale.put("vic", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("vmw", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("vo", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("vot", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("vun", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("wa", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("wae", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("wak", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("wal", ETHI_SCRIPT_MAP);
        scriptsByLocale.put("war", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("was", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("wbq", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("wbr", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("wls", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("wo", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("wtm", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("xal", CYRILLIC_SCRIPT_MAP);
        scriptsByLocale.put("xav", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("xcr", getScriptsMap("", "Cari"));
        scriptsByLocale.put("xh", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("xnr", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("xog", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("xpr", getScriptsMap("", "Prti"));
        scriptsByLocale.put("xsa", getScriptsMap("", "Sarb"));
        scriptsByLocale.put("xsr", DEVANAGARI_SCRIPT_MAP);
        scriptsByLocale.put("xum", getScriptsMap("", "Ital"));
        scriptsByLocale.put("yao", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("yap", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("yav", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("ybb", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("yi", HEBREW_SCRIPT_MAP);
        scriptsByLocale.put("yo", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("yrk", CYRILLIC_SCRIPT_MAP);
        scriptsByLocale.put("yua", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("yue", getScriptsMap("", "Hans"));
        scriptsByLocale.put("za", getScriptsMap("", LATIN, "CN", "Hans"));
        scriptsByLocale.put("zap", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("zdj", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("zea", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("zen", getScriptsMap("", "Tfng"));
        scriptsByLocale.put("zh", getScriptsMap("", "Hant", "CN", "Hans", "HK", "Hans", "MO", "Hans", "SG", "Hans", "MN", "Hans"));
        scriptsByLocale.put("zmi", EMPTY_SCRIPT_MAP);
        scriptsByLocale.put("zu", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("zun", LATIN_SCRIPT_MAP);
        scriptsByLocale.put("zza", ARABIC_SCRIPT_MAP);
    }

    /**
     * Private constructor to prevent instantiation
     */
    private LocaleUtils() {
        // do nothing
    }

    /**
     * Gets the script for the given locale. For example, if a US citizen uses German Locale, and calls this method with
     * Locale.getDefault(), the result would be "Runr"
     * 
     * @param locale
     * @return a String indicating the script or null
     */
    @Nullable
    private static String getScript(@NonNull Locale locale) {
        String localeString = locale.toString();
        String language = "";
        String country = "";
        if (localeString.contains("_")) {
            String[] split = localeString.split("_");
            language = split[0];
            country = split[1];
        } else {
            language = localeString;
        }
        Map<String, String> scripts = scriptsByLocale.get(language);
        return scripts != null ? scripts.get(country) : null;
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
    @NonNull
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
        return LATIN.equals(getScript(locale));
    }

    /**
     * Determine if a string is likely a valid language
     * 
     * @param input the input string
     * @return true if the string is likely a language
     */
    public static boolean isLanguage(@NonNull String input) {
        if (input.contains("_") || input.contains("-")) {
            String[] split = input.split("[_,\\-]");
            String language = split[0];
            // NOSONAR String country = split[1];
            // for now just check the language
            return scriptsByLocale.containsKey(language);
        }
        return scriptsByLocale.containsKey(input);
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
