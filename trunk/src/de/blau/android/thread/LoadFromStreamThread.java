package de.blau.android.thread;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import android.app.Activity;
import android.os.Handler;
import android.util.Log;
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
			// showDialog() seems to use a separate thread to set up the dialog, and if
			// this operation completes too quickly, dismissDialog() throws an
			// IllegalArgumentException "no dialog with ID was ever shown"
			boolean dismissed = false;
			long giveUp = System.currentTimeMillis() + 3000;
			while (!dismissed && System.currentTimeMillis() < giveUp) {
				try {
					caller.dismissDialog(DialogFactory.PROGRESS_LOADING);
					dismissed = true;
				} catch (IllegalArgumentException e) {
					try {
						sleep(100);
					} catch (InterruptedException e2) {
						// sleep cut short - not a problem
					}
				}
			}
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
		try {
			final OsmParser osmParser = new OsmParser();
			osmParser.start(in);
			delegator.reset();
			delegator.setCurrentStorage(osmParser.getStorage());
			if (boxForEmptyData != null && delegator.isEmpty()) {
				delegator.setOriginalBox(boxForEmptyData);
			}
			viewBox.setBorders(delegator.getOriginalBox());
		} catch (SAXException e) {
			Log.e("Vespucci", "Problem parsing", e);
		} catch (IOException e) {
			Log.e("Vespucci", "Problem parsing", e);
		} catch (ParserConfigurationException e) {
			Log.e("Vespucci", "Problem parsing", e);
		} finally {
			Server.close(in);
			handler.post(loadingDone);
		}
	}

}
