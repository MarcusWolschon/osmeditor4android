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

    private static final String GREEK      = "Grek";
    private static final String RUNIC      = "Runr";
    private static final String GOTHIC     = "Goth";
    private static final String ARMENIAN   = "Armn";
    private static final String THAI       = "Thai";
    private static final String HEBREW     = "Hebr";
    private static final String DEVANAGARI = "Deva";
    private static final String ARABIC     = "Arab";
    private static final String KANA       = "Kana";
    private static final String AVESTAN    = "Avst";
    private static final String CYRILLIC   = "Cyrl";
    private static final String LATIN      = "Latn";

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
        scriptsByLocale.put("aa", getScriptsMap("", LATIN));
        scriptsByLocale.put("ab", getScriptsMap("", CYRILLIC));
        scriptsByLocale.put("abq", getScriptsMap("", CYRILLIC));
        scriptsByLocale.put("abr", getScriptsMap("", ""));
        scriptsByLocale.put("ace", getScriptsMap("", LATIN));
        scriptsByLocale.put("ach", getScriptsMap("", LATIN));
        scriptsByLocale.put("ada", getScriptsMap("", LATIN));
        scriptsByLocale.put("ady", getScriptsMap("", CYRILLIC));
        scriptsByLocale.put("ae", getScriptsMap("", AVESTAN));
        scriptsByLocale.put("af", getScriptsMap("", LATIN));
        scriptsByLocale.put("agq", getScriptsMap("", LATIN));
        scriptsByLocale.put("aii", getScriptsMap("", CYRILLIC));
        scriptsByLocale.put("ain", getScriptsMap("", KANA));
        scriptsByLocale.put("ak", getScriptsMap("", LATIN));
        scriptsByLocale.put("akk", getScriptsMap("", "Xsux"));
        scriptsByLocale.put("ale", getScriptsMap("", LATIN));
        scriptsByLocale.put("alt", getScriptsMap("", CYRILLIC));
        scriptsByLocale.put("am", getScriptsMap("", "Ethi"));
        scriptsByLocale.put("amo", getScriptsMap("", LATIN));
        scriptsByLocale.put("an", getScriptsMap("", LATIN));
        scriptsByLocale.put("anp", getScriptsMap("", DEVANAGARI));
        scriptsByLocale.put("aoz", getScriptsMap("", ""));
        scriptsByLocale.put("ar", getScriptsMap("", ARABIC, "IR", "Syrc"));
        scriptsByLocale.put("arc", getScriptsMap("", "Armi"));
        scriptsByLocale.put("arn", getScriptsMap("", LATIN));
        scriptsByLocale.put("arp", getScriptsMap("", LATIN));
        scriptsByLocale.put("arw", getScriptsMap("", LATIN));
        scriptsByLocale.put("as", getScriptsMap("", "Beng"));
        scriptsByLocale.put("asa", getScriptsMap("", LATIN));
        scriptsByLocale.put("ast", getScriptsMap("", LATIN));
        scriptsByLocale.put("atj", getScriptsMap("", ""));
        scriptsByLocale.put("av", getScriptsMap("", CYRILLIC));
        scriptsByLocale.put("awa", getScriptsMap("", DEVANAGARI));
        scriptsByLocale.put("ay", getScriptsMap("", LATIN));
        scriptsByLocale.put("az", getScriptsMap("", LATIN, "AZ", CYRILLIC, "IR", ARABIC));
        scriptsByLocale.put("ba", getScriptsMap("", CYRILLIC));
        scriptsByLocale.put("bal", getScriptsMap("", ARABIC, "IR", LATIN, "PK", LATIN));
        scriptsByLocale.put("ban", getScriptsMap("", LATIN, "ID", "Bali"));
        scriptsByLocale.put("bap", getScriptsMap("", ""));
        scriptsByLocale.put("bas", getScriptsMap("", LATIN));
        scriptsByLocale.put("bax", getScriptsMap("", "Bamu"));
        scriptsByLocale.put("bbc", getScriptsMap("", LATIN, "ID", "Batk"));
        scriptsByLocale.put("bbj", getScriptsMap("", ""));
        scriptsByLocale.put("bci", getScriptsMap("", ""));
        scriptsByLocale.put("be", getScriptsMap("", CYRILLIC));
        scriptsByLocale.put("bej", getScriptsMap("", ARABIC));
        scriptsByLocale.put("bem", getScriptsMap("", LATIN));
        scriptsByLocale.put("bew", getScriptsMap("", ""));
        scriptsByLocale.put("bez", getScriptsMap("", LATIN));
        scriptsByLocale.put("bfd", getScriptsMap("", ""));
        scriptsByLocale.put("bfq", getScriptsMap("", "Taml"));
        scriptsByLocale.put("bft", getScriptsMap("", ARABIC));
        scriptsByLocale.put("bfy", getScriptsMap("", DEVANAGARI));
        scriptsByLocale.put("bg", getScriptsMap("", CYRILLIC));
        scriptsByLocale.put("bgc", getScriptsMap("", ""));
        scriptsByLocale.put("bgx", getScriptsMap("", ""));
        scriptsByLocale.put("bh", getScriptsMap("", DEVANAGARI));
        scriptsByLocale.put("bhb", getScriptsMap("", DEVANAGARI));
        scriptsByLocale.put("bhi", getScriptsMap("", ""));
        scriptsByLocale.put("bhk", getScriptsMap("", ""));
        scriptsByLocale.put("bho", getScriptsMap("", DEVANAGARI));
        scriptsByLocale.put("bi", getScriptsMap("", LATIN));
        scriptsByLocale.put("bik", getScriptsMap("", LATIN));
        scriptsByLocale.put("bin", getScriptsMap("", LATIN));
        scriptsByLocale.put("bjj", getScriptsMap("", DEVANAGARI));
        scriptsByLocale.put("bjn", getScriptsMap("", ""));
        scriptsByLocale.put("bkm", getScriptsMap("", ""));
        scriptsByLocale.put("bku", getScriptsMap("", LATIN));
        scriptsByLocale.put("bla", getScriptsMap("", LATIN));
        scriptsByLocale.put("blt", getScriptsMap("", "Tavt"));
        scriptsByLocale.put("bm", getScriptsMap("", LATIN));
        scriptsByLocale.put("bmq", getScriptsMap("", ""));
        scriptsByLocale.put("bn", getScriptsMap("", "Beng"));
        scriptsByLocale.put("bo", getScriptsMap("", "Tibt"));
        scriptsByLocale.put("bqi", getScriptsMap("", ""));
        scriptsByLocale.put("bqv", getScriptsMap("", LATIN));
        scriptsByLocale.put("br", getScriptsMap("", LATIN));
        scriptsByLocale.put("bra", getScriptsMap("", DEVANAGARI));
        scriptsByLocale.put("brh", getScriptsMap("", ""));
        scriptsByLocale.put("brx", getScriptsMap("", DEVANAGARI));
        scriptsByLocale.put("bs", getScriptsMap("", LATIN));
        scriptsByLocale.put("bss", getScriptsMap("", ""));
        scriptsByLocale.put("bto", getScriptsMap("", ""));
        scriptsByLocale.put("btv", getScriptsMap("", DEVANAGARI));
        scriptsByLocale.put("bua", getScriptsMap("", CYRILLIC));
        scriptsByLocale.put("buc", getScriptsMap("", LATIN));
        scriptsByLocale.put("bug", getScriptsMap("", LATIN, "ID", "Bugi"));
        scriptsByLocale.put("bum", getScriptsMap("", ""));
        scriptsByLocale.put("bvb", getScriptsMap("", ""));
        scriptsByLocale.put("bya", getScriptsMap("", LATIN));
        scriptsByLocale.put("byn", getScriptsMap("", "Ethi"));
        scriptsByLocale.put("byv", getScriptsMap("", ""));
        scriptsByLocale.put("bze", getScriptsMap("", ""));
        scriptsByLocale.put("bzx", getScriptsMap("", ""));
        scriptsByLocale.put("ca", getScriptsMap("", LATIN));
        scriptsByLocale.put("cad", getScriptsMap("", LATIN));
        scriptsByLocale.put("car", getScriptsMap("", LATIN));
        scriptsByLocale.put("cay", getScriptsMap("", LATIN));
        scriptsByLocale.put("cch", getScriptsMap("", LATIN));
        scriptsByLocale.put("ccp", getScriptsMap("", "Beng"));
        scriptsByLocale.put("ce", getScriptsMap("", CYRILLIC));
        scriptsByLocale.put("ceb", getScriptsMap("", LATIN));
        scriptsByLocale.put("cgg", getScriptsMap("", LATIN));
        scriptsByLocale.put("ch", getScriptsMap("", LATIN));
        scriptsByLocale.put("chk", getScriptsMap("", LATIN));
        scriptsByLocale.put("chm", getScriptsMap("", CYRILLIC));
        scriptsByLocale.put("chn", getScriptsMap("", LATIN));
        scriptsByLocale.put("cho", getScriptsMap("", LATIN));
        scriptsByLocale.put("chp", getScriptsMap("", LATIN));
        scriptsByLocale.put("chr", getScriptsMap("", "Cher"));
        scriptsByLocale.put("chy", getScriptsMap("", LATIN));
        scriptsByLocale.put("cja", getScriptsMap("", ARABIC));
        scriptsByLocale.put("cjm", getScriptsMap("", "Cham"));
        scriptsByLocale.put("cjs", getScriptsMap("", CYRILLIC));
        scriptsByLocale.put("ckb", getScriptsMap("", ARABIC));
        scriptsByLocale.put("ckt", getScriptsMap("", CYRILLIC));
        scriptsByLocale.put("co", getScriptsMap("", LATIN));
        scriptsByLocale.put("cop", getScriptsMap("", ARABIC));
        scriptsByLocale.put("cpe", getScriptsMap("", LATIN));
        scriptsByLocale.put("cr", getScriptsMap("", "Cans"));
        scriptsByLocale.put("crh", getScriptsMap("", CYRILLIC));
        scriptsByLocale.put("crj", getScriptsMap("", ""));
        scriptsByLocale.put("crk", getScriptsMap("", "Cans"));
        scriptsByLocale.put("crl", getScriptsMap("", ""));
        scriptsByLocale.put("crm", getScriptsMap("", ""));
        scriptsByLocale.put("crs", getScriptsMap("", ""));
        scriptsByLocale.put("cs", getScriptsMap("", LATIN));
        scriptsByLocale.put("csb", getScriptsMap("", LATIN));
        scriptsByLocale.put("csw", getScriptsMap("", ""));
        scriptsByLocale.put("cu", getScriptsMap("", "Glag"));
        scriptsByLocale.put("cv", getScriptsMap("", CYRILLIC));
        scriptsByLocale.put("cy", getScriptsMap("", LATIN));
        scriptsByLocale.put("da", getScriptsMap("", LATIN));
        scriptsByLocale.put("daf", getScriptsMap("", ""));
        scriptsByLocale.put("dak", getScriptsMap("", LATIN));
        scriptsByLocale.put("dar", getScriptsMap("", CYRILLIC));
        scriptsByLocale.put("dav", getScriptsMap("", LATIN));
        scriptsByLocale.put("dcc", getScriptsMap("", ""));
        scriptsByLocale.put("de", getScriptsMap("", LATIN, "AT", LATIN, "CH", LATIN, "BR", RUNIC, "KZ", RUNIC, "US", RUNIC));
        scriptsByLocale.put("del", getScriptsMap("", LATIN));
        scriptsByLocale.put("den", getScriptsMap("", LATIN));
        scriptsByLocale.put("dgr", getScriptsMap("", LATIN));
        scriptsByLocale.put("din", getScriptsMap("", LATIN));
        scriptsByLocale.put("dje", getScriptsMap("", LATIN));
        scriptsByLocale.put("dng", getScriptsMap("", CYRILLIC));
        scriptsByLocale.put("doi", getScriptsMap("", ARABIC));
        scriptsByLocale.put("dsb", getScriptsMap("", LATIN));
        scriptsByLocale.put("dtm", getScriptsMap("", ""));
        scriptsByLocale.put("dua", getScriptsMap("", LATIN));
        scriptsByLocale.put("dv", getScriptsMap("", "Thaa"));
        scriptsByLocale.put("dyo", getScriptsMap("", ARABIC));
        scriptsByLocale.put("dyu", getScriptsMap("", LATIN));
        scriptsByLocale.put("dz", getScriptsMap("", "Tibt"));
        scriptsByLocale.put("ebu", getScriptsMap("", LATIN));
        scriptsByLocale.put("ee", getScriptsMap("", LATIN));
        scriptsByLocale.put("efi", getScriptsMap("", LATIN));
        scriptsByLocale.put("egy", getScriptsMap("", "Egyp"));
        scriptsByLocale.put("eka", getScriptsMap("", LATIN));
        scriptsByLocale.put("eky", getScriptsMap("", "Kali"));
        scriptsByLocale.put("el", getScriptsMap("", GREEK));
        scriptsByLocale.put("en", getScriptsMap("", LATIN));
        scriptsByLocale.put("eo", getScriptsMap("", LATIN));
        scriptsByLocale.put("es", getScriptsMap("", LATIN));
        scriptsByLocale.put("et", getScriptsMap("", LATIN));
        scriptsByLocale.put("ett", getScriptsMap("", "Ital"));
        scriptsByLocale.put("eu", getScriptsMap("", LATIN));
        scriptsByLocale.put("evn", getScriptsMap("", CYRILLIC));
        scriptsByLocale.put("ewo", getScriptsMap("", LATIN));
        scriptsByLocale.put("fa", getScriptsMap("", ARABIC));
        scriptsByLocale.put("fan", getScriptsMap("", LATIN));
        scriptsByLocale.put("ff", getScriptsMap("", LATIN));
        scriptsByLocale.put("ffm", getScriptsMap("", ""));
        scriptsByLocale.put("fi", getScriptsMap("", LATIN));
        scriptsByLocale.put("fil", getScriptsMap("", LATIN, "US", "Tglg"));
        scriptsByLocale.put("fiu", getScriptsMap("", LATIN));
        scriptsByLocale.put("fj", getScriptsMap("", LATIN));
        scriptsByLocale.put("fo", getScriptsMap("", LATIN));
        scriptsByLocale.put("fon", getScriptsMap("", LATIN));
        scriptsByLocale.put("fr", getScriptsMap("", LATIN));
        scriptsByLocale.put("frr", getScriptsMap("", LATIN));
        scriptsByLocale.put("frs", getScriptsMap("", LATIN));
        scriptsByLocale.put("fud", getScriptsMap("", ""));
        scriptsByLocale.put("fuq", getScriptsMap("", ""));
        scriptsByLocale.put("fur", getScriptsMap("", LATIN));
        scriptsByLocale.put("fuv", getScriptsMap("", ""));
        scriptsByLocale.put("fy", getScriptsMap("", LATIN));
        scriptsByLocale.put("ga", getScriptsMap("", LATIN));
        scriptsByLocale.put("gaa", getScriptsMap("", LATIN));
        scriptsByLocale.put("gag", getScriptsMap("", LATIN, "MD", CYRILLIC));
        scriptsByLocale.put("gay", getScriptsMap("", LATIN));
        scriptsByLocale.put("gba", getScriptsMap("", ARABIC));
        scriptsByLocale.put("gbm", getScriptsMap("", DEVANAGARI));
        scriptsByLocale.put("gcr", getScriptsMap("", LATIN));
        scriptsByLocale.put("gd", getScriptsMap("", LATIN));
        scriptsByLocale.put("gez", getScriptsMap("", "Ethi"));
        scriptsByLocale.put("ggn", getScriptsMap("", ""));
        scriptsByLocale.put("gil", getScriptsMap("", LATIN));
        scriptsByLocale.put("gjk", getScriptsMap("", ""));
        scriptsByLocale.put("gju", getScriptsMap("", ""));
        scriptsByLocale.put("gl", getScriptsMap("", LATIN));
        scriptsByLocale.put("gld", getScriptsMap("", CYRILLIC));
        scriptsByLocale.put("glk", getScriptsMap("", ""));
        scriptsByLocale.put("gn", getScriptsMap("", LATIN));
        scriptsByLocale.put("gon", getScriptsMap("", "Telu"));
        scriptsByLocale.put("gor", getScriptsMap("", LATIN));
        scriptsByLocale.put("gos", getScriptsMap("", ""));
        scriptsByLocale.put("got", getScriptsMap("", GOTHIC));
        scriptsByLocale.put("grb", getScriptsMap("", LATIN));
        scriptsByLocale.put("grc", getScriptsMap("", "Cprt"));
        scriptsByLocale.put("grt", getScriptsMap("", "Beng"));
        scriptsByLocale.put("gsw", getScriptsMap("", LATIN));
        scriptsByLocale.put("gu", getScriptsMap("", "Gujr"));
        scriptsByLocale.put("gub", getScriptsMap("", ""));
        scriptsByLocale.put("guz", getScriptsMap("", LATIN));
        scriptsByLocale.put("gv", getScriptsMap("", LATIN));
        scriptsByLocale.put("gvr", getScriptsMap("", ""));
        scriptsByLocale.put("gwi", getScriptsMap("", LATIN));
        scriptsByLocale.put("ha", getScriptsMap("", ARABIC, "NE", LATIN, "GH", LATIN));
        scriptsByLocale.put("hai", getScriptsMap("", LATIN));
        scriptsByLocale.put("haw", getScriptsMap("", LATIN));
        scriptsByLocale.put("haz", getScriptsMap("", ""));
        scriptsByLocale.put("he", getScriptsMap("", HEBREW));
        scriptsByLocale.put("hi", getScriptsMap("", DEVANAGARI));
        scriptsByLocale.put("hil", getScriptsMap("", LATIN));
        scriptsByLocale.put("hit", getScriptsMap("", "Xsux"));
        scriptsByLocale.put("hmn", getScriptsMap("", LATIN));
        scriptsByLocale.put("hnd", getScriptsMap("", ""));
        scriptsByLocale.put("hne", getScriptsMap("", DEVANAGARI));
        scriptsByLocale.put("hnn", getScriptsMap("", LATIN));
        scriptsByLocale.put("hno", getScriptsMap("", ""));
        scriptsByLocale.put("ho", getScriptsMap("", LATIN));
        scriptsByLocale.put("hoc", getScriptsMap("", DEVANAGARI));
        scriptsByLocale.put("hoj", getScriptsMap("", DEVANAGARI));
        scriptsByLocale.put("hop", getScriptsMap("", LATIN));
        scriptsByLocale.put("hr", getScriptsMap("", LATIN));
        scriptsByLocale.put("hsb", getScriptsMap("", LATIN));
        scriptsByLocale.put("ht", getScriptsMap("", LATIN));
        scriptsByLocale.put("hu", getScriptsMap("", LATIN));
        scriptsByLocale.put("hup", getScriptsMap("", LATIN));
        scriptsByLocale.put("hy", getScriptsMap("", ARMENIAN));
        scriptsByLocale.put("hz", getScriptsMap("", LATIN));
        scriptsByLocale.put("ia", getScriptsMap("", LATIN));
        scriptsByLocale.put("iba", getScriptsMap("", LATIN));
        scriptsByLocale.put("ibb", getScriptsMap("", LATIN));
        scriptsByLocale.put("id", getScriptsMap("", LATIN));
        scriptsByLocale.put("ig", getScriptsMap("", LATIN));
        scriptsByLocale.put("ii", getScriptsMap("", "Yiii", "CN", LATIN));
        scriptsByLocale.put("ik", getScriptsMap("", LATIN));
        scriptsByLocale.put("ikt", getScriptsMap("", ""));
        scriptsByLocale.put("ilo", getScriptsMap("", LATIN));
        scriptsByLocale.put("inh", getScriptsMap("", CYRILLIC));
        scriptsByLocale.put("is", getScriptsMap("", LATIN));
        scriptsByLocale.put("it", getScriptsMap("", LATIN));
        scriptsByLocale.put("iu", getScriptsMap("", "Cans", "CA", LATIN));
        scriptsByLocale.put("ja", getScriptsMap("", "Jpan"));
        scriptsByLocale.put("jmc", getScriptsMap("", LATIN));
        scriptsByLocale.put("jml", getScriptsMap("", ""));
        scriptsByLocale.put("jpr", getScriptsMap("", HEBREW));
        scriptsByLocale.put("jrb", getScriptsMap("", HEBREW));
        scriptsByLocale.put("jv", getScriptsMap("", LATIN, "ID", "Java"));
        scriptsByLocale.put("ka", getScriptsMap("", "Geor"));
        scriptsByLocale.put("kaa", getScriptsMap("", CYRILLIC));
        scriptsByLocale.put("kab", getScriptsMap("", LATIN));
        scriptsByLocale.put("kac", getScriptsMap("", LATIN));
        scriptsByLocale.put("kaj", getScriptsMap("", LATIN));
        scriptsByLocale.put("kam", getScriptsMap("", LATIN));
        scriptsByLocale.put("kao", getScriptsMap("", ""));
        scriptsByLocale.put("kbd", getScriptsMap("", CYRILLIC));
        scriptsByLocale.put("kca", getScriptsMap("", CYRILLIC));
        scriptsByLocale.put("kcg", getScriptsMap("", LATIN));
        scriptsByLocale.put("kck", getScriptsMap("", ""));
        scriptsByLocale.put("kde", getScriptsMap("", LATIN));
        scriptsByLocale.put("kdt", getScriptsMap("", THAI));
        scriptsByLocale.put("kea", getScriptsMap("", LATIN));
        scriptsByLocale.put("kfo", getScriptsMap("", LATIN));
        scriptsByLocale.put("kfr", getScriptsMap("", DEVANAGARI));
        scriptsByLocale.put("kfy", getScriptsMap("", ""));
        scriptsByLocale.put("kg", getScriptsMap("", LATIN));
        scriptsByLocale.put("kge", getScriptsMap("", ""));
        scriptsByLocale.put("kgp", getScriptsMap("", ""));
        scriptsByLocale.put("kha", getScriptsMap("", LATIN, "IN", "Beng"));
        scriptsByLocale.put("khb", getScriptsMap("", "Talu"));
        scriptsByLocale.put("khn", getScriptsMap("", ""));
        scriptsByLocale.put("khq", getScriptsMap("", LATIN));
        scriptsByLocale.put("kht", getScriptsMap("", "Mymr"));
        scriptsByLocale.put("khw", getScriptsMap("", ""));
        scriptsByLocale.put("ki", getScriptsMap("", LATIN));
        scriptsByLocale.put("kj", getScriptsMap("", LATIN));
        scriptsByLocale.put("kjg", getScriptsMap("", ""));
        scriptsByLocale.put("kjh", getScriptsMap("", CYRILLIC));
        scriptsByLocale.put("kk", getScriptsMap("", ARABIC, "KZ", CYRILLIC, "TR", CYRILLIC));
        scriptsByLocale.put("kkj", getScriptsMap("", ""));
        scriptsByLocale.put("kl", getScriptsMap("", LATIN));
        scriptsByLocale.put("kln", getScriptsMap("", LATIN));
        scriptsByLocale.put("km", getScriptsMap("", "Khmr"));
        scriptsByLocale.put("kmb", getScriptsMap("", LATIN));
        scriptsByLocale.put("kn", getScriptsMap("", "Knda"));
        scriptsByLocale.put("ko", getScriptsMap("", "Kore"));
        scriptsByLocale.put("koi", getScriptsMap("", CYRILLIC));
        scriptsByLocale.put("kok", getScriptsMap("", DEVANAGARI));
        scriptsByLocale.put("kos", getScriptsMap("", LATIN));
        scriptsByLocale.put("kpe", getScriptsMap("", LATIN));
        scriptsByLocale.put("kpy", getScriptsMap("", CYRILLIC));
        scriptsByLocale.put("kr", getScriptsMap("", LATIN));
        scriptsByLocale.put("krc", getScriptsMap("", CYRILLIC));
        scriptsByLocale.put("kri", getScriptsMap("", LATIN));
        scriptsByLocale.put("krl", getScriptsMap("", LATIN));
        scriptsByLocale.put("kru", getScriptsMap("", DEVANAGARI));
        scriptsByLocale.put("ks", getScriptsMap("", ARABIC));
        scriptsByLocale.put("ksb", getScriptsMap("", LATIN));
        scriptsByLocale.put("ksf", getScriptsMap("", LATIN));
        scriptsByLocale.put("ksh", getScriptsMap("", LATIN));
        scriptsByLocale.put("ku", getScriptsMap("", LATIN, "LB", ARABIC));
        scriptsByLocale.put("kum", getScriptsMap("", CYRILLIC));
        scriptsByLocale.put("kut", getScriptsMap("", LATIN));
        scriptsByLocale.put("kv", getScriptsMap("", CYRILLIC));
        scriptsByLocale.put("kvr", getScriptsMap("", ""));
        scriptsByLocale.put("kvx", getScriptsMap("", ""));
        scriptsByLocale.put("kw", getScriptsMap("", LATIN));
        scriptsByLocale.put("kxm", getScriptsMap("", ""));
        scriptsByLocale.put("kxp", getScriptsMap("", ""));
        scriptsByLocale.put("ky", getScriptsMap("", CYRILLIC, "CN", ARABIC, "TR", LATIN));
        scriptsByLocale.put("kyu", getScriptsMap("", "Kali"));
        scriptsByLocale.put("la", getScriptsMap("", LATIN));
        scriptsByLocale.put("lad", getScriptsMap("", HEBREW));
        scriptsByLocale.put("lag", getScriptsMap("", LATIN));
        scriptsByLocale.put("lah", getScriptsMap("", ARABIC));
        scriptsByLocale.put("laj", getScriptsMap("", ""));
        scriptsByLocale.put("lam", getScriptsMap("", LATIN));
        scriptsByLocale.put("lb", getScriptsMap("", LATIN));
        scriptsByLocale.put("lbe", getScriptsMap("", CYRILLIC));
        scriptsByLocale.put("lbw", getScriptsMap("", ""));
        scriptsByLocale.put("lcp", getScriptsMap("", THAI));
        scriptsByLocale.put("lep", getScriptsMap("", "Lepc"));
        scriptsByLocale.put("lez", getScriptsMap("", CYRILLIC));
        scriptsByLocale.put("lg", getScriptsMap("", LATIN));
        scriptsByLocale.put("li", getScriptsMap("", LATIN));
        scriptsByLocale.put("lif", getScriptsMap("", DEVANAGARI));
        scriptsByLocale.put("lis", getScriptsMap("", "Lisu"));
        scriptsByLocale.put("ljp", getScriptsMap("", ""));
        scriptsByLocale.put("lki", getScriptsMap("", ARABIC));
        scriptsByLocale.put("lkt", getScriptsMap("", ""));
        scriptsByLocale.put("lmn", getScriptsMap("", "Telu"));
        scriptsByLocale.put("lmo", getScriptsMap("", ""));
        scriptsByLocale.put("ln", getScriptsMap("", LATIN));
        scriptsByLocale.put("lo", getScriptsMap("", "Laoo"));
        scriptsByLocale.put("lol", getScriptsMap("", LATIN));
        scriptsByLocale.put("loz", getScriptsMap("", LATIN));
        scriptsByLocale.put("lrc", getScriptsMap("", ""));
        scriptsByLocale.put("lt", getScriptsMap("", LATIN));
        scriptsByLocale.put("lu", getScriptsMap("", LATIN));
        scriptsByLocale.put("lua", getScriptsMap("", LATIN));
        scriptsByLocale.put("lui", getScriptsMap("", LATIN));
        scriptsByLocale.put("lun", getScriptsMap("", LATIN));
        scriptsByLocale.put("luo", getScriptsMap("", LATIN));
        scriptsByLocale.put("lus", getScriptsMap("", "Beng"));
        scriptsByLocale.put("lut", getScriptsMap("", LATIN));
        scriptsByLocale.put("luy", getScriptsMap("", LATIN));
        scriptsByLocale.put("luz", getScriptsMap("", ""));
        scriptsByLocale.put("lv", getScriptsMap("", LATIN));
        scriptsByLocale.put("lwl", getScriptsMap("", THAI));
        scriptsByLocale.put("mad", getScriptsMap("", LATIN));
        scriptsByLocale.put("maf", getScriptsMap("", ""));
        scriptsByLocale.put("mag", getScriptsMap("", DEVANAGARI));
        scriptsByLocale.put("mai", getScriptsMap("", DEVANAGARI));
        scriptsByLocale.put("mak", getScriptsMap("", LATIN, "ID", "Bugi"));
        scriptsByLocale.put("man", getScriptsMap("", LATIN, "GN", "Nkoo"));
        scriptsByLocale.put("mas", getScriptsMap("", LATIN));
        scriptsByLocale.put("maz", getScriptsMap("", ""));
        scriptsByLocale.put("mdf", getScriptsMap("", CYRILLIC));
        scriptsByLocale.put("mdh", getScriptsMap("", LATIN));
        scriptsByLocale.put("mdr", getScriptsMap("", LATIN));
        scriptsByLocale.put("mdt", getScriptsMap("", ""));
        scriptsByLocale.put("men", getScriptsMap("", LATIN));
        scriptsByLocale.put("mer", getScriptsMap("", LATIN));
        scriptsByLocale.put("mfa", getScriptsMap("", ""));
        scriptsByLocale.put("mfe", getScriptsMap("", LATIN));
        scriptsByLocale.put("mg", getScriptsMap("", LATIN));
        scriptsByLocale.put("mgh", getScriptsMap("", LATIN));
        scriptsByLocale.put("mgp", getScriptsMap("", ""));
        scriptsByLocale.put("mgy", getScriptsMap("", ""));
        scriptsByLocale.put("mh", getScriptsMap("", LATIN));
        scriptsByLocale.put("mi", getScriptsMap("", LATIN));
        scriptsByLocale.put("mic", getScriptsMap("", LATIN));
        scriptsByLocale.put("min", getScriptsMap("", LATIN));
        scriptsByLocale.put("mk", getScriptsMap("", CYRILLIC));
        scriptsByLocale.put("ml", getScriptsMap("", "Mlym"));
        scriptsByLocale.put("mn", getScriptsMap("", CYRILLIC, "CN", "Mong"));
        scriptsByLocale.put("mnc", getScriptsMap("", "Mong"));
        scriptsByLocale.put("mni", getScriptsMap("", "Beng", "IN", "Mtei"));
        scriptsByLocale.put("mns", getScriptsMap("", CYRILLIC));
        scriptsByLocale.put("mnw", getScriptsMap("", "Mymr"));
        scriptsByLocale.put("moe", getScriptsMap("", ""));
        scriptsByLocale.put("moh", getScriptsMap("", LATIN));
        scriptsByLocale.put("mos", getScriptsMap("", LATIN));
        scriptsByLocale.put("mr", getScriptsMap("", DEVANAGARI));
        scriptsByLocale.put("mrd", getScriptsMap("", ""));
        scriptsByLocale.put("mrj", getScriptsMap("", ""));
        scriptsByLocale.put("ms", getScriptsMap("", ARABIC, "MY", LATIN, "SG", LATIN));
        scriptsByLocale.put("mt", getScriptsMap("", LATIN));
        scriptsByLocale.put("mtr", getScriptsMap("", ""));
        scriptsByLocale.put("mua", getScriptsMap("", LATIN));
        scriptsByLocale.put("mus", getScriptsMap("", LATIN));
        scriptsByLocale.put("mvy", getScriptsMap("", ""));
        scriptsByLocale.put("mwk", getScriptsMap("", ""));
        scriptsByLocale.put("mwl", getScriptsMap("", LATIN));
        scriptsByLocale.put("mwr", getScriptsMap("", DEVANAGARI));
        scriptsByLocale.put("mxc", getScriptsMap("", ""));
        scriptsByLocale.put("my", getScriptsMap("", "Mymr"));
        scriptsByLocale.put("myv", getScriptsMap("", CYRILLIC));
        scriptsByLocale.put("myx", getScriptsMap("", ""));
        scriptsByLocale.put("myz", getScriptsMap("", "Mand"));
        scriptsByLocale.put("na", getScriptsMap("", LATIN));
        scriptsByLocale.put("nap", getScriptsMap("", LATIN));
        scriptsByLocale.put("naq", getScriptsMap("", LATIN));
        scriptsByLocale.put("nb", getScriptsMap("", LATIN));
        scriptsByLocale.put("nbf", getScriptsMap("", ""));
        scriptsByLocale.put("nch", getScriptsMap("", ""));
        scriptsByLocale.put("nd", getScriptsMap("", LATIN));
        scriptsByLocale.put("ndc", getScriptsMap("", ""));
        scriptsByLocale.put("nds", getScriptsMap("", LATIN));
        scriptsByLocale.put("ne", getScriptsMap("", DEVANAGARI));
        scriptsByLocale.put("new", getScriptsMap("", DEVANAGARI));
        scriptsByLocale.put("ng", getScriptsMap("", LATIN));
        scriptsByLocale.put("ngl", getScriptsMap("", ""));
        scriptsByLocale.put("nhe", getScriptsMap("", ""));
        scriptsByLocale.put("nhw", getScriptsMap("", ""));
        scriptsByLocale.put("nia", getScriptsMap("", LATIN));
        scriptsByLocale.put("nij", getScriptsMap("", ""));
        scriptsByLocale.put("niu", getScriptsMap("", LATIN));
        scriptsByLocale.put("nl", getScriptsMap("", LATIN));
        scriptsByLocale.put("nmg", getScriptsMap("", LATIN));
        scriptsByLocale.put("nn", getScriptsMap("", LATIN));
        scriptsByLocale.put("nnh", getScriptsMap("", ""));
        scriptsByLocale.put("nod", getScriptsMap("", "Lana"));
        scriptsByLocale.put("noe", getScriptsMap("", ""));
        scriptsByLocale.put("nog", getScriptsMap("", CYRILLIC));
        scriptsByLocale.put("nqo", getScriptsMap("", "Nkoo"));
        scriptsByLocale.put("nr", getScriptsMap("", LATIN));
        scriptsByLocale.put("nsk", getScriptsMap("", ""));
        scriptsByLocale.put("nso", getScriptsMap("", LATIN));
        scriptsByLocale.put("nus", getScriptsMap("", LATIN));
        scriptsByLocale.put("nv", getScriptsMap("", LATIN));
        scriptsByLocale.put("ny", getScriptsMap("", LATIN));
        scriptsByLocale.put("nym", getScriptsMap("", LATIN));
        scriptsByLocale.put("nyn", getScriptsMap("", LATIN));
        scriptsByLocale.put("nyo", getScriptsMap("", LATIN));
        scriptsByLocale.put("nzi", getScriptsMap("", LATIN));
        scriptsByLocale.put("oc", getScriptsMap("", LATIN));
        scriptsByLocale.put("oj", getScriptsMap("", "Cans"));
        scriptsByLocale.put("om", getScriptsMap("", LATIN, "ET", "Ethi"));
        scriptsByLocale.put("or", getScriptsMap("", "Orya"));
        scriptsByLocale.put("os", getScriptsMap("", CYRILLIC));
        scriptsByLocale.put("osa", getScriptsMap("", LATIN));
        scriptsByLocale.put("osc", getScriptsMap("", "Ital"));
        scriptsByLocale.put("otk", getScriptsMap("", "Orkh"));
        scriptsByLocale.put("pa", getScriptsMap("", "Guru", "PK", ARABIC));
        scriptsByLocale.put("pag", getScriptsMap("", LATIN));
        scriptsByLocale.put("pal", getScriptsMap("", "Phli"));
        scriptsByLocale.put("pam", getScriptsMap("", LATIN));
        scriptsByLocale.put("pap", getScriptsMap("", LATIN));
        scriptsByLocale.put("pau", getScriptsMap("", LATIN));
        scriptsByLocale.put("peo", getScriptsMap("", "Xpeo"));
        scriptsByLocale.put("phn", getScriptsMap("", "Phnx"));
        scriptsByLocale.put("pi", getScriptsMap("", DEVANAGARI));
        scriptsByLocale.put("pko", getScriptsMap("", ""));
        scriptsByLocale.put("pl", getScriptsMap("", LATIN));
        scriptsByLocale.put("pon", getScriptsMap("", LATIN));
        scriptsByLocale.put("pra", getScriptsMap("", "Brah"));
        scriptsByLocale.put("prd", getScriptsMap("", ARABIC));
        scriptsByLocale.put("prg", getScriptsMap("", LATIN));
        scriptsByLocale.put("prs", getScriptsMap("", ARABIC));
        scriptsByLocale.put("ps", getScriptsMap("", ARABIC));
        scriptsByLocale.put("pt", getScriptsMap("", LATIN));
        scriptsByLocale.put("puu", getScriptsMap("", ""));
        scriptsByLocale.put("qu", getScriptsMap("", LATIN));
        scriptsByLocale.put("raj", getScriptsMap("", LATIN));
        scriptsByLocale.put("rap", getScriptsMap("", LATIN));
        scriptsByLocale.put("rar", getScriptsMap("", LATIN));
        scriptsByLocale.put("rcf", getScriptsMap("", LATIN));
        scriptsByLocale.put("rej", getScriptsMap("", LATIN, "ID", "Rjng"));
        scriptsByLocale.put("ria", getScriptsMap("", ""));
        scriptsByLocale.put("rif", getScriptsMap("", ""));
        scriptsByLocale.put("rjs", getScriptsMap("", DEVANAGARI));
        scriptsByLocale.put("rkt", getScriptsMap("", "Beng"));
        scriptsByLocale.put("rm", getScriptsMap("", LATIN));
        scriptsByLocale.put("rmf", getScriptsMap("", ""));
        scriptsByLocale.put("rmo", getScriptsMap("", ""));
        scriptsByLocale.put("rmt", getScriptsMap("", ""));
        scriptsByLocale.put("rn", getScriptsMap("", LATIN));
        scriptsByLocale.put("rng", getScriptsMap("", ""));
        scriptsByLocale.put("ro", getScriptsMap("", LATIN, "RS", CYRILLIC));
        scriptsByLocale.put("rob", getScriptsMap("", ""));
        scriptsByLocale.put("rof", getScriptsMap("", LATIN));
        scriptsByLocale.put("rom", getScriptsMap("", CYRILLIC));
        scriptsByLocale.put("ru", getScriptsMap("", CYRILLIC));
        scriptsByLocale.put("rue", getScriptsMap("", ""));
        scriptsByLocale.put("rup", getScriptsMap("", LATIN));
        scriptsByLocale.put("rw", getScriptsMap("", LATIN));
        scriptsByLocale.put("rwk", getScriptsMap("", LATIN));
        scriptsByLocale.put("ryu", getScriptsMap("", ""));
        scriptsByLocale.put("sa", getScriptsMap("", DEVANAGARI));
        scriptsByLocale.put("sad", getScriptsMap("", LATIN));
        scriptsByLocale.put("saf", getScriptsMap("", LATIN));
        scriptsByLocale.put("sah", getScriptsMap("", CYRILLIC));
        scriptsByLocale.put("sam", getScriptsMap("", HEBREW));
        scriptsByLocale.put("saq", getScriptsMap("", LATIN));
        scriptsByLocale.put("sas", getScriptsMap("", LATIN));
        scriptsByLocale.put("sat", getScriptsMap("", LATIN));
        scriptsByLocale.put("saz", getScriptsMap("", "Saur"));
        scriptsByLocale.put("sbp", getScriptsMap("", LATIN));
        scriptsByLocale.put("sc", getScriptsMap("", LATIN));
        scriptsByLocale.put("sck", getScriptsMap("", ""));
        scriptsByLocale.put("scn", getScriptsMap("", LATIN));
        scriptsByLocale.put("sco", getScriptsMap("", LATIN));
        scriptsByLocale.put("scs", getScriptsMap("", ""));
        scriptsByLocale.put("sd", getScriptsMap("", ARABIC, "IN", DEVANAGARI));
        scriptsByLocale.put("sdh", getScriptsMap("", ARABIC));
        scriptsByLocale.put("se", getScriptsMap("", LATIN, "NO", CYRILLIC));
        scriptsByLocale.put("see", getScriptsMap("", LATIN));
        scriptsByLocale.put("sef", getScriptsMap("", ""));
        scriptsByLocale.put("seh", getScriptsMap("", LATIN));
        scriptsByLocale.put("sel", getScriptsMap("", CYRILLIC));
        scriptsByLocale.put("ses", getScriptsMap("", LATIN));
        scriptsByLocale.put("sg", getScriptsMap("", LATIN));
        scriptsByLocale.put("sga", getScriptsMap("", LATIN));
        scriptsByLocale.put("shi", getScriptsMap("", "Tfng"));
        scriptsByLocale.put("shn", getScriptsMap("", "Mymr"));
        scriptsByLocale.put("si", getScriptsMap("", "Sinh"));
        scriptsByLocale.put("sid", getScriptsMap("", LATIN));
        scriptsByLocale.put("sk", getScriptsMap("", LATIN));
        scriptsByLocale.put("skr", getScriptsMap("", ""));
        scriptsByLocale.put("sl", getScriptsMap("", LATIN));
        scriptsByLocale.put("sm", getScriptsMap("", LATIN));
        scriptsByLocale.put("sma", getScriptsMap("", LATIN));
        scriptsByLocale.put("smi", getScriptsMap("", LATIN));
        scriptsByLocale.put("smj", getScriptsMap("", LATIN));
        scriptsByLocale.put("smn", getScriptsMap("", LATIN));
        scriptsByLocale.put("sms", getScriptsMap("", LATIN));
        scriptsByLocale.put("sn", getScriptsMap("", LATIN));
        scriptsByLocale.put("snk", getScriptsMap("", LATIN));
        scriptsByLocale.put("so", getScriptsMap("", LATIN));
        scriptsByLocale.put("son", getScriptsMap("", LATIN));
        scriptsByLocale.put("sou", getScriptsMap("", ""));
        scriptsByLocale.put("sq", getScriptsMap("", LATIN));
        scriptsByLocale.put("sr", getScriptsMap("", LATIN));
        scriptsByLocale.put("srn", getScriptsMap("", LATIN));
        scriptsByLocale.put("srr", getScriptsMap("", LATIN));
        scriptsByLocale.put("srx", getScriptsMap("", ""));
        scriptsByLocale.put("ss", getScriptsMap("", LATIN));
        scriptsByLocale.put("ssy", getScriptsMap("", LATIN));
        scriptsByLocale.put("st", getScriptsMap("", LATIN));
        scriptsByLocale.put("su", getScriptsMap("", LATIN));
        scriptsByLocale.put("suk", getScriptsMap("", LATIN));
        scriptsByLocale.put("sus", getScriptsMap("", LATIN, "GN", ARABIC));
        scriptsByLocale.put("sv", getScriptsMap("", LATIN));
        scriptsByLocale.put("sw", getScriptsMap("", LATIN));
        scriptsByLocale.put("swb", getScriptsMap("", ARABIC, "YT", LATIN));
        scriptsByLocale.put("swc", getScriptsMap("", LATIN));
        scriptsByLocale.put("swv", getScriptsMap("", ""));
        scriptsByLocale.put("sxn", getScriptsMap("", ""));
        scriptsByLocale.put("syi", getScriptsMap("", ""));
        scriptsByLocale.put("syl", getScriptsMap("", "Beng", "BD", "Sylo"));
        scriptsByLocale.put("syr", getScriptsMap("", "Syrc"));
        scriptsByLocale.put("ta", getScriptsMap("", "Taml"));
        scriptsByLocale.put("tab", getScriptsMap("", CYRILLIC));
        scriptsByLocale.put("taj", getScriptsMap("", ""));
        scriptsByLocale.put("tbw", getScriptsMap("", LATIN));
        scriptsByLocale.put("tcy", getScriptsMap("", "Knda"));
        scriptsByLocale.put("tdd", getScriptsMap("", "Tale"));
        scriptsByLocale.put("tdg", getScriptsMap("", ""));
        scriptsByLocale.put("tdh", getScriptsMap("", ""));
        scriptsByLocale.put("te", getScriptsMap("", "Telu"));
        scriptsByLocale.put("tem", getScriptsMap("", LATIN));
        scriptsByLocale.put("teo", getScriptsMap("", LATIN));
        scriptsByLocale.put("ter", getScriptsMap("", LATIN));
        scriptsByLocale.put("tet", getScriptsMap("", LATIN));
        scriptsByLocale.put("tg", getScriptsMap("", CYRILLIC, "PK", ARABIC));
        scriptsByLocale.put("th", getScriptsMap("", THAI));
        scriptsByLocale.put("thl", getScriptsMap("", ""));
        scriptsByLocale.put("thq", getScriptsMap("", ""));
        scriptsByLocale.put("thr", getScriptsMap("", ""));
        scriptsByLocale.put("ti", getScriptsMap("", "Ethi"));
        scriptsByLocale.put("tig", getScriptsMap("", "Ethi"));
        scriptsByLocale.put("tiv", getScriptsMap("", LATIN));
        scriptsByLocale.put("tk", getScriptsMap("", LATIN));
        scriptsByLocale.put("tkl", getScriptsMap("", LATIN));
        scriptsByLocale.put("tkt", getScriptsMap("", ""));
        scriptsByLocale.put("tli", getScriptsMap("", LATIN));
        scriptsByLocale.put("tmh", getScriptsMap("", LATIN));
        scriptsByLocale.put("tn", getScriptsMap("", LATIN));
        scriptsByLocale.put("to", getScriptsMap("", LATIN));
        scriptsByLocale.put("tog", getScriptsMap("", LATIN));
        scriptsByLocale.put("tpi", getScriptsMap("", LATIN));
        scriptsByLocale.put("tr", getScriptsMap("", LATIN, "DE", ARABIC, "MK", ARABIC));
        scriptsByLocale.put("tru", getScriptsMap("", LATIN));
        scriptsByLocale.put("trv", getScriptsMap("", LATIN));
        scriptsByLocale.put("ts", getScriptsMap("", LATIN));
        scriptsByLocale.put("tsf", getScriptsMap("", ""));
        scriptsByLocale.put("tsg", getScriptsMap("", LATIN));
        scriptsByLocale.put("tsi", getScriptsMap("", LATIN));
        scriptsByLocale.put("tsj", getScriptsMap("", ""));
        scriptsByLocale.put("tt", getScriptsMap("", CYRILLIC));
        scriptsByLocale.put("ttj", getScriptsMap("", ""));
        scriptsByLocale.put("tts", getScriptsMap("", THAI));
        scriptsByLocale.put("tum", getScriptsMap("", LATIN));
        scriptsByLocale.put("tut", getScriptsMap("", CYRILLIC));
        scriptsByLocale.put("tvl", getScriptsMap("", LATIN));
        scriptsByLocale.put("twq", getScriptsMap("", LATIN));
        scriptsByLocale.put("ty", getScriptsMap("", LATIN));
        scriptsByLocale.put("tyv", getScriptsMap("", CYRILLIC));
        scriptsByLocale.put("tzm", getScriptsMap("", LATIN));
        scriptsByLocale.put("ude", getScriptsMap("", CYRILLIC));
        scriptsByLocale.put("udm", getScriptsMap("", CYRILLIC, "RU", LATIN));
        scriptsByLocale.put("ug", getScriptsMap("", ARABIC, "KZ", CYRILLIC, "MN", CYRILLIC));
        scriptsByLocale.put("uga", getScriptsMap("", "Ugar"));
        scriptsByLocale.put("uk", getScriptsMap("", CYRILLIC));
        scriptsByLocale.put("uli", getScriptsMap("", LATIN));
        scriptsByLocale.put("umb", getScriptsMap("", LATIN));
        scriptsByLocale.put("und", getScriptsMap("", ""));
        scriptsByLocale.put("unr", getScriptsMap("", "Beng", "NP", DEVANAGARI));
        scriptsByLocale.put("unx", getScriptsMap("", "Beng"));
        scriptsByLocale.put("ur", getScriptsMap("", ARABIC));
        scriptsByLocale.put("uz", getScriptsMap("", LATIN, "AF", ARABIC, "CN", CYRILLIC));
        scriptsByLocale.put("vai", getScriptsMap("", "Vaii"));
        scriptsByLocale.put("ve", getScriptsMap("", LATIN));
        scriptsByLocale.put("vi", getScriptsMap("", LATIN, "US", "Hani"));
        scriptsByLocale.put("vic", getScriptsMap("", ""));
        scriptsByLocale.put("vmw", getScriptsMap("", ""));
        scriptsByLocale.put("vo", getScriptsMap("", LATIN));
        scriptsByLocale.put("vot", getScriptsMap("", LATIN));
        scriptsByLocale.put("vun", getScriptsMap("", LATIN));
        scriptsByLocale.put("wa", getScriptsMap("", LATIN));
        scriptsByLocale.put("wae", getScriptsMap("", LATIN));
        scriptsByLocale.put("wak", getScriptsMap("", LATIN));
        scriptsByLocale.put("wal", getScriptsMap("", "Ethi"));
        scriptsByLocale.put("war", getScriptsMap("", LATIN));
        scriptsByLocale.put("was", getScriptsMap("", LATIN));
        scriptsByLocale.put("wbq", getScriptsMap("", ""));
        scriptsByLocale.put("wbr", getScriptsMap("", ""));
        scriptsByLocale.put("wls", getScriptsMap("", ""));
        scriptsByLocale.put("wo", getScriptsMap("", LATIN));
        scriptsByLocale.put("wtm", getScriptsMap("", ""));
        scriptsByLocale.put("xal", getScriptsMap("", CYRILLIC));
        scriptsByLocale.put("xav", getScriptsMap("", ""));
        scriptsByLocale.put("xcr", getScriptsMap("", "Cari"));
        scriptsByLocale.put("xh", getScriptsMap("", LATIN));
        scriptsByLocale.put("xnr", getScriptsMap("", ""));
        scriptsByLocale.put("xog", getScriptsMap("", LATIN));
        scriptsByLocale.put("xpr", getScriptsMap("", "Prti"));
        scriptsByLocale.put("xsa", getScriptsMap("", "Sarb"));
        scriptsByLocale.put("xsr", getScriptsMap("", DEVANAGARI));
        scriptsByLocale.put("xum", getScriptsMap("", "Ital"));
        scriptsByLocale.put("yao", getScriptsMap("", LATIN));
        scriptsByLocale.put("yap", getScriptsMap("", LATIN));
        scriptsByLocale.put("yav", getScriptsMap("", LATIN));
        scriptsByLocale.put("ybb", getScriptsMap("", ""));
        scriptsByLocale.put("yi", getScriptsMap("", HEBREW));
        scriptsByLocale.put("yo", getScriptsMap("", LATIN));
        scriptsByLocale.put("yrk", getScriptsMap("", CYRILLIC));
        scriptsByLocale.put("yua", getScriptsMap("", ""));
        scriptsByLocale.put("yue", getScriptsMap("", "Hans"));
        scriptsByLocale.put("za", getScriptsMap("", LATIN, "CN", "Hans"));
        scriptsByLocale.put("zap", getScriptsMap("", LATIN));
        scriptsByLocale.put("zdj", getScriptsMap("", ""));
        scriptsByLocale.put("zea", getScriptsMap("", ""));
        scriptsByLocale.put("zen", getScriptsMap("", "Tfng"));
        scriptsByLocale.put("zh", getScriptsMap("", "Hant", "CN", "Hans", "HK", "Hans", "MO", "Hans", "SG", "Hans", "MN", "Hans"));
        scriptsByLocale.put("zmi", getScriptsMap("", ""));
        scriptsByLocale.put("zu", getScriptsMap("", LATIN));
        scriptsByLocale.put("zun", getScriptsMap("", LATIN));
        scriptsByLocale.put("zza", getScriptsMap("", ARABIC));
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
