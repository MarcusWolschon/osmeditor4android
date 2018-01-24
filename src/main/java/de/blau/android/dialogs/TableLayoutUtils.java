package de.blau.android.dialogs;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.Typeface;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
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
        TableRow tr = new TableRow(activity);
        TextView cell = new TextView(activity);
        cell.setSingleLine();
        cell.setText(cell1);
        cell.setMinEms(FIRST_CELL_WIDTH);
        cell.setMaxEms(MAX_FIRST_CELL_WIDTH);
        if (cell2 == null) {
            cell.setTypeface(null, Typeface.BOLD);
        }
        cell.setEllipsize(TruncateAt.MARQUEE);
        tr.addView(cell);
        cell = new TextView(activity);
        if (cell2 != null) {
            cell.setText(cell2);
            cell.setMinEms(FIRST_CELL_WIDTH);
            // cell.setHorizontallyScrolling(true);
            // cell.setSingleLine(true);
            cell.setEllipsize(TextUtils.TruncateAt.END);
            Linkify.addLinks(cell, Linkify.WEB_URLS);
            cell.setMovementMethod(LinkMovementMethod.getInstance());
            cell.setPadding(5, 0, 0, 0);
            cell.setEllipsize(TruncateAt.MARQUEE);
            // This stops links from working
            // if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            // cell.setTextIsSelectable(true);
            // }
            tr.addView(cell);
        }
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
        tr.addView(cell);
        cell = new TextView(activity);
        if (cell2 != null) {
            cell.setText(cell2);
            cell.setMinEms(FIRST_CELL_WIDTH);
            Linkify.addLinks(cell, Linkify.WEB_URLS);
            cell.setMovementMethod(LinkMovementMethod.getInstance());
            cell.setPadding(5, 0, 0, 0);
            cell.setEllipsize(TruncateAt.MARQUEE);
            // This stops links from working
            // if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            // cell.setTextIsSelectable(true);
            // }
            tr.addView(cell);
        }
        tr.setLayoutParams(tp);
        return tr;
    }

    public static View divider(FragmentActivity activity) {
        TableRow tr = new TableRow(activity);
        View v = new View(activity);
        TableRow.LayoutParams trp = new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, 1);
        trp.span = 2;
        v.setLayoutParams(trp);
        v.setBackgroundColor(Color.rgb(204, 204, 204));
        tr.addView(v);
        return tr;
    }
}
