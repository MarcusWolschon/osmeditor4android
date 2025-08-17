package de.blau.android.dialogs;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.TextUtils.TruncateAt;
import android.text.method.LinkMovementMethod;
import android.text.style.StyleSpan;
import android.text.util.Linkify;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TableRow.LayoutParams;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.blau.android.R;
import de.blau.android.util.ThemeUtils;

/**
 * Add rows to a table layout
 * 
 * @author Simon Poole
 *
 */
public final class TableLayoutUtils {
    static final int FIRST_CELL_WIDTH     = 5;
    static final int MAX_FIRST_CELL_WIDTH = 12;

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
     * @param context an Android Context
     * @param cell1 text for the first cell
     * @param cell2 text for the second cell
     * @param tp LayoutParams for the row
     * @return a TableRow
     */
    @SuppressLint("NewApi")
    @NonNull
    public static TableRow createRow(@NonNull Context context, String cell1, CharSequence cell2, @NonNull TableLayout.LayoutParams tp) {
        return createRow(context, cell1, null, cell2, false, tp, R.attr.colorAccent, R.color.material_teal);
    }

    /**
     * Get a new TableRow with the provided contents - three columns
     * 
     * If cell2 and cell3 are null cell1 will be considered a full width heading, if cell2 != cell3 then cell1 will be
     * highlighted
     * 
     * @param context an Android Context
     * @param cell1 text for the first cell
     * @param cell2 text for the second cell
     * @param cell3 text for the third cell
     * @param tp LayoutParams for the row
     * @return a TableRow
     */
    @SuppressLint("NewApi")
    @NonNull
    public static TableRow createRow(@NonNull Context context, String cell1, @Nullable CharSequence cell2, @Nullable CharSequence cell3,
            @NonNull TableLayout.LayoutParams tp) {
        return createRow(context, cell1, cell2, cell3, false, tp, R.attr.colorAccent, R.color.material_teal);
    }

    /**
     * Get a new TableRow with the provided contents - three columns
     * 
     * If cell2 and cell3 are null cell1 will be considered a full width heading, if cell2 != cell3 then cell1 will be
     * highlighted
     * 
     * @param context an Android Context
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
    public static TableRow createRow(@NonNull Context context, String cell1, @Nullable CharSequence cell2, @Nullable CharSequence cell3,
            @NonNull TableLayout.LayoutParams tp, int highlightColorAttr, int highlightColorFallback) {
        return createRow(context, cell1, cell2, cell3, false, tp, highlightColorAttr, highlightColorFallback);
    }

    /**
     * Get a new TableRow with the provided contents - three columns
     * 
     * If cell2 and cell3 are null cell1 will be considered a full width heading, if cell2 != cell3 then cell1 will be
     * highlighted
     * 
     * @param context an Android Context
     * @param cell1 text for the first cell
     * @param cell2 text for the second cell
     * @param isUrl if true don't allow C&amp;P on the values so that they can be clicked on
     * @param tp LayoutParams for the row
     * @return a TableRow
     */
    @SuppressLint("NewApi")
    @NonNull
    public static TableRow createRow(@NonNull Context context, @NonNull String cell1, @Nullable CharSequence cell2, boolean isUrl,
            @NonNull TableLayout.LayoutParams tp) {
        return createRow(context, cell1, null, cell2, isUrl, tp, R.attr.colorAccent, R.color.material_teal);
    }

    /**
     * Get a new TableRow with the provided contents - three columns
     * 
     * If cell2 and cell3 are null cell1 will be considered a full width heading, if cell2 != cell3 then cell1 will be
     * highlighted
     * 
     * @param context an Android Context
     * @param cell1 a string resource id for the first cell
     * @param cell2 text for the second cell
     * @param cell3 text for the third cell
     * @param tp LayoutParams for the row
     * @return a TableRow
     */
    @SuppressLint("NewApi")
    @NonNull
    public static TableRow createRow(@NonNull Context context, int cell1, CharSequence cell2, CharSequence cell3, @NonNull TableLayout.LayoutParams tp) {
        return createRow(context, cell1, cell2, cell3, false, tp);
    }

    /**
     * Get a new TableRow with the provided contents - three columns
     * 
     * If cell2 and cell3 are null cell1 will be considered a full width heading, if cell2 != cell3 then cell1 will be
     * highlighted
     * 
     * @param context an Android Context
     * @param cell1 a string resource id for the first cell
     * @param cell2 text for the second cell
     * @param cell3 text for the third cell
     * @param isUrl if true don't allow C&amp;P on the values so that they can be clicked on
     * @param tp LayoutParams for the row
     * @return a TableRow
     */
    @SuppressLint("NewApi")
    @NonNull
    public static TableRow createRow(@NonNull Context context, int cell1, @Nullable CharSequence cell2, @Nullable CharSequence cell3, boolean isUrl,
            @NonNull TableLayout.LayoutParams tp) {
        return createRow(context, context.getString(cell1), cell2, cell3, isUrl, tp);
    }

    /**
     * Get a new TableRow with the provided contents - three columns
     * 
     * If cell2 and cell3 are null cell1 will be considered a full width heading, if cell2 != cell3 then cell1 will be
     * highlighted
     * 
     * @param context an Android Context
     * @param cell1 text for the first cell
     * @param cell2 text for the second cell
     * @param cell3 text for the third cell
     * @param isUrl if true don't allow C&amp;P on the values so that they can be clicked on
     * @param tp LayoutParams for the row
     * @return a TableRow
     */
    @SuppressLint("NewApi")
    @NonNull
    public static TableRow createRow(@NonNull Context context, @NonNull String cell1, @Nullable CharSequence cell2, @Nullable CharSequence cell3, boolean isUrl,
            @NonNull TableLayout.LayoutParams tp) {
        return createRow(context, cell1, cell2, cell3, isUrl, tp, R.attr.colorAccent, R.color.material_teal);
    }

    /**
     * Get a new TableRow with the provided contents - three columns
     * 
     * If cell2 and cell3 are null cell1 will be considered a full width heading, if cell2 != cell3 then cell1 will be
     * highlighted
     * 
     * @param context an Android Context
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
    public static TableRow createRow(@NonNull Context context, @NonNull String cell1, @Nullable CharSequence cell2, @Nullable CharSequence cell3, boolean isUrl,
            @NonNull TableLayout.LayoutParams tp, int highlightColorAttr, int highlightColorFallback) {
        TableRow tr = new TableRow(context);
        TextView cell = new TextView(context);
        cell.setSingleLine();
        cell.setMinEms(FIRST_CELL_WIDTH);

        SpannableString span = new SpannableString(cell1);
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
            ThemeUtils.setSpanColor(context, span, highlightColorAttr, highlightColorFallback);
            ThemeUtils.setSpanColor(context, span2, highlightColorAttr, highlightColorFallback);
            ThemeUtils.setSpanColor(context, span3, highlightColorAttr, highlightColorFallback);
            cell.setMaxEms(MAX_FIRST_CELL_WIDTH);
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
            addCell(context, span2, isUrl, tr, null);
            addCell(context, span3, isUrl, tr, null);
        }
        tr.setLayoutParams(tp);
        return tr;
    }

    /**
     * Add headers over the two value columns
     * 
     * @param context an Android Context
     * @param cell2 header 1
     * @param cell3 header 2
     * @param tp layout params
     * @return a TableRow
     */
    @NonNull
    public static TableRow createHeaderRow(@NonNull Context context, @Nullable CharSequence cell2, @Nullable CharSequence cell3,
            @NonNull TableLayout.LayoutParams tp) {
        return createHeaderRow(context, cell2, cell3, tp, false);
    }

    /**
     * Add headers over the two value columns
     * 
     * @param context an Android Context
     * @param cell2 header 1
     * @param cell3 header 2
     * @param tp layout params
     * @param center if true center the headings
     * @return a TableRow
     */
    @NonNull
    public static TableRow createHeaderRow(@NonNull Context context, @Nullable CharSequence cell2, @Nullable CharSequence cell3,
            @NonNull TableLayout.LayoutParams tp, boolean center) {
        TableRow tr = new TableRow(context);
        TextView cell = new TextView(context);
        cell.setSingleLine();
        cell.setMinEms(FIRST_CELL_WIDTH);
        cell.setMaxEms(MAX_FIRST_CELL_WIDTH);

        SpannableString span2 = null;
        if (cell2 != null) {
            span2 = new SpannableString(cell2);
        }
        SpannableString span3 = null;
        if (cell3 != null) {
            span3 = new SpannableString(cell3);
        }

        tr.addView(cell);
        TableRow.LayoutParams trp = new TableRow.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        if (center) {
            trp.gravity = Gravity.CENTER_HORIZONTAL;
        }
        TextView tv2 = addCell(context, span2, false, tr, trp);
        tv2.setTypeface(null, Typeface.BOLD);
        TextView tv3 = addCell(context, span3, false, tr, trp);
        tv3.setTypeface(null, Typeface.BOLD);

        if (center) {
            tv2.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            tv3.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        }

        tr.setLayoutParams(tp);
        return tr;
    }

    /**
     * Get a new TableRow with the provided contents - two columns
     * 
     * @param context an Android Context
     * @param cell1 a string resource id for the first cell
     * @param cell2 text for the second cell
     * @param tp LayoutParams for the row
     * @return a TableRow
     */
    @SuppressLint("NewApi")
    @NonNull
    public static TableRow createRow(@NonNull Context context, int cell1, @Nullable CharSequence cell2, @NonNull TableLayout.LayoutParams tp) {
        return createRow(context, cell1, cell2, false, tp);
    }

    /**
     * Get a new TableRow with the provided contents - two columns
     * 
     * @param context an Android Context
     * @param cell1 a string resource id for the first cell
     * @param cell2 text for the second cell
     * @param isUrl if true don't allow C&amp;P on the values so that they can be clicked on
     * @param tp LayoutParams for the row
     * @return a TableRow
     */
    @SuppressLint("NewApi")
    @NonNull
    public static TableRow createRow(@NonNull Context context, int cell1, @Nullable CharSequence cell2, boolean isUrl, @NonNull TableLayout.LayoutParams tp) {
        return createRow(context, context.getString(cell1), cell2, isUrl, null, tp);
    }

    /**
     * Get a new TableRow with the provided contents - two columns
     * 
     * @param context an Android Context
     * @param cell1 a string for the first cell
     * @param cell2 text for the second cell
     * @param isUrl if true don't allow C&amp;P on the values so that they can be clicked on
     * @param tp LayoutParams for the row
     * @return a TableRow
     */
    @SuppressLint("NewApi")
    @NonNull
    public static TableRow createRow(@NonNull Context context, @NonNull CharSequence cell1, @Nullable CharSequence cell2, boolean isUrl,
            @Nullable View.OnClickListener listener, @NonNull TableLayout.LayoutParams tp) {
        TableRow tr = new TableRow(context);
        TextView cell = new TextView(context);
        cell.setMinEms(FIRST_CELL_WIDTH);
        cell.setMaxEms(MAX_FIRST_CELL_WIDTH);
        cell.setMaxLines(2);
        cell.setText(cell1);
        if (cell2 == null) {
            cell.setTypeface(null, Typeface.BOLD);
        }
        cell.setEllipsize(TruncateAt.MARQUEE);
        tr.addView(cell);
        TableRow.LayoutParams trp = new TableRow.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        if (cell2 == null) {
            trp.span = 2;
        }

        if (listener != null) {
            SpannableString span = new SpannableString(cell2);
            ThemeUtils.setSpanColor(context, span, android.R.attr.textColorLink, R.color.ccc_blue);
            addCell(context, span, isUrl, tr, trp).setOnClickListener(listener);
            tr.setOnClickListener(listener);
        } else {
            addCell(context, cell2, isUrl, tr, trp);
        }
        tr.setLayoutParams(tp);
        return tr;
    }

    /**
     * Add a new cell to a TableRow
     * 
     * @param context an Android Context
     * @param cellText the text to use for the cell
     * @param isUrl if true don't allow C&amp;P on the values so that they can be clicked on
     * @param tr the TableRow to add the cell to
     * @param tp LayoutParams for the row
     * @return the TextView added
     */
    @NonNull
    private static TextView addCell(@NonNull Context context, @Nullable CharSequence cellText, boolean isUrl, @NonNull TableRow tr,
            @Nullable TableRow.LayoutParams tp) {
        TextView cell = new TextView(context);
        if (cellText != null) {
            cell.setText(cellText);
            cell.setMinEms(FIRST_CELL_WIDTH);
            boolean hasLink = Linkify.addLinks(cell, Linkify.WEB_URLS);

            // note order of the following seems to be relevant to enable both selecting and clicking the link
            cell.setTextIsSelectable(true);
            if (isUrl || hasLink) {
                cell.setMovementMethod(LinkMovementMethod.getInstance());
            }

            cell.setPaddingRelative(10, 0, 0, 0);
            cell.setEllipsize(TruncateAt.MARQUEE);

            if (tp != null) {
                cell.setLayoutParams(tp);
            }
            tr.addView(cell);
        }
        return cell;
    }

    /**
     * Get a divider for the TableLayout
     * 
     * @param context an Android Context
     * @return a empty View the width of the TableLayout
     */
    @NonNull
    public static View divider(@NonNull Context context) {
        TableRow tr = new TableRow(context);
        View v = new View(context);
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
     * @param context an Android Context
     * @param text the text
     * @param tp LayoutParams for the row
     * @return a TableRow
     */
    @NonNull
    public static View createFullRowTitle(@NonNull Context context, @NonNull String text, @NonNull android.widget.TableLayout.LayoutParams tp) {
        TableRow tr = new TableRow(context);
        TextView cell = new TextView(context);
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
     * @param context an Android Context
     * @param text the text
     * @param tp LayoutParams for the row
     * @return a TableRow
     */
    @NonNull
    public static View createFullRow(@NonNull Context context, @NonNull String text, @NonNull android.widget.TableLayout.LayoutParams tp) {
        TableRow tr = new TableRow(context);
        TextView cell = new TextView(context);
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

    /**
     * Display three cells with a button in the last one
     * 
     * @param context an Android Context
     * @param res resource id for the text in the first cell
     * @param text the text
     * @param button the ImageButton
     * @param tp LayoutParams for the row
     * @return a TableRow
     */
    @NonNull
    public static View createRowWithButton(@NonNull Context context, int res, @NonNull String text, @NonNull ImageButton button,
            @NonNull android.widget.TableLayout.LayoutParams tp) {
        TableRow tr = new TableRow(context);
        TextView cell = new TextView(context);
        cell.setMinEms(FIRST_CELL_WIDTH);
        cell.setMaxEms(MAX_FIRST_CELL_WIDTH);
        cell.setText(res);
        tr.addView(cell);

        TableRow.LayoutParams trlp = new TableRow.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        addCell(context, text, false, tr, trlp);

        TableRow.LayoutParams trlp2 = new TableRow.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        trlp2.gravity = Gravity.TOP | Gravity.END;
        trlp2.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        button.setLayoutParams(trlp2);
        tr.addView(button);
        tr.setLayoutParams(tp);
        return tr;
    }
}
