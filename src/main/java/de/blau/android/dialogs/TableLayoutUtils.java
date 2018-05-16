package de.blau.android.dialogs;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils.TruncateAt;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.view.View;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

public class TableLayoutUtils {
    private static final int FIRST_CELL_WIDTH = 5;

    private static final int MAX_FIRST_CELL_WIDTH = 8;

    @SuppressLint("NewApi")
    public static TableRow createRow(FragmentActivity activity, String cell1, CharSequence cell2, TableLayout.LayoutParams tp) {
        return createRow(activity, cell1, null, cell2, false, tp);
    }

    @SuppressLint("NewApi")
    public static TableRow createRow(FragmentActivity activity, String cell1, CharSequence cell2, CharSequence cell3, TableLayout.LayoutParams tp) {
        return createRow(activity, cell1, cell2, cell3, false, tp);
    }

    @SuppressLint("NewApi")
    public static TableRow createRow(FragmentActivity activity, String cell1, CharSequence cell2, boolean isUrl, TableLayout.LayoutParams tp) {
        return createRow(activity, cell1, null, cell2, isUrl, tp);
    }

    @SuppressLint("NewApi")
    public static TableRow createRow(FragmentActivity activity, String cell1, CharSequence cell2, CharSequence cell3, boolean isUrl,
            TableLayout.LayoutParams tp) {
        TableRow tr = new TableRow(activity);
        TextView cell = new TextView(activity);
        cell.setSingleLine();
        cell.setText(cell1);
        cell.setMinEms(FIRST_CELL_WIDTH);
        cell.setMaxEms(MAX_FIRST_CELL_WIDTH);
        if (cell2 == null && cell3 == null) {
            cell.setTypeface(null, Typeface.BOLD);
        }
        cell.setEllipsize(TruncateAt.MARQUEE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            cell.setTextIsSelectable(true);
        }
        tr.addView(cell);
        addCell(activity, cell2, isUrl, tr, null);
        addCell(activity, cell3, isUrl, tr, null);
        tr.setLayoutParams(tp);
        return tr;
    }

    /**
     * @param activity
     * @param cellText
     * @param isUrl
     * @param tr
     */
    private static void addCell(FragmentActivity activity, CharSequence cellText, boolean isUrl, TableRow tr, TableRow.LayoutParams tp) {
        TextView cell;
        cell = new TextView(activity);
        if (cellText != null) {
            cell.setText(cellText);
            cell.setMinEms(FIRST_CELL_WIDTH);
            // cell.setSingleLine(true);
            // cell.setEllipsize(TextUtils.TruncateAt.END);
            Linkify.addLinks(cell, Linkify.WEB_URLS);
            cell.setMovementMethod(LinkMovementMethod.getInstance());
            cell.setPadding(5, 0, 0, 0);
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

    @SuppressLint("NewApi")
    public static TableRow createRow(FragmentActivity activity, int cell1, CharSequence cell2, CharSequence cell3, TableLayout.LayoutParams tp) {
        return createRow(activity, cell1, cell2, cell3, false, tp);
    }

    @SuppressLint("NewApi")
    public static TableRow createRow(FragmentActivity activity, int cell1, CharSequence cell2, CharSequence cell3, boolean isUrl, TableLayout.LayoutParams tp) {
        TableRow tr = new TableRow(activity);
        TextView cell = new TextView(activity);
        cell.setMinEms(FIRST_CELL_WIDTH);
        cell.setMaxEms(MAX_FIRST_CELL_WIDTH);
        cell.setMaxLines(2);
        cell.setText(cell1);
        if (cell2 == null && cell3 == null) {
            cell.setTypeface(null, Typeface.BOLD);
        }
        cell.setEllipsize(TruncateAt.MARQUEE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            cell.setTextIsSelectable(true);
        }
        tr.addView(cell);
        addCell(activity, cell2, isUrl, tr, null);
        addCell(activity, cell3, isUrl, tr, null);
        tr.setLayoutParams(tp);
        return tr;
    }

    @SuppressLint("NewApi")
    public static TableRow createRow(FragmentActivity activity, int cell1, CharSequence cell2, TableLayout.LayoutParams tp) {
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
        trp.span = 2;
        addCell(activity, cell2, false, tr, trp);
        tr.setLayoutParams(tp);
        return tr;
    }

    public static View divider(FragmentActivity activity) {
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
