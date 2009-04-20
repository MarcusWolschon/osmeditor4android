package de.blau.android.thread;

import android.app.Activity;
import android.content.res.Resources.NotFoundException;
import android.os.Handler;
import android.view.View;
import de.blau.android.DialogFactory;
import de.blau.android.osm.BoundingBox;
import de.blau.android.osm.StorageDelegator;

public class LoadFromStorageThread extends LogicThread {

	private final StorageDelegator delegator;

	private final BoundingBox boxForEmptyData;

	private final BoundingBox viewBox;

	private final Runnable loadingDone = new Runnable() {
		public void run() {
			View map = caller.getCurrentFocus();
			viewBox.setRatio((float) map.getWidth() / map.getHeight());
			map.invalidate();
			caller.dismissDialog(DialogFactory.PROGRESS_LOADING);
		}
	};

	public LoadFromStorageThread(final Activity caller, final Handler handler, final StorageDelegator delegator,
			 final BoundingBox boxForEmptyData, final BoundingBox viewBox) {
		super(caller, handler);
		this.delegator = delegator;
		this.boxForEmptyData = boxForEmptyData;
		this.viewBox = viewBox;
	}

	@Override
	public void run() {
		try {
			delegator.loadFromStorage();
			if (boxForEmptyData != null && delegator.isEmpty()) {
				delegator.setBoundingBox(boxForEmptyData);
			}
			viewBox.setBorders(delegator.getOriginalBox());
		} catch (NotFoundException e) {
			e.printStackTrace();
		} finally {
			handler.post(loadingDone);
		}
	}

}
