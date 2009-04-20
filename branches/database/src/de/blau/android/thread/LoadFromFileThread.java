package de.blau.android.thread;

import java.io.IOException;

import android.app.Activity;
import android.content.res.Resources.NotFoundException;
import android.os.Handler;
import android.view.View;
import android.view.Window;
import de.blau.android.DialogFactory;
import de.blau.android.Logic;
import de.blau.android.R;
import de.blau.android.osm.BoundingBox;
import de.blau.android.osm.StorageDelegator;
import de.blau.android.resources.Paints;

public class LoadFromFileThread extends LogicThread {

	private final StorageDelegator delegator;

	private final BoundingBox viewBox;

	private final Paints paints;

	private final Runnable loadingDone = new Runnable() {
		public void run() {
			View map = caller.getCurrentFocus();
			caller.dismissDialog(DialogFactory.PROGRESS_LOADING);
			caller.getWindow().setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.menu_move);
			viewBox.setRatio((float) map.getWidth() / map.getHeight());
			paints.updateStrokes((Logic.STROKE_FACTOR / viewBox.getWidth()));
			map.invalidate();
		}
	};

	public LoadFromFileThread(final Activity caller, final Handler handler, final StorageDelegator delegator,
			final BoundingBox viewBox, final Paints paints) {
		super(caller, handler);
		this.delegator = delegator;
		this.viewBox = viewBox;
		this.paints = paints;
	}

	@Override
	public void run() {
		try {
			// TODO Needs to reimplemented if needed
			// delegator.readFromFile(caller.getApplicationContext());
			// viewBox.setBorders(delegator.getOriginalBox());
		} catch (NotFoundException e) {
			e.printStackTrace();
			//exceptions.add(e);
		// } catch (IOException e) {
		// 	e.printStackTrace();
			//exceptions.add(e);
		// } catch (ClassNotFoundException e) {
		//	e.printStackTrace();
			//exceptions.add(e);
		} catch (Exception e) {
			e.printStackTrace();
			//exceptions.add(e);
		} finally {
			handler.post(loadingDone);
		}
	}

}
