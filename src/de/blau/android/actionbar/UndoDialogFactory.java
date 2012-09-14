package de.blau.android.actionbar;

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import de.blau.android.Main;
import de.blau.android.R;
import de.blau.android.osm.UndoStorage;

public class UndoDialogFactory {

	public static void showUndoDialog(final Main main, final UndoStorage undo) {
		Builder dialog = new Builder(main);
		dialog.setTitle(R.string.undo);
		
		String[] undoActions = undo.getUndoActions();
		String[] redoActions = undo.getRedoActions();

		List<UndoDialogItem> items = new ArrayList<UndoDialogItem>();
		for (int i = 0; i < redoActions.length; i++) {
			items.add(new UndoDialogItem(main, redoActions.length-i, true, redoActions[i]));
		}
		for (int i = undoActions.length-1; i >= 0; i--) {
			items.add(new UndoDialogItem(main, undoActions.length-i, false, undoActions[i]));
		}
		final UndoAdapter adapter = new UndoAdapter(main, items);
		dialog.setAdapter(adapter, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				UndoDialogItem item = adapter.getItem(which);
				for (int i = 0; i < item.index; i++) {
					if (item.isRedo) {
						undo.redo();
					} else {
						undo.undo();
					}
				}
				dialog.dismiss();
				main.invalidateMap();
			}
		});
		dialog.show();
	}
	
	private static class UndoAdapter extends ArrayAdapter<UndoDialogItem> {
		public UndoAdapter(Context context, List<UndoDialogItem> objects) {
			super(context, android.R.layout.simple_list_item_1, objects);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			return getItem(position);
		}
		
	}
	
	private static class UndoDialogItem extends TextView {

		public final int index;
		public final boolean isRedo;

		private UndoDialogItem(Context ctx, int index, boolean isRedo, String name) {
			super(ctx);
			Resources r = ctx.getResources();
			setText(r.getString(isRedo ? R.string.redo : R.string.undo) + ": " + name);
			int pad = dpToPx(ctx, 15);
			setPadding(pad, pad, pad, pad);
			setTextColor(r.getColor(android.R.color.primary_text_dark));
			setBackgroundColor(Color.DKGRAY);

			this.index = index;
			this.isRedo = isRedo;
		}
	}
	
	/**
	 * Converts a size in dp to pixels
	 * @param dp size in display point
	 * @return size in pixels (for the current display metrics)
	 */
	private static int dpToPx(Context ctx, int dp) {
		return Math.round(dp * ctx.getResources().getDisplayMetrics().density);
	}

}
