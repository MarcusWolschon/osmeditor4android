package io.vespucci.resources;

import java.util.Locale;
import java.util.Map.Entry;
import java.util.TreeMap;

import android.net.Uri;
import androidx.annotation.NonNull;

public final class ImageryOffsetId {

    static final String MAPBOX = "mapbox";
    static final String BING   = "bing";

    private static final String QUERY_START        = "?";
    private static final String QUERY_DELIM        = "&";
    private static final String KV_SEPARATOR       = "=";
    private static final String ACCESS_TOKEN_PARAM = "access_token";

    /**
     * Private constructor
     */
    private ImageryOffsetId() {
        // empty
    }

    /**
     * This is based on the reference implementation see
     * 
     * https://trac.openstreetmap.org/browser/subversion/applications/editors/josm/plugins/imagery_offset_db/src/iodb/ImageryIdGenerator.java#L24
     * 
     */
    @NonNull
    static String generate(@NonNull String id, @NonNull String url) {

        // predefined special cases
        if (BING.equalsIgnoreCase(id)) {
            return BING;
        }

        if (MAPBOX.equalsIgnoreCase(id)) {
            return MAPBOX;
        }

        Uri parsed = Uri.parse(url);

        int port = parsed.getPort();
        url = parsed.getHost() + (port != -1 ? ":" + port : "") + parsed.getPath();
        // TMS: remove /{zoom} and /{y}.png parts
        url = url.replaceAll("\\/\\{[^}]+\\}(?:\\.\\w+)?", "");
        // TMS: remove variable parts
        url = url.replaceAll("\\{[^}]+\\}", "");
        while (url.contains("..")) {
            url = url.replace("..", ".");
        }
        if (url.startsWith(".")) {
            url = url.substring(1);
        }

        String query = parsed.getQuery();
        if (query == null) {
            return url;
        }

        Uri.Builder builder = new Uri.Builder();
        TreeMap<String, String> qparams = new TreeMap<>();
        String[] qparamsStr = query.split(QUERY_DELIM);
        for (String p : qparamsStr) {
            String[] kv = p.split(KV_SEPARATOR);
            kv[0] = kv[0].toLowerCase(Locale.US);
            // TMS: skip parameters with variable values and Mapbox's access token
            boolean hasValue = kv.length > 1;
            if ((hasValue && hasTemplate(kv[1])) || ACCESS_TOKEN_PARAM.equals(kv[0])) { // NOSONAR
                continue;
            }
            qparams.put(kv[0], hasValue ? kv[1] : null);
        }
        // Reconstruct query parameters
        for (Entry<String, String> qk : qparams.entrySet()) {
            builder.appendQueryParameter(qk.getKey(), qk.getValue());
        }
        query = builder.build().getQuery(); // can return null
        return url + (query != null ? QUERY_START + query : "");
    }

    /**
     * Determine if the input string contains a moustache placeholder
     * 
     * Traverses the String once
     * 
     * @param input the input string
     * @return true if it contains a placeholder
     */
    private static boolean hasTemplate(@NonNull String input) {
        int length = input.length();
        for (int i = 0; i < length; i++) {
            if (input.charAt(i) == '{') {
                for (int j = i + 1; j < length; j++) {
                    if (input.charAt(j) == '}') {
                        return true;
                    }
                }
                return false;
            }
        }
        return false;
    }
}
