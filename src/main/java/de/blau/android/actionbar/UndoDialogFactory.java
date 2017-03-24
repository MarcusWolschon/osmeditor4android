package de.blau.android.actionbar;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog.Builder;
import android.text.Html;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import de.blau.android.Logic;
import de.blau.android.Main;
import de.blau.android.R;
import de.blau.android.osm.UndoStorage;
import de.blau.android.util.Density;
import de.blau.android.util.ThemeUtils;

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
				main.supportInvalidateOptionsMenu();
			}
		});
		dialog.show();
	}
	
	private static class UndoAdapter extends ArrayAdapter<UndoDialogItem> {
		public UndoAdapter(Context context, List<UndoDialogItem> objects) {
			super(context, android.R.layout.simple_list_item_1, objects);
		}

		@NonNull
		@Override
		public View getView(int position, View convertView, @NonNull ViewGroup parent) {
			return getItem(position);
		}
		
	}
	
	private static class UndoDialogItem extends TextView {

		public final int index;
		public final boolean isRedo;

		private UndoDialogItem(Context ctx, int index, boolean isRedo, String name) {
			super(ctx);
			Resources r = ctx.getResources();
			setText(Html.fromHtml(r.getString(isRedo ? R.string.redo : R.string.undo) + ": " + name));
			int pad = Density.dpToPx(15);
			setPadding(pad, pad, pad, pad);
			setCompoundDrawablePadding(pad);
			setCompoundDrawablesWithIntrinsicBounds(isRedo ? ThemeUtils.getResIdFromAttribute(ctx,R.attr.undolist_redo) : ThemeUtils.getResIdFromAttribute(ctx,R.attr.undolist_undo), 0, 0, 0);

			this.index = index;
			this.isRedo = isRedo;
		}
	}

}
