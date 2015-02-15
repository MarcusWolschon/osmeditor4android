package de.blau.android.actionbar;

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import de.blau.android.Logic;
import de.blau.android.Main;
import de.blau.android.R;
import de.blau.android.osm.UndoStorage;
import de.blau.android.util.Density;

public class UndoDialogFactory {

	public static void showUndoDialog(final Main main, final Logic logic, final UndoStorage undo) {
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
						logic.redo();
					} else {
						logic.undo();
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
			int pad = Density.dpToPx(15);
			setPadding(pad, pad, pad, pad);
			setTextColor(r.getColor(isRedo ? android.R.color.primary_text_light : android.R.color.primary_text_dark));
			setBackgroundColor(r.getColor(isRedo ? android.R.color.background_light : android.R.color.background_dark));
			setCompoundDrawablePadding(pad);
			setCompoundDrawablesWithIntrinsicBounds(isRedo ? R.drawable.undolist_redo : R.drawable.undolist_undo, 0, 0, 0);

			this.index = index;
			this.isRedo = isRedo;
		}
	}

}
