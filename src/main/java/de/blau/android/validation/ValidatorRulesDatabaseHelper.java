package de.blau.android.validation;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import de.blau.android.osm.Tags;

public class ValidatorRulesDatabaseHelper extends SQLiteOpenHelper {
    private static final String DEBUG_TAG = "ValidatorRulesDatabaseHelper";
    private static final String DATABASE_NAME = "validator_rules";
    private static final int DATABASE_VERSION = 2;

    public ValidatorRulesDatabaseHelper(final Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        try {
            db.execSQL("CREATE TABLE rulesets (id INTEGER, name TEXT)");
            ValidatorRulesDatabase.addRuleset(db, ValidatorRulesDatabase.DEFAULT_RULESET, ValidatorRulesDatabase.DEFAULT_RULESET_NAME); 
            
            db.execSQL("CREATE TABLE resurveytags (ruleset INTEGER, key TEXT, value TEXT DEFAULT NULL, is_regexp INTEGER DEFAULT 0, days INTEGER DEFAULT 365, FOREIGN KEY(ruleset) REFERENCES rulesets(id))");
            ValidatorRulesDatabase.addResurvey(db, ValidatorRulesDatabase.DEFAULT_RULESET, Tags.KEY_SHOP, null, false, 365);
            ValidatorRulesDatabase.addResurvey(db, ValidatorRulesDatabase.DEFAULT_RULESET, Tags.KEY_AMENITY, Tags.VALUE_RESTAURANT, false, 365);
            ValidatorRulesDatabase.addResurvey(db, ValidatorRulesDatabase.DEFAULT_RULESET, Tags.KEY_AMENITY, Tags.VALUE_FAST_FOOD, false, 365);
            ValidatorRulesDatabase.addResurvey(db, ValidatorRulesDatabase.DEFAULT_RULESET, Tags.KEY_AMENITY, Tags.VALUE_CAFE, false, 365);
            ValidatorRulesDatabase.addResurvey(db, ValidatorRulesDatabase.DEFAULT_RULESET, Tags.KEY_AMENITY, Tags.VALUE_PUB, false, 365);
            ValidatorRulesDatabase.addResurvey(db, ValidatorRulesDatabase.DEFAULT_RULESET, Tags.KEY_AMENITY, Tags.VALUE_BAR, false, 365);
            ValidatorRulesDatabase.addResurvey(db, ValidatorRulesDatabase.DEFAULT_RULESET, Tags.KEY_AMENITY, Tags.VALUE_TOILETS, false, 365);
            
            db.execSQL("CREATE TABLE checktags (ruleset INTEGER, key TEXT, optional INTEGER DEFAULT 0, FOREIGN KEY(ruleset) REFERENCES rulesets(id))");
            ValidatorRulesDatabase.addCheck(db, ValidatorRulesDatabase.DEFAULT_RULESET, Tags.KEY_OPENING_HOURS, false);
            ValidatorRulesDatabase.addCheck(db, ValidatorRulesDatabase.DEFAULT_RULESET, Tags.KEY_NAME, false);
            ValidatorRulesDatabase.addCheck(db, ValidatorRulesDatabase.DEFAULT_RULESET, Tags.KEY_WHEELCHAIR, false);
        } catch (SQLException e) {
            Log.w(DEBUG_TAG, "Problem creating database", e);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(DEBUG_TAG, "Upgrading database from version " + oldVersion + " to " + newVersion);
        if (oldVersion <= 1 && newVersion >= 2) {
            db.execSQL("ALTER TABLE resurveytags ADD COLUMN is_regexp INTEGER DEFAULT 0");
        }
    }
}
