package de.blau.android.dialogs;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.TextUtils.TruncateAt;
import android.text.method.LinkMovementMethod;
import android.text.style.StyleSpan;
import android.text.util.Linkify;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TableRow.LayoutParams;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.FragmentActivity;
import de.blau.android.R;
import de.blau.android.util.ThemeUtils;

/**
 * Add rows to a table layout
 * 
 * @author Simon Poole
 *
 */
public final class TableLayoutUtils {
    private static final int FIRST_CELL_WIDTH = 5;

    private static final int MAX_FIRST_CELL_WIDTH = 12;

    /**
     * Private constructor to stop instantiation
     */
    private TableLayoutUtils() {
        // private
    }

    /**
     * Get a new TableRow with the provided contents - three columns
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
        return createRow(activity, cell1, null, cell2, false, tp, R.attr.colorAccent, Color.GREEN);
    }

    /**
     * Get a new TableRow with the provided contents - three columns
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
        return createRow(activity, cell1, cell2, cell3, false, tp, R.attr.colorAccent, Color.GREEN);
    }

    /**
     * Get a new TableRow with the provided contents - three columns
     * 
     * If cell2 and cell3 are null cell1 will be considered a full width heading, if cell2 != cell3 then cell1 will be
     * highlighted
     * 
     * @param activity the FragmentActivity the TableLayout is being displayed on
     * @param cell1 text for the first cell
     * @param cell2 text for the second cell
     * @param cell3 text for the third cell
     * @param tp LayoutParams for the row
     * @param highlightColorAttr the highlight color attribute resource id
     * @param highlightColorFallback a fallback highlight color
     * @return a TableRow
     */
    @SuppressLint("NewApi")
    @NonNull
    public static TableRow createRow(@NonNull FragmentActivity activity, String cell1, @Nullable CharSequence cell2, @Nullable CharSequence cell3,
            @NonNull TableLayout.LayoutParams tp, int highlightColorAttr, int highlightColorFallback) {
        return createRow(activity, cell1, cell2, cell3, false, tp, highlightColorAttr, highlightColorFallback);
    }

    /**
     * Get a new TableRow with the provided contents - three columns
     * 
     * If cell2 and cell3 are null cell1 will be considered a full width heading, if cell2 != cell3 then cell1 will be
     * highlighted
     * 
     * @param activity the FragmentActivity the TableLayout is being displayed on
     * @param cell1 text for the first cell
     * @param cell2 text for the second cell
     * @param isUrl if true don't allow C&amp;P on the values so that they can be clicked on
     * @param tp LayoutParams for the row
     * @return a TableRow
     */
    @SuppressLint("NewApi")
    @NonNull
    public static TableRow createRow(@NonNull FragmentActivity activity, @NonNull String cell1, @Nullable CharSequence cell2, boolean isUrl,
            @NonNull TableLayout.LayoutParams tp) {
        return createRow(activity, cell1, null, cell2, isUrl, tp, R.attr.colorAccent, Color.GREEN);
    }

    /**
     * Get a new TableRow with the provided contents - three columns
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
     * Get a new TableRow with the provided contents - three columns
     * 
     * If cell2 and cell3 are null cell1 will be considered a full width heading, if cell2 != cell3 then cell1 will be
     * highlighted
     * 
     * @param activity the FragmentActivity the TableLayout is being displayed on
     * @param cell1 a string resource id for the first cell
     * @param cell2 text for the second cell
     * @param cell3 text for the third cell
     * @param isUrl if true don't allow C&amp;P on the values so that they can be clicked on
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
     * Get a new TableRow with the provided contents - three columns
     * 
     * If cell2 and cell3 are null cell1 will be considered a full width heading, if cell2 != cell3 then cell1 will be
     * highlighted
     * 
     * @param activity the FragmentActivity the TableLayout is being displayed on
     * @param cell1 text for the first cell
     * @param cell2 text for the second cell
     * @param cell3 text for the third cell
     * @param isUrl if true don't allow C&amp;P on the values so that they can be clicked on
     * @param tp LayoutParams for the row
     * @return a TableRow
     */
    @SuppressLint("NewApi")
    @NonNull
    public static TableRow createRow(@NonNull FragmentActivity activity, @NonNull String cell1, @Nullable CharSequence cell2, @Nullable CharSequence cell3,
            boolean isUrl, @NonNull TableLayout.LayoutParams tp) {
        return createRow(activity, cell1, cell2, cell3, isUrl, tp, R.attr.colorAccent, Color.GREEN);
    }

    /**
     * Get a new TableRow with the provided contents - three columns
     * 
     * If cell2 and cell3 are null cell1 will be considered a full width heading, if cell2 != cell3 then cell1 will be
     * highlighted
     * 
     * @param activity the FragmentActivity the TableLayout is being displayed on
     * @param cell1 text for the first cell
     * @param cell2 text for the second cell
     * @param cell3 text for the third cell
     * @param isUrl if true don't allow C&amp;P on the values so that they can be clicked on
     * @param tp LayoutParams for the row
     * @param highlightColorAttr the highlight color attribute resource id, if -1 no highlight will be used
     * @param highlightColorFallback a fallback highlight color
     * @return a TableRow
     */
    @SuppressLint("NewApi")
    @NonNull
    public static TableRow createRow(@NonNull FragmentActivity activity, @NonNull String cell1, @Nullable CharSequence cell2, @Nullable CharSequence cell3,
            boolean isUrl, @NonNull TableLayout.LayoutParams tp, int highlightColorAttr, int highlightColorFallback) {
        TableRow tr = new TableRow(activity);
        TextView cell = new TextView(activity);
        cell.setSingleLine();
        cell.setMinEms(FIRST_CELL_WIDTH);
        cell.setMaxEms(MAX_FIRST_CELL_WIDTH);

        SpannableString span = new SpannableString((CharSequence) cell1);
        SpannableString span2 = null;
        if (cell2 != null) {
            span2 = new SpannableString(cell2);
        }
        SpannableString span3 = null;
        if (cell3 != null) {
            span3 = new SpannableString(cell3);
        }
        boolean isTitle = cell2 == null && cell3 == null;
        if (isTitle) { // heading
            span.setSpan(new StyleSpan(Typeface.BOLD), 0, span.length(), 0);
        } else if (highlightColorAttr != -1 && (cell2 != null && (cell3 == null || !cell2.toString().equals(cell3.toString())))) {
            // note a CharSequence doesn't necessarily have a content aware equals, so we need to convert to String
            // first
            ThemeUtils.setSpanColor(activity, span, highlightColorAttr, highlightColorFallback);
            ThemeUtils.setSpanColor(activity, span2, highlightColorAttr, highlightColorFallback);
            ThemeUtils.setSpanColor(activity, span3, highlightColorAttr, highlightColorFallback);
        }
        cell.setText(span);
        cell.setEllipsize(TruncateAt.MARQUEE);
        cell.setTextIsSelectable(true);
        tr.addView(cell);
        if (isTitle) {
            TableRow.LayoutParams trlp = (LayoutParams) cell.getLayoutParams();
            trlp.span = 3;
            trlp.weight = 1;
            cell.setLayoutParams(trlp);
        } else {
            addCell(activity, span2, isUrl, tr, null);
            addCell(activity, span3, isUrl, tr, null);
        }
        tr.setLayoutParams(tp);
        return tr;
    }

    /**
     * Get a new TableRow with the provided contents - two columns
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
     * Get a new TableRow with the provided contents - two columns
     * 
     * @param activity the FragmentActivity the TableLayout is being displayed on
     * @param cell1 a string resource id for the first cell
     * @param cell2 text for the second cell
     * @param isUrl if true don't allow C&amp;P on the values so that they can be clicked on
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
        cell.setTextIsSelectable(true);
        tr.addView(cell);
        TableRow.LayoutParams trp = new TableRow.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
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
     * @param isUrl if true don't allow C&amp;P on the values so that they can be clicked on
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
            if (!isUrl) {
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
        TableRow.LayoutParams trp = new TableRow.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1);
        trp.span = 3;
        v.setLayoutParams(trp);
        v.setBackgroundColor(Color.rgb(204, 204, 204));
        tr.addView(v);
        return tr;
    }

    /**
     * Display a longer, potential multi-line text over the full width
     * 
     * @param activity the FragmentActivity the TableLayout is being displayed on
     * @param text the text
     * @param tp LayoutParams for the row
     * @return a TableRow
     */
    @NonNull
    public static View createFullRowTitle(@NonNull FragmentActivity activity, @NonNull String text, @NonNull android.widget.TableLayout.LayoutParams tp) {
        TableRow tr = new TableRow(activity);
        TextView cell = new TextView(activity);
        cell.setText(text);
        cell.setSingleLine();
        cell.setTypeface(null, Typeface.BOLD);
        cell.setTextIsSelectable(true);
        tr.addView(cell);

        TableRow.LayoutParams trlp = (LayoutParams) cell.getLayoutParams();
        trlp.span = 3;
        trlp.weight = 1;
        cell.setLayoutParams(trlp);

        tr.setLayoutParams(tp);
        return tr;
    }

    /**
     * Display a longer, potential multi-line text over the full width
     * 
     * @param activity the FragmentActivity the TableLayout is being displayed on
     * @param text the text
     * @param tp LayoutParams for the row
     * @return a TableRow
     */
    @NonNull
    public static View createFullRow(@NonNull FragmentActivity activity, @NonNull String text, @NonNull android.widget.TableLayout.LayoutParams tp) {
        TableRow tr = new TableRow(activity);
        TextView cell = new TextView(activity);
        cell.setText(text);
        cell.setTextIsSelectable(true);
        tr.addView(cell);

        TableRow.LayoutParams trlp = (LayoutParams) cell.getLayoutParams();
        trlp.span = 3;
        trlp.weight = 1;
        cell.setLayoutParams(trlp);

        tr.setLayoutParams(tp);
        return tr;
    }
}
