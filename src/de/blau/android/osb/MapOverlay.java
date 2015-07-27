package de.blau.android.osb;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Bitmap.Config;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;
import de.blau.android.Application;
import de.blau.android.Map;
import de.blau.android.R;
import de.blau.android.osm.BoundingBox;
import de.blau.android.osm.Server;
import de.blau.android.resources.Profile;
import de.blau.android.util.Density;
import de.blau.android.util.GeoMath;
import de.blau.android.util.IssueAlert;
import de.blau.android.views.IMapView;
import de.blau.android.views.overlay.OpenStreetMapViewOverlay;

public class MapOverlay extends OpenStreetMapViewOverlay {
	
	/** Maximum closed age to display: 7 days. */
	private static final long MAX_CLOSED_AGE = 7 * 24 * 60 * 60 * 1000;
	
	/** viewbox needs to be less wide than this for displaying bugs, just to avoid querying the whole world for bugs */ 
	private static final int TOLERANCE_MIN_VIEWBOX_WIDTH = 40000 * 32;
	
	/** Previously requested area. */
	private Rect prev;
	
	/** Current area. */
	private Rect cur;
	
	/** Paint for open bugs. */
	private final Paint openPaint;
	
	/** Paint for closed bugs. */
	private final Paint closedPaint;
	
	private Bitmap cachedIconClosed;
	private Bitmap cachedIconOpen;
	
	/** Map this is an overlay of. */
	private final Map map;
	
	/** Bugs visible on the overlay. */
	private BugStorage bugs = Application.getBugStorage();
	
	/** Event handlers for the overlay. */
	private final Handler handler;
	
	private Server server;
	
	public MapOverlay(final Map map, Server s) {
		this.map = map;
		server = s;
		prev = new Rect();
		cur = new Rect();
		// bugs = new ArrayList<Bug>();
		handler = new Handler();
		openPaint = Profile.getCurrent(Profile.OPEN_NOTE).getPaint();
		closedPaint = Profile.getCurrent(Profile.CLOSED_NOTE).getPaint();
	}
	
	@Override
	public boolean isReadyToDraw() {
		if (map.getPrefs().isOpenStreetBugsEnabled()) {
			return map.getOpenStreetMapTilesOverlay().isReadyToDraw();
		}
		return true;
	}
	
	@Override
	protected void onDraw(Canvas c, IMapView osmv) {
		if (map.getPrefs().isOpenStreetBugsEnabled()) {
			
			// the idea is to have the circles a bit bigger when zoomed in, not so
			// big when zoomed out
			final float radius = Density.dpToPx(1.0f + osmv.getZoomLevel() / 2.0f);
			BoundingBox bb = osmv.getViewBox();
			
//			if ((bb.getWidth() > TOLERANCE_MIN_VIEWBOX_WIDTH) || (bb.getHeight() > TOLERANCE_MIN_VIEWBOX_WIDTH)) {
//				return;
//			}
			cur.set(bb.getLeft(), bb.getTop(), bb.getRight(), bb.getBottom());

			// draw all the bugs on the map as slightly transparent circles
			int w = Application.mainActivity.getMap().getWidth();
			int h = Application.mainActivity.getMap().getHeight();
			ArrayList<Bug> bugList = bugs.getBugs(bb);
			if (bugList != null) {
				for (Bug b : bugList) {
					float x = GeoMath.lonE7ToX(w , bb, b.getLon());
					float y = GeoMath.latE7ToY(h, w, bb, b.getLat()); 

					if (b.isClosed()) {
						if (cachedIconClosed == null) {
							cachedIconClosed = BitmapFactory.decodeResource(map.getContext().getResources(), R.drawable.bug_closed);
						}
						c.drawBitmap(cachedIconClosed, x, y, null); // FIXME icon should be centered on coordinates, probably
						// c.drawCircle(x, y, radius, b.isClosed() ? closedPaint : openPaint);
					} else {
						if (cachedIconOpen == null) {
							cachedIconOpen = BitmapFactory.decodeResource(map.getContext().getResources(), R.drawable.bug_open);
						}
						c.drawBitmap(cachedIconOpen, x, y, null); // FIXME icon should be centered on coordinates, probably
					}
				}
			}
		}
	}

	@Override
	protected void onDrawFinished(Canvas c, IMapView osmv) {
		// do nothing
	}
	
	/**
	 * Given screen coordinates, find all nearby bugs.
	 * @param x Screen X-coordinate.
	 * @param y Screen Y-coordinate.
	 * @param viewBox Map view box.
	 * @return List of bugs close to given location.
	 */
	public List<Bug> getClickedBugs(final float x, final float y, final BoundingBox viewBox) {
		List<Bug> result = new ArrayList<Bug>();
		if (map.getPrefs().isOpenStreetBugsEnabled()) {
			final float tolerance = Profile.getCurrent().nodeToleranceValue;
			ArrayList<Bug> bugList = bugs.getBugs(viewBox);
			if (bugList != null) {
				for (Bug b : bugList) {
					int lat = b.getLat();
					int lon = b.getLon();
					float differenceX = Math.abs(GeoMath.lonE7ToX(map.getWidth(), viewBox, lon) - x);
					float differenceY = Math.abs(GeoMath.latE7ToY(map.getHeight(), map.getWidth(), viewBox, lat) - y);
					if ((differenceX <= tolerance) && (differenceY <= tolerance)) {
						if (Math.hypot(differenceX, differenceY) <= tolerance) {
							result.add(b);
						}
					}
				}
			}
			// For debugging the OSB editor when the OSB site is down:
			//result.add(new Bug(GeoMath.yToLatE7(map.getHeight(), viewBox, y), GeoMath.xToLonE7(map.getWidth(), viewBox, x), true));
		}
		return result;
	}
}
