package de.blau.android.search;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.io.ByteArrayInputStream;
import java.util.Map;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import ch.poole.osm.josmfilterparser.Condition;
import ch.poole.osm.josmfilterparser.JosmFilterParser;

public final class Util {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, Util.class.getSimpleName().length());
    private static final String DEBUG_TAG = Util.class.getSimpleName().substring(0, TAG_LEN);

    /**
     * Private constructor
     */
    private Util() {
        // nothing
    }
    
    /**
     * Get the Condition object for an expression, caching it
     * 
     * @param conditionCache the cache
     * @param expression the expression
     * @return the Condition or null
     */
    @Nullable
    public static Condition getCondition(@NonNull Map<String, Condition> conditionCache, @NonNull String expression) {
        Condition condition = conditionCache.get(expression); // NOSONAR
        if (condition == null) {
            try {
                JosmFilterParser parser = new JosmFilterParser(new ByteArrayInputStream(expression.getBytes()));
                condition = parser.condition();
            } catch (ch.poole.osm.josmfilterparser.ParseException | IllegalArgumentException ex) {
                Log.e(DEBUG_TAG, "member_expression " + expression + " caused " + ex.getMessage());
                try {
                    JosmFilterParser parser = new JosmFilterParser(new ByteArrayInputStream("".getBytes()));
                    condition = parser.condition();
                } catch (ch.poole.osm.josmfilterparser.ParseException | IllegalArgumentException ex2) {
                    Log.e(DEBUG_TAG, "member_expression dummy caused " + ex2.getMessage());
                }
            }
            conditionCache.put(expression, condition);
        } else {
            condition.reset();
        }
        return condition;
    }
}
