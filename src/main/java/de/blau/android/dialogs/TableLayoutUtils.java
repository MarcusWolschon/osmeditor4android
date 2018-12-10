package de.blau.android.dialogs;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewCompat;
import android.text.SpannableString;
import android.text.TextUtils.TruncateAt;
import android.text.method.LinkMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.text.util.Linkify;
import android.view.View;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TableRow.LayoutParams;
import android.widget.TextView;
import de.blau.android.R;
import de.blau.android.util.ThemeUtils;

/**
 * Add rows to a table layout
 * 
 * @author simon
 *
 */
public class TableLayoutUtils {
    private static final int FIRST_CELL_WIDTH = 5;

    private static final int MAX_FIRST_CELL_WIDTH = 12;

    /**
     * Get a new TableRox with the provided contents - three columns
     * 
     * If cell2 and cell3 are null cell1 will be considered a full width heading, if cell2 != cell3 then cell1 will be
     * highlighted
     * 
     * @param activity the FragmentActivity the TableLayout is being displayed on
     * @param cell1 text for the first cell
     * @param cell2 text for the second cell
     * @param tp LayoutParams for the row
     * @return a TableRow
     */
    @SuppressLint("NewApi")
    @NonNull
    public static TableRow createRow(@NonNull FragmentActivity activity, String cell1, CharSequence cell2, @NonNull TableLayout.LayoutParams tp) {
        return createRow(activity, cell1, null, cell2, false, tp);
    }

    /**
     * Get a new TableRox with the provided contents - three columns
     * 
     * If cell2 and cell3 are null cell1 will be considered a full width heading, if cell2 != cell3 then cell1 will be
     * highlighted
     * 
     * @param activity the FragmentActivity the TableLayout is being displayed on
     * @param cell1 text for the first cell
     * @param cell2 text for the second cell
     * @param cell3 text for the third cell
     * @param tp LayoutParams for the row
     * @return a TableRow
     */
    @SuppressLint("NewApi")
    @NonNull
    public static TableRow createRow(@NonNull FragmentActivity activity, String cell1, @Nullable CharSequence cell2, @Nullable CharSequence cell3,
            @NonNull TableLayout.LayoutParams tp) {
        return createRow(activity, cell1, cell2, cell3, false, tp);
    }

    /**
     * Get a new TableRox with the provided contents - three columns
     * 
     * If cell2 and cell3 are null cell1 will be considered a full width heading, if cell2 != cell3 then cell1 will be
     * highlighted
     * 
     * @param activity the FragmentActivity the TableLayout is being displayed on
     * @param cell1 text for the first cell
     * @param cell2 text for the second cell
     * @param isUrl if true don't allow C&P on the values so that they can be clicked on
     * @param tp LayoutParams for the row
     * @return a TableRow
     */
    @SuppressLint("NewApi")
    @NonNull
    public static TableRow createRow(@NonNull FragmentActivity activity, @NonNull String cell1, @Nullable CharSequence cell2, boolean isUrl,
            @NonNull TableLayout.LayoutParams tp) {
        return createRow(activity, cell1, null, cell2, isUrl, tp);
    }

    /**
     * Get a new TableRox with the provided contents - three columns
     * 
     * If cell2 and cell3 are null cell1 will be considered a full width heading, if cell2 != cell3 then cell1 will be
     * highlighted
     * 
     * @param activity the FragmentActivity the TableLayout is being displayed on
     * @param cell1 a string resource id for the first cell
     * @param cell2 text for the second cell
     * @param cell3 text for the third cell
     * @param tp LayoutParams for the row
     * @return a TableRow
     */
    @SuppressLint("NewApi")
    @NonNull
    public static TableRow createRow(@NonNull FragmentActivity activity, int cell1, CharSequence cell2, CharSequence cell3,
            @NonNull TableLayout.LayoutParams tp) {
        return createRow(activity, cell1, cell2, cell3, false, tp);
    }

    /**
     * Get a new TableRox with the provided contents - three columns
     * 
     * If cell2 and cell3 are null cell1 will be considered a full width heading, if cell2 != cell3 then cell1 will be
     * highlighted
     * 
     * @param activity the FragmentActivity the TableLayout is being displayed on
     * @param cell1 a string resource id for the first cell
     * @param cell2 text for the second cell
     * @param cell3 text for the third cell
     * @param isUrl if true don't allow C&P on the values so that they can be clicked on
     * @param tp LayoutParams for the row
     * @return a TableRow
     */
    @SuppressLint("NewApi")
    @NonNull
    public static TableRow createRow(@NonNull FragmentActivity activity, int cell1, @Nullable CharSequence cell2, @Nullable CharSequence cell3, boolean isUrl,
            @NonNull TableLayout.LayoutParams tp) {
        return createRow(activity, activity.getString(cell1), cell2, cell3, isUrl, tp);
    }

    /**
     * Get a new TableRox with the provided contents - three columns
     * 
     * If cell2 and cell3 are null cell1 will be considered a full width heading, if cell2 != cell3 then cell1 will be
     * highlighted
     * 
     * @param activity the FragmentActivity the TableLayout is being displayed on
     * @param cell1 text for the first cell
     * @param cell2 text for the second cell
     * @param cell3 text for the third cell
     * @param isUrl if true don't allow C&P on the values so that they can be clicked on
     * @param tp LayoutParams for the row
     * @return a TableRow
     */
    @SuppressLint("NewApi")
    @NonNull
    public static TableRow createRow(@NonNull FragmentActivity activity, @NonNull String cell1, @Nullable CharSequence cell2, @Nullable CharSequence cell3,
            boolean isUrl, @NonNull TableLayout.LayoutParams tp) {
        TableRow tr = new TableRow(activity);
        TextView cell = new TextView(activity);
        cell.setSingleLine();
        cell.setMinEms(FIRST_CELL_WIDTH);
        cell.setMaxEms(MAX_FIRST_CELL_WIDTH);

        SpannableString span = new SpannableString((CharSequence) cell1);
        boolean isTitle = cell1 != null && cell2 == null && cell3 == null;
        if (isTitle) { // heading
            span.setSpan(new StyleSpan(Typeface.BOLD), 0, span.length(), 0);
        } else if (cell2 != null && !cell2.equals(cell3)) { // values changed
            span.setSpan(new ForegroundColorSpan(ThemeUtils.getStyleAttribColorValue(activity, R.attr.colorAccent, Color.GREEN)), 0, span.length(), 0);
        }
        cell.setText(span);

        cell.setEllipsize(TruncateAt.MARQUEE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            cell.setTextIsSelectable(true);
        }
        tr.addView(cell);
        if (isTitle) {
            TableRow.LayoutParams trlp = (LayoutParams) cell.getLayoutParams();
            trlp.span = 3;
            trlp.weight = 1;
            cell.setLayoutParams(trlp);
        } else {
            addCell(activity, cell2, isUrl, tr, null);
            addCell(activity, cell3, isUrl, tr, null);
        }
        tr.setLayoutParams(tp);
        return tr;
    }

    /**
     * Get a new TableRox with the provided contents - two columns
     * 
     * @param activity the FragmentActivity the TableLayout is being displayed on
     * @param cell1 a string resource id for the first cell
     * @param cell2 text for the second cell
     * @param tp LayoutParams for the row
     * @return a TableRow
     */
    @SuppressLint("NewApi")
    @NonNull
    public static TableRow createRow(@NonNull FragmentActivity activity, int cell1, @Nullable CharSequence cell2, @NonNull TableLayout.LayoutParams tp) {
        return createRow(activity, cell1, cell2, false, tp);
    }

    /**
     * Get a new TableRox with the provided contents - two columns
     * 
     * @param activity the FragmentActivity the TableLayout is being displayed on
     * @param cell1 a string resource id for the first cell
     * @param cell2 text for the second cell
     * @param isUrl if true don't allow C&P on the values so that they can be clicked on
     * @param tp LayoutParams for the row
     * @return a TableRow
     */
    @SuppressLint("NewApi")
    @NonNull
    public static TableRow createRow(@NonNull FragmentActivity activity, int cell1, @Nullable CharSequence cell2, boolean isUrl,
            @NonNull TableLayout.LayoutParams tp) {
        TableRow tr = new TableRow(activity);
        TextView cell = new TextView(activity);
        cell.setMinEms(FIRST_CELL_WIDTH);
        cell.setMaxEms(MAX_FIRST_CELL_WIDTH);
        cell.setMaxLines(2);
        cell.setText(cell1);
        if (cell2 == null) {
            cell.setTypeface(null, Typeface.BOLD);
        }
        cell.setEllipsize(TruncateAt.MARQUEE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            cell.setTextIsSelectable(true);
        }
        tr.addView(cell);
        TableRow.LayoutParams trp = new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT);
        if (cell2 == null) {
            trp.span = 2;
        }
        addCell(activity, cell2, isUrl, tr, trp);
        tr.setLayoutParams(tp);
        return tr;
    }

    /**
     * Add a new cell to a TableRow
     * 
     * @param activity the FragmentActivity the TableLayout is being displayed on
     * @param cellText the text to use for the cell
     * @param isUrl if true don't allow C&P on the values so that they can be clicked on
     * @param tr the TableRow to add the cell to
     * @param tp LayoutParams for the row
     */
    @NonNull
    private static void addCell(@NonNull FragmentActivity activity, @Nullable CharSequence cellText, boolean isUrl, TableRow tr,
            @Nullable TableRow.LayoutParams tp) {
        TextView cell;
        cell = new TextView(activity);
        if (cellText != null) {
            cell.setText(cellText);
            cell.setMinEms(FIRST_CELL_WIDTH);
            Linkify.addLinks(cell, Linkify.WEB_URLS);
            cell.setMovementMethod(LinkMovementMethod.getInstance());

            ViewCompat.setPaddingRelative(cell, 10, 0, 0, 0);

            cell.setEllipsize(TruncateAt.MARQUEE);
            if (!isUrl && Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                cell.setTextIsSelectable(true);
            }
            if (tp != null) {
                cell.setLayoutParams(tp);
            }
            tr.addView(cell);
        }
    }

    /**
     * Get a divider for the TableLayout
     * 
     * @param activity the FragmentActivity the TableLayout is being displayed on
     * @return a empty View the width of the TableLayout
     */
    @NonNull
    public static View divider(@NonNull FragmentActivity activity) {
        TableRow tr = new TableRow(activity);
        View v = new View(activity);
        TableRow.LayoutParams trp = new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, 1);
        trp.span = 3;
        v.setLayoutParams(trp);
        v.setBackgroundColor(Color.rgb(204, 204, 204));
        tr.addView(v);
        return tr;
    }
}
