package de.blau.android.validation;

import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import androidx.annotation.NonNull;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.viewpager.widget.PagerTabStrip;
import androidx.cursoradapter.widget.CursorAdapter;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import ch.poole.android.numberpicker.library.NumberPicker;
import de.blau.android.App;
import de.blau.android.R;
import de.blau.android.dialogs.ViewPagerAdapter;
import de.blau.android.filter.Filter;
import de.blau.android.util.ThemeUtils;
import de.blau.android.views.ExtendedViewPager;

public class ValidatorRulesUI {
    private static final String DEBUG_TAG = ValidatorRulesUI.class.getSimpleName();

    /**
     * Ruleset database related methods and fields
     */
    private Cursor          resurveyCursor;
    private ResurveyAdapter resurveyAdapter;
    private Cursor          checkCursor;
    private CheckAdapter    checkAdapter;

    /**
     * Show a list of the templates in the database, selection will either load a template or start the edit dialog on
     * it
     * 
     * @param context Android context
     */
    public void manageRulesetContents(@NonNull final Context context) {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(context);
        View rulesetView = LayoutInflater.from(context).inflate(R.layout.validator_ruleset_list, null);
        ExtendedViewPager pager = (ExtendedViewPager) rulesetView.findViewById(R.id.pager);
        PagerTabStrip pagerTabStrip = (PagerTabStrip) pager.findViewById(R.id.pager_header);
        pagerTabStrip.setDrawFullUnderline(true);
        pagerTabStrip.setTabIndicatorColor(ThemeUtils.getStyleAttribColorValue(context, R.attr.colorAccent, R.color.dark_grey));

        pager.setAdapter(new ViewPagerAdapter(context, rulesetView, new int[] { R.id.resurvey_page, R.id.check_page },
                new int[] { R.string.resurvey_entries, R.string.check_entries }));

        alertDialog.setTitle(context.getString(R.string.validator_title, context.getString(R.string.default_)));
        alertDialog.setView(rulesetView);
        final ValidatorRulesDatabaseHelper vrDb = new ValidatorRulesDatabaseHelper(context); // NOSONAR will be closed when dismissed
        final SQLiteDatabase writableDb = vrDb.getWritableDatabase();
        ListView resurveyList = (ListView) rulesetView.findViewById(R.id.listViewResurvey);
        resurveyCursor = ValidatorRulesDatabase.queryResurveyByName(writableDb, ValidatorRulesDatabase.DEFAULT_RULESET_NAME);
        resurveyAdapter = new ResurveyAdapter(writableDb, context, resurveyCursor);
        resurveyList.setAdapter(resurveyAdapter);
        ListView checkList = (ListView) rulesetView.findViewById(R.id.listViewCheck);
        checkCursor = ValidatorRulesDatabase.queryCheckByName(writableDb, ValidatorRulesDatabase.DEFAULT_RULESET_NAME);
        checkAdapter = new CheckAdapter(writableDb, context, checkCursor);
        checkList.setAdapter(checkAdapter);
        alertDialog.setNeutralButton(R.string.done, null);
        alertDialog.setOnDismissListener(new OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                resurveyCursor.close();
                writableDb.close();
                vrDb.close();
            }
        });
        final FloatingActionButton fab = (FloatingActionButton) rulesetView.findViewById(R.id.add);
        fab.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu popup = new PopupMenu(context, fab);

                // menu items for adding rules
                MenuItem addResurveyEntry = popup.getMenu().add(R.string.add_resurvey_entry);
                addResurveyEntry.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        showResurveyDialog(context, writableDb, false, -1);
                        return true;
                    }
                });
                MenuItem addCheckEntry = popup.getMenu().add(R.string.add_check_entry);
                addCheckEntry.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        showCheckDialog(context, writableDb, false, -1);
                        return true;
                    }
                });
                popup.show();// showing popup menu
            }
        });
        alertDialog.show();
    }

    private class ResurveyAdapter extends CursorAdapter {
        final SQLiteDatabase db;

        /**
         * Construct a new ResurveyAdapter
         * 
         * @param db an open database
         * @param context an Android Context
         * @param cursor a Cursor
         */
        public ResurveyAdapter(final SQLiteDatabase db, Context context, Cursor cursor) {
            super(context, cursor, 0);
            this.db = db;
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            Log.d(DEBUG_TAG, "newView");
            return LayoutInflater.from(context).inflate(R.layout.validator_ruleset_list_resurvey_item, parent, false);
        }

        @Override
        public void bindView(final View view, final Context context, Cursor cursor) {
            Log.d(DEBUG_TAG, "bindView");
            final int id = cursor.getInt(cursor.getColumnIndexOrThrow("_id"));
            view.setTag(id);
            Log.d(DEBUG_TAG, "bindView id " + id);
            String value = cursor.getString(cursor.getColumnIndexOrThrow(ValidatorRulesDatabase.VALUE_FIELD));
            String key = cursor.getString(cursor.getColumnIndexOrThrow(ValidatorRulesDatabase.KEY_FIELD));
            int days = cursor.getInt(cursor.getColumnIndexOrThrow(ValidatorRulesDatabase.DAYS_FIELD));
            TextView valueView = (TextView) view.findViewById(R.id.value);
            valueView.setText(value != null ? value : "*");
            TextView keyView = (TextView) view.findViewById(R.id.key);
            keyView.setText(key);
            TextView daysView = (TextView) view.findViewById(R.id.days);
            daysView.setText(Integer.toString(days));
            view.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    Integer id = (Integer) view.getTag();
                    showResurveyDialog(context, db, true, id != null ? id : -1);
                }
            });
        }
    }

    /**
     * Replace the current cursor for the resurvey table
     * 
     * @param db the template database
     */
    private void newResurveyCursor(@NonNull final SQLiteDatabase db) {
        Cursor newCursor = ValidatorRulesDatabase.queryResurveyByName(db, null);
        Cursor oldCursor = resurveyAdapter.swapCursor(newCursor);
        oldCursor.close();
        resurveyAdapter.notifyDataSetChanged();
    }

    /**
     * Show a dialog for editing and saving a resurvey entry
     * 
     * @param context an Android Context
     * @param db a writable instance of the resurvey entry database
     * @param existing true if this is not a new resurvey entry
     * @param id the rowid of the resurvey entry in the database or -1 if not saved yet
     */
    private void showResurveyDialog(@NonNull final Context context, @NonNull final SQLiteDatabase db, final boolean existing, final int id) {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(context);
        View templateView = LayoutInflater.from(context).inflate(R.layout.validator_ruleset_resurvey_item, null);
        alertDialog.setView(templateView);

        final EditText keyEdit = (EditText) templateView.findViewById(R.id.resurvey_key);
        final EditText valueEdit = (EditText) templateView.findViewById(R.id.resurvey_value);
        final CheckBox regexpCheck = (CheckBox) templateView.findViewById(R.id.resurvey_is_regexp);
        final NumberPicker daysPicker = (NumberPicker) templateView.findViewById(R.id.resurvey_days);
        if (existing) {
            Cursor cursor = db.rawQuery(ValidatorRulesDatabase.QUERY_RESURVEY_BY_ROWID, new String[] { Integer.toString(id) });
            if (cursor.moveToFirst()) {
                String key = cursor.getString(cursor.getColumnIndexOrThrow(ValidatorRulesDatabase.KEY_FIELD));
                String value = cursor.getString(cursor.getColumnIndexOrThrow(ValidatorRulesDatabase.VALUE_FIELD));
                boolean isRegexp = cursor.getInt(cursor.getColumnIndexOrThrow(ValidatorRulesDatabase.ISREGEXP_FIELD)) == 1;
                int days = cursor.getInt(cursor.getColumnIndexOrThrow(ValidatorRulesDatabase.DAYS_FIELD));
                keyEdit.setText(key);
                valueEdit.setText(value);
                regexpCheck.setChecked(isRegexp);
                daysPicker.setValue(days);
            } else {
                Log.e(DEBUG_TAG, "resurvey id " + Integer.toString(id) + " not found");
            }
            cursor.close();

            alertDialog.setTitle(R.string.edit_resurvey_title);
            alertDialog.setNeutralButton(R.string.Delete, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Log.d(DEBUG_TAG, "deleting template " + Integer.toString(id));
                    ValidatorRulesDatabase.deleteResurvey(db, id);
                    newResurveyCursor(db);
                    resetValidator(context);
                }
            });
        } else {
            alertDialog.setTitle(R.string.add_resurvey_title);
            daysPicker.setValue(ValidatorRulesDatabaseHelper.ONE_YEAR);
        }
        alertDialog.setNegativeButton(R.string.cancel, null);

        alertDialog.setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (!existing) {
                    ValidatorRulesDatabase.addResurvey(db, 0, keyEdit.getText().toString(), valueEdit.getText().toString(), regexpCheck.isChecked(),
                            daysPicker.getValue());
                } else {
                    ValidatorRulesDatabase.updateResurvey(db, id, keyEdit.getText().toString(), valueEdit.getText().toString(), regexpCheck.isChecked(),
                            daysPicker.getValue());
                }
                newResurveyCursor(db);
                resetValidator(context);
            }
        });
        alertDialog.show();
    }

    /**
     * Reset the validator and element states after changes to the DB
     * 
     * @param context Android Context
     */
    private void resetValidator(final Context context) {
        // problems need to be recalculated and filter cache cleared
        App.getDelegator().resetProblems();
        Filter filter = App.getLogic().getFilter();
        if (filter != null) {
            filter.clear();
        }
        App.getDefaultValidator(context).reset(context);
    }

    private class CheckAdapter extends CursorAdapter {
        final SQLiteDatabase db;

        /**
         * Construct a new CheckAdapter
         * 
         * @param db an open database
         * @param context an Android Context
         * @param cursor a Cursor
         */
        public CheckAdapter(@NonNull final SQLiteDatabase db, @NonNull Context context, @NonNull Cursor cursor) {
            super(context, cursor, 0);
            this.db = db;
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            Log.d(DEBUG_TAG, "newView");
            return LayoutInflater.from(context).inflate(R.layout.validator_ruleset_list_check_item, parent, false);
        }

        @Override
        public void bindView(final View view, final Context context, Cursor cursor) {
            Log.d(DEBUG_TAG, "bindView");
            final int id = cursor.getInt(cursor.getColumnIndexOrThrow("_id"));
            view.setTag(id);
            Log.d(DEBUG_TAG, "bindView id " + id);
            String key = cursor.getString(cursor.getColumnIndexOrThrow(ValidatorRulesDatabase.KEY_FIELD));
            boolean optional = cursor.getInt(cursor.getColumnIndexOrThrow(ValidatorRulesDatabase.OPTIONAL_FIELD)) == 1;
            TextView keyView = (TextView) view.findViewById(R.id.key);
            keyView.setText(key);
            ImageView optionalView = (ImageView) view.findViewById(R.id.optional);
            optionalView.setVisibility(optional ? View.VISIBLE : View.GONE);
            view.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    Integer id = (Integer) view.getTag();
                    showCheckDialog(context, db, true, id != null ? id : -1);
                }
            });
        }
    }

    /**
     * Replace the current cursor for the check table
     * 
     * @param db the template database
     */
    private void newCheckCursor(@NonNull final SQLiteDatabase db) {
        Cursor newCursor = ValidatorRulesDatabase.queryCheckByName(db, null);
        Cursor oldCursor = checkAdapter.swapCursor(newCursor);
        oldCursor.close();
        checkAdapter.notifyDataSetChanged();
    }

    /**
     * Show a dialog for editing and saving a check entry
     * 
     * @param context an Android Context
     * @param db a writable instance of the check entry database
     * @param existing true if this is not a new check entry
     * @param id the rowid of the check entry in the database or -1 if not saved yet
     */
    private void showCheckDialog(@NonNull final Context context, @NonNull final SQLiteDatabase db, final boolean existing, final int id) {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(context);
        View templateView = LayoutInflater.from(context).inflate(R.layout.validator_ruleset_check_item, null);
        alertDialog.setView(templateView);

        final EditText keyEdit = (EditText) templateView.findViewById(R.id.check_key);
        final CheckBox optionalEdit = (CheckBox) templateView.findViewById(R.id.check_optional);

        if (existing) {
            Cursor cursor = db.rawQuery(ValidatorRulesDatabase.QUERY_CHECK_BY_ROWID, new String[] { Integer.toString(id) });
            if (cursor.moveToFirst()) {
                String key = cursor.getString(cursor.getColumnIndexOrThrow(ValidatorRulesDatabase.KEY_FIELD));
                boolean optional = cursor.getInt(cursor.getColumnIndexOrThrow(ValidatorRulesDatabase.OPTIONAL_FIELD)) == 1;
                keyEdit.setText(key);
                optionalEdit.setChecked(optional);
            } else {
                Log.e(DEBUG_TAG, "check id " + Integer.toString(id) + " not found");
            }
            cursor.close();

            alertDialog.setTitle(R.string.edit_check_title);
            alertDialog.setNeutralButton(R.string.Delete, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Log.d(DEBUG_TAG, "deleting template " + Integer.toString(id));
                    ValidatorRulesDatabase.deleteCheck(db, id);
                    newCheckCursor(db);
                    resetValidator(context);
                }
            });
        } else {
            alertDialog.setTitle(R.string.add_check_title);
        }
        alertDialog.setNegativeButton(R.string.cancel, null);

        alertDialog.setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (!existing) {
                    ValidatorRulesDatabase.addCheck(db, 0, keyEdit.getText().toString(), optionalEdit.isChecked());
                } else {
                    ValidatorRulesDatabase.updateCheck(db, id, keyEdit.getText().toString(), optionalEdit.isChecked());
                }
                newCheckCursor(db);
                resetValidator(context);
            }
        });
        alertDialog.show();
    }
}
