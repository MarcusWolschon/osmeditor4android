package de.blau.android.thread;

import java.io.InputStream;

import android.app.Activity;
import android.content.res.Resources.NotFoundException;
import android.os.Handler;
import android.view.View;
import de.blau.android.DialogFactory;
import de.blau.android.osm.BoundingBox;
import de.blau.android.osm.OsmParser;
import de.blau.android.osm.Server;
import de.blau.android.osm.StorageDelegator;

public class LoadFromStreamThread extends LogicThread {

	private final StorageDelegator delegator;

	private final InputStream in;

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

	public LoadFromStreamThread(final Activity caller, final Handler handler, final StorageDelegator delegator,
			final InputStream in, final BoundingBox boxForEmptyData, final BoundingBox viewBox) {
		super(caller, handler);
		this.delegator = delegator;
		this.in = in;
		this.boxForEmptyData = boxForEmptyData;
		this.viewBox = viewBox;
	}

	@Override
	public void run() {
		final OsmParser osmParser = new OsmParser();
		try {
			osmParser.start(in);
			delegator.reset();
			delegator.setCurrentStorage(osmParser.getStorage());
			if (boxForEmptyData != null && delegator.isEmpty()) {
				delegator.setOriginalBox(boxForEmptyData);
			}
			viewBox.setBorders(delegator.getOriginalBox());
		} catch (NotFoundException e) {
			e.printStackTrace();
			//exceptions.add(e);
		} finally {
			Server.close(in);
			handler.post(loadingDone);
		}
	}

}
