package de.blau.android;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.WeakHashMap;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import de.blau.android.Logic.Mode;
import de.blau.android.exception.OsmException;
import de.blau.android.filter.Filter;
import de.blau.android.osm.BoundingBox;
import de.blau.android.osm.GeoPoint;
import de.blau.android.osm.GeoPoint.InterruptibleGeoPoint;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Relation;
import de.blau.android.osm.StorageDelegator;
import de.blau.android.osm.Tags;
import de.blau.android.osm.Way;
import de.blau.android.prefs.Preferences;
import de.blau.android.presets.Preset;
import de.blau.android.presets.Preset.PresetItem;
import de.blau.android.resources.DataStyle;
import de.blau.android.resources.DataStyle.FeatureStyle;
import de.blau.android.resources.TileLayerServer;
import de.blau.android.services.TrackerService;
import de.blau.android.util.Density;
import de.blau.android.util.GeoMath;
import de.blau.android.util.Offset;
import de.blau.android.util.collections.LongHashSet;
import de.blau.android.views.IMapView;
import de.blau.android.views.overlay.MapOverlayTilesOverlay;
import de.blau.android.views.overlay.MapTilesOverlay;
import de.blau.android.views.overlay.MapViewOverlay;

/**
 * Paints all data provided previously by {@link Logic}.<br/>
 * As well as a number of overlays.
 * There is a default overlay that fetches rendered tiles
 * from an OpenStreetMap-server.
 * 
 * @author mb 
 * @author Marcus Wolschon <Marcus@Wolschon.biz>
 */

public class Map extends View implements IMapView {
	
	@SuppressWarnings("unused")
	private static final String DEBUG_TAG = Map.class.getSimpleName();

	public static final int ICON_SIZE_DP = 20;
	
	/** Use reflection to access Canvas method only available in API11. */
	private static final Method mIsHardwareAccelerated;

	private static final int HOUSE_NUMBER_RADIUS = 10;
	
	/**
	 * zoom level from which on we display data
	 */
	private static final int SHOW_DATA_LIMIT = 12; 
	
	/**
	 * zoom level from which on we display icons and house numbers
	 */
	private static final int SHOW_ICONS_LIMIT = 15;
	
	/** half the width/height of a node icon in px */
	private final int iconRadius;
	
	private final int iconSelectedBorder;
	
	private final int houseNumberRadius;
	
	private final int verticalNumberOffset;

    private final ArrayList<BoundingBox> boundingBoxes = new ArrayList<BoundingBox>();

	private Preferences prefs;
	
	/** Direction we're pointing. 0-359 is valid, anything else is invalid.*/
	private float orientation = -1f;
	
	/**
	 * List of Overlays we are showing.<br/>
	 * This list is initialized to contain only one
	 * {@link MapTilesOverlay} at construction-time but
	 * can be changed to contain additional overlays later.
	 * @see #getOverlays()
	 */
	final List<MapViewOverlay> mOverlays = Collections.synchronizedList(new ArrayList<MapViewOverlay>());
	
	/**
	 * The visible area in decimal-degree (WGS84) -space.
	 */
	private BoundingBox myViewBox;
	
	private StorageDelegator delegator;
	
	/**
	 * show icons for POIs (in a wide sense of the word)
	 */
	private boolean showIcons = false;
	
	/**
	 * show icons for POIs tagged on (closed) ways
	 */
	private boolean showWayIcons = false;
	
	/**
	 * Always darken non-downloaded areas
	 */
	private boolean alwaysDrawBoundingBoxes = false;
	
	/**
	 * Stores icons that apply to a certain "thing". This can be e.g. a node or a SortedMap of tags.
	 */
	private final WeakHashMap<Object, Bitmap> iconcache = new WeakHashMap<Object, Bitmap>();
	
	/**
	 * Stores strings that apply to a certain "thing". This can be e.g. a node or a SortedMap of tags.
	 */
	private final WeakHashMap<Object, String> labelcache = new WeakHashMap<Object, String>();
	
	/** Caches if the map is zoomed into edit range during one onDraw pass */
	private boolean tmpDrawingInEditRange;

	/** Caches the edit mode during one onDraw pass */
	private Logic.Mode tmpDrawingEditMode;
	
	/** Caches the currently selected nodes during one onDraw pass */
	private List<Node> tmpDrawingSelectedNodes;

	/** Caches the currently selected ways during one onDraw pass */
	private List<Way> tmpDrawingSelectedWays;
	
	/** Caches the current "clickable elements" set during one onDraw pass */
	private Set<OsmElement> tmpClickableElements;

	/** used for highlighting relation members */
	private List<Way> tmpDrawingSelectedRelationWays;
	private List<Node> tmpDrawingSelectedRelationNodes;
	
	/**
	 * Locked or not
	 */
	private boolean tmpLocked;
	
	/**
	 * 
	 */
	private ArrayList<Way> tmpStyledWays = new ArrayList<Way>();
	private ArrayList<Way> tmpHiddenWays = new ArrayList<Way>();
	
	
	/** Caches the preset during one onDraw pass */
	private Preset[] tmpPresets;
	
	/** Caches the Paint used for node tolerance */
    private Paint nodeTolerancePaint;
	private Paint nodeTolerancePaint2;
	
	/** Caches the Paint used for way tolerance */
    private Paint wayTolerancePaint;
	private Paint wayTolerancePaint2;
	
	/** cached zoom level, calculated once per onDraw pass **/
    private int zoomLevel = 0;
	
	/** Cache the current filter **/
	private Filter tmpFilter = null;
		
	/** */
    private boolean inNodeIconZoomRange = false;
	
	/**
	 * We just need one path object
	 */
    private Path path = new Path();
	
	private LongHashSet handles;
	
	private Location displayLocation = null;
	private boolean isFollowingGPS = false;

	private TrackerService tracker;
	
	private Paint textPaint = new Paint();
	
	/**
	 * support for display a crosshairs at a position
	 */
	private boolean showCrosshairs = false;
	private int crosshairsLat = 0;
	private int crosshairsLon = 0;
	
	static {
		Method m;
		try {
			m = Canvas.class.getMethod("isHardwareAccelerated", (Class[])null);
		} catch (NoSuchMethodException e) {
			m = null;
		}
		mIsHardwareAccelerated = m;
	}
	
	private Context context;
	
	private Rect canvasBounds;

	@SuppressLint("NewApi")
	public Map(final Context context) {
		super(context);
		this.context = context;

		canvasBounds = new Rect();
		
		setFocusable(true);
		setFocusableInTouchMode(true);
		
		//Style me
		setBackgroundColor(ContextCompat.getColor(context,R.color.ccc_white));
		setDrawingCacheEnabled(false);
		//
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) { 
			setLayerType(View.LAYER_TYPE_SOFTWARE, null);
		}
		
		iconRadius = Density.dpToPx(ICON_SIZE_DP / 2);
		houseNumberRadius = Density.dpToPx(HOUSE_NUMBER_RADIUS);
		verticalNumberOffset = Density.dpToPx(3);
		iconSelectedBorder = Density.dpToPx(2);
		
		// TODO externalize
		textPaint.setColor(Color.WHITE);
		textPaint.setTypeface(Typeface.SANS_SERIF);
		textPaint.setTextSize(Density.dpToPx(12));
		textPaint.setShadowLayer(1, 0, 0, Color.BLACK);
	}
	
	public void createOverlays(Context ctx)
	{
		// create an overlay that displays pre-rendered tiles from the internet.
		synchronized(mOverlays) {
			if (mOverlays.size() == 0) // only set once
			{
				if (prefs == null) // just to be safe
					mOverlays.add(new MapTilesOverlay(this, TileLayerServer.getDefault(ctx, true), null));
				else {
					// mOverlays.add(new OpenStreetMapTilesOverlay(this, OpenStreetMapTileServer.get(getResources(), prefs.backgroundLayer(), true), null));
					MapTilesOverlay osmto = new MapTilesOverlay(this, TileLayerServer.get(ctx, prefs.backgroundLayer(), true), null);
					// Log.d("Map","background tile renderer " + osmto.getRendererInfo().toString());
					mOverlays.add(osmto);
					mOverlays.add(new MapOverlayTilesOverlay(this));
				}
				mOverlays.add(new de.blau.android.tasks.MapOverlay(this, prefs.getServer()));
				mOverlays.add(new de.blau.android.photos.MapOverlay(this, prefs.getServer()));
				mOverlays.add(new de.blau.android.grid.MapOverlay(this, prefs.getServer()));
			}
		}
	}
	
	public MapTilesOverlay getOpenStreetMapTilesOverlay() {
		synchronized(mOverlays) {
			for (MapViewOverlay osmvo : mOverlays) {
				if ((osmvo instanceof MapTilesOverlay) && !(osmvo instanceof MapOverlayTilesOverlay)) {
					return (MapTilesOverlay)osmvo;
				}
			}
		}
		return null;
	}

	/**
	 * The names of these clases are patently silly and should be refactored
	 * @return
	 */
	public MapOverlayTilesOverlay getOpenStreetMapOverlayTilesOverlay() {
		synchronized(mOverlays) {
			for (MapViewOverlay osmvo : mOverlays) {
				if (osmvo instanceof MapOverlayTilesOverlay) {
					return (MapOverlayTilesOverlay)osmvo;
				}
			}
		}
		return null;

	}

	public de.blau.android.tasks.MapOverlay getOpenStreetBugsOverlay() {
		synchronized(mOverlays) {
			for (MapViewOverlay osmvo : mOverlays) {
				if (osmvo instanceof de.blau.android.tasks.MapOverlay) {
					return (de.blau.android.tasks.MapOverlay)osmvo;
				}
			}
		}
		return null;
	}

	public de.blau.android.photos.MapOverlay getPhotosOverlay() {
		synchronized(mOverlays) {
			for (MapViewOverlay osmvo : mOverlays) {
				if (osmvo instanceof de.blau.android.photos.MapOverlay) {
					return (de.blau.android.photos.MapOverlay)osmvo;
				}
			}
		}
		return null;
	}

	public void onDestroy() {
		synchronized(mOverlays) {
			for (MapViewOverlay osmvo : mOverlays) {
				osmvo.onDestroy();
			}
		}
		tracker = null;
		iconcache.clear();
		labelcache.clear();
		tmpPresets = null;
	}
	
	public void onLowMemory() {
		synchronized(mOverlays) {
			for (MapViewOverlay osmvo : mOverlays) {
				osmvo.onLowMemory();
			}
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onDraw(final Canvas canvas) {
		super.onDraw(canvas);
		long time = System.currentTimeMillis();
		
		zoomLevel = calcZoomLevel(canvas);
		
		// set in paintOsmData now tmpDrawingInEditRange = Main.logic.isInEditZoomRange();
		final Logic logic = App.getLogic();
		tmpDrawingEditMode = logic.getMode();
		tmpFilter = logic.getFilter();
		tmpDrawingSelectedNodes = logic.getSelectedNodes();
		tmpDrawingSelectedWays = logic.getSelectedWays();
		tmpClickableElements = logic.getClickableElements();
		tmpDrawingSelectedRelationWays = logic.getSelectedRelationWays();
		tmpDrawingSelectedRelationNodes = logic.getSelectedRelationNodes();
		tmpPresets = App.getCurrentPresets(context);
		tmpLocked = logic.isLocked();
		
		inNodeIconZoomRange = zoomLevel > SHOW_ICONS_LIMIT;
		
		// handles = null; this forces creation of a new object, simply clear it in paintHandles after use
		
		// Draw our Overlays.
		canvas.getClipBounds(canvasBounds);
		MapTilesOverlay.resetAttributionArea(canvasBounds, 0);
		synchronized(mOverlays) {
			for (MapViewOverlay osmvo : mOverlays) { 
				if (!(osmvo instanceof de.blau.android.tasks.MapOverlay)) {
					osmvo.onManagedDraw(canvas, this);
				}
			}
		}
		
		if (zoomLevel > SHOW_DATA_LIMIT) {
			paintOsmData(canvas);
		}
		getOpenStreetBugsOverlay().onManagedDraw(canvas, this); // draw bugs on top of data
		
		if (zoomLevel > 10) {
			if (tmpDrawingEditMode != Mode.MODE_ALIGN_BACKGROUND) {
				// shallow copy to avoid modification issues
				boundingBoxes.clear();
				boundingBoxes.addAll(delegator.getBoundingBoxes());
				paintStorageBox(canvas, boundingBoxes);
			}
			paintGpsTrack(canvas);
		}
		paintGpsPos(canvas);
		if (tmpDrawingInEditRange)
			paintCrosshairs(canvas);
		
		if (tmpDrawingEditMode == Mode.MODE_ALIGN_BACKGROUND) {
			paintZoomAndOffset(canvas);
		}
		
		time = System.currentTimeMillis() - time;
		
		if (prefs.isStatsVisible()) {
			paintStats(canvas, (int) (1 / (time / 1000f)));
		}
	}
	

	@Override
	protected void onSizeChanged(final int w, final int h, final int oldw, final int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		try {
			myViewBox.setRatio(this, (float) w / h, true);
		} catch (OsmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/* Overlay Event Forwarders */
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		synchronized(mOverlays) {
			for (MapViewOverlay osmvo : mOverlays) {
				if (osmvo.onTouchEvent(event, this)) {
					return true;
				}
			}
		}
		return super.onTouchEvent(event);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		synchronized(mOverlays) {
			for (MapViewOverlay osmvo : mOverlays) {
				if (osmvo.onKeyDown(keyCode, event, this)) {
					return true;
				}
			}
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		synchronized(mOverlays) {
			for (MapViewOverlay osmvo : mOverlays) {
				if (osmvo.onKeyUp(keyCode, event, this)) {
					return true;
				}
			}
		}
		return super.onKeyUp(keyCode, event);
	}
	
	@Override
	public boolean onTrackballEvent(MotionEvent event) {
		synchronized(mOverlays) {
			for (MapViewOverlay osmvo : mOverlays) {
				if (osmvo.onTrackballEvent(event, this)) {
					return true;
				}
			}
		}
		return super.onTrackballEvent(event);
	}
	
	/**
	 * As of Android 4.0.4, clipping with Op.DIFFERENCE is not supported if hardware acceleration is used.
	 * (see http://android-developers.blogspot.de/2011/03/android-30-hardware-acceleration.html)
	 * Op.DIFFERENCE and clipPath supported as of 18
	 * 
	 * !!! FIXME Disable using HW clipping completely for now, see bug https://github.com/MarcusWolschon/osmeditor4android/issues/307
	 * 
	 * @param c Canvas to check
	 * @return true if the canvas supports proper clipping with Op.DIFFERENCE
	 */
	private boolean hasFullClippingSupport(Canvas c) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB /*&& Build.VERSION.SDK_INT < 18*/ && mIsHardwareAccelerated != null) {
			try {
				return !(Boolean)mIsHardwareAccelerated.invoke(c, (Object[])null);
			} catch (IllegalArgumentException e) {
			} catch (IllegalAccessException e) {
			} catch (InvocationTargetException e) {
			}
		}
		// Older versions do not use hardware acceleration
		return true;
	}
	
	private boolean myIsHardwareAccelerated(Canvas c) {
		if (mIsHardwareAccelerated != null) {
			try {
				return (Boolean)mIsHardwareAccelerated.invoke(c, (Object[])null);
			} catch (IllegalArgumentException e) {
			} catch (IllegalAccessException e) {
			} catch (InvocationTargetException e) {
			}
		}
		// Older versions do not use hardware acceleration
		return false;
	}
	

	private void paintCrosshairs(Canvas canvas) {
		// 
		if (showCrosshairs) {
			Paint paint = DataStyle.getCurrent(DataStyle.CROSSHAIRS).getPaint();
			canvas.save();
			canvas.translate(GeoMath.lonE7ToX(getWidth(), getViewBox(), crosshairsLon), GeoMath.latE7ToY(getHeight(), getWidth(), getViewBox(),crosshairsLat));
			canvas.drawPath(DataStyle.getCurrent().crosshairs_path, paint);
			canvas.restore();
		}
	}
	
	private void paintGpsTrack(final Canvas canvas) {
		if (tracker == null) return;
		float[] linePoints = pointListToLinePointsArray(tracker.getTrackPoints());
		canvas.drawLines(linePoints, DataStyle.getCurrent(DataStyle.GPS_TRACK).getPaint());
	}
	
	/**
	 * @param canvas
	 */
	private void paintGpsPos(final Canvas canvas) {
		if (displayLocation == null) return;
		BoundingBox viewBox = getViewBox();
		float x = GeoMath.lonE7ToX(getWidth(), viewBox, (int)(displayLocation.getLongitude() * 1E7));
		float y = GeoMath.latE7ToY(getHeight(), getWidth(), viewBox, (int)(displayLocation.getLatitude() * 1E7)); 
				                
		float o = -1f;
		if (displayLocation.hasBearing() && displayLocation.hasSpeed() && displayLocation.getSpeed() > 1.4f) {
			// 1.4m/s ~= 5km/h ~= walking pace
			// faster than walking pace - use the GPS bearing
			o = displayLocation.getBearing();
		} else {
			// slower than walking pace - use the compass orientation (if available)
			if (orientation >= 0) {
				o = orientation;
			}
		}
		Paint paint = null;
		if (isFollowingGPS) {
			paint = DataStyle.getCurrent(DataStyle.GPS_POS_FOLLOW).getPaint();
		} else {
			paint = DataStyle.getCurrent(DataStyle.GPS_POS).getPaint();
		}
	
		if (o < 0) {
			// no orientation data available
			canvas.drawCircle(x, y, paint.getStrokeWidth(), paint);
		} else {
			// show the orientation using a pointy indicator
			canvas.save();
			canvas.translate(x, y);
			canvas.rotate(o);
			canvas.drawPath(DataStyle.getCurrent().orientation_path, paint);
			canvas.restore();
		}
		if (displayLocation.hasAccuracy()) {
			// FIXME this assumes square pixels
			float accuracyInPixels = (float) (GeoMath.convertMetersToGeoDistance(displayLocation.getAccuracy())*((double)getWidth()/(viewBox.getWidth()/1E7D)));
			RectF accuracyRect = new RectF(x-accuracyInPixels,y+accuracyInPixels,x+accuracyInPixels,y-accuracyInPixels);
			canvas.drawOval(accuracyRect, DataStyle.getCurrent(DataStyle.GPS_ACCURACY).getPaint());	
		}
	}
	
	private void paintStats(final Canvas canvas, final int fps) {
		int pos = 1;
		String text = "";
		Paint infotextPaint = DataStyle.getCurrent(DataStyle.INFOTEXT).getPaint();
		float textSize = infotextPaint.getTextSize();
		
		BoundingBox viewBox = getViewBox();
		
		text = "viewBox: " + viewBox.toString();
		canvas.drawText(text, 5, getHeight() - textSize * pos++, infotextPaint);
		text = "Relations (current/API) :" + delegator.getCurrentStorage().getRelations().size() + "/"
				+ delegator.getApiRelationCount();
		canvas.drawText(text, 5, getHeight() - textSize * pos++, infotextPaint);
		text = "Ways (current/API) :" + delegator.getCurrentStorage().getWays().size() + "/"
				+ delegator.getApiWayCount();
		canvas.drawText(text, 5, getHeight() - textSize * pos++, infotextPaint);
		text = "Nodes (current/Waynodes/API) :" + delegator.getCurrentStorage().getNodes().size() + "/"
				+ delegator.getCurrentStorage().getWaynodes().size() + "/" + delegator.getApiNodeCount();
		canvas.drawText(text, 5, getHeight() - textSize * pos++, infotextPaint);
		text = "fps: " + fps;
		canvas.drawText(text, 5, getHeight() - textSize * pos++, infotextPaint);
		text = "hardware acceleration: " + (myIsHardwareAccelerated(canvas) ? "on" : "off");
		canvas.drawText(text, 5, getHeight() - textSize * pos++, infotextPaint);
	}
	
	/**
	 * Paint the current tile zoom level and offset ... very ugly
	 * @param canvas
	 */
	private void paintZoomAndOffset(final Canvas canvas) {
		int pos = App.mainActivity.getSupportActionBar().getHeight() + 5; 
		Offset o = getOpenStreetMapTilesOverlay().getRendererInfo().getOffset(zoomLevel);
		String text = "Z " + zoomLevel + " Offset " +  (o != null ? String.format(Locale.US,"%.5f",o.lon) + "/" +  String.format(Locale.US,"%.5f",o.lat) : "0.00000/0.00000");
		float textSize = textPaint.getTextSize();
		canvas.drawText(text, 5, pos + textSize, textPaint);
	}
	
	/**
	 * Paints all OSM data on the given canvas.
	 * 
	 * @param canvas Canvas, where the data shall be painted on.
	 */
	private void paintOsmData(final Canvas canvas) {
		// first find all nodes that we need to display (for density calculations)

		List<Node> paintNodes = delegator.getCurrentStorage().getNodes(getViewBox()); 
		
		// the following should guarantee that if the selected node is off screen but the handle not, the handle gets drawn
		// note this isn't perfect because touch areas of other nodes just outside the screen still won't get drawn
		// TODO check if we can't avoid searching paintNodes multiple times
		if (tmpDrawingSelectedNodes != null) {
			for (Node n:tmpDrawingSelectedNodes) {
				if (!paintNodes.contains(n)) {
					paintNodes.add(n);
				}
			}
		}
		
		// 
		tmpDrawingInEditRange = App.getLogic().isInEditZoomRange(); // do this after density calc
		
		boolean drawTolerance = tmpDrawingInEditRange // if we are not in editing range none of the further checks are necessary
								&& !tmpLocked 
								&& tmpDrawingEditMode.elementsSelectable();
		
		//Paint all ways
		List<Way> ways = delegator.getCurrentStorage().getWays();
		
		boolean filterMode = tmpFilter != null; // we have an active filter
		
		/*
		 Split the ways it to whose that we are going to show and those that we hide, rendering is far simpler for the later 
		 */
		tmpHiddenWays.clear();
		tmpStyledWays.clear();
		for (Way w:ways) {
			if (filterMode) {
				if (tmpFilter.include(w, tmpDrawingInEditRange && tmpDrawingSelectedWays != null && tmpDrawingSelectedWays.contains(w))) {
					tmpStyledWays.add(w);
				} else {
					tmpHiddenWays.add(w);
				}
			} else {
				tmpStyledWays.add(w);
			}
		}
		// draw hidden ways first
		for (Way w:tmpHiddenWays) {
			paintHiddenWay(canvas,w);
		}
		
		boolean displayHandles =
				   tmpDrawingSelectedNodes == null
				&& tmpDrawingSelectedRelationWays == null
				&& tmpDrawingSelectedRelationNodes == null
				&& tmpDrawingEditMode.elementsGeomEditiable();
		Collections.sort(tmpStyledWays,layerComparator);
		for (Way w:tmpStyledWays) {
			paintWay(canvas,w,displayHandles, drawTolerance);
		}
		
		//Paint nodes
		Boolean hwAccelarationWorkaround = myIsHardwareAccelerated(canvas) && Build.VERSION.SDK_INT < 19;
		for (Node n:paintNodes) {
			paintNode(canvas, n, hwAccelarationWorkaround, drawTolerance);
		}
		paintHandles(canvas);
	}
	
	/**
	 * For ordering according to layer value and draw lines on top of areas in the same layer
	 */
    private Comparator<Way> layerComparator = new Comparator<Way>() {
		@Override
		public int compare(Way w1, Way w2) {
			int layer1 = 0;
			int layer2 = 0;
			String layer1Str = w1.getTagWithKey(Tags.KEY_LAYER);
			if (layer1Str!=null) {
				try {
					layer1 = Integer.parseInt(layer1Str);
				} catch (NumberFormatException e) {
					// FIXME should validate here
				}
			}
			String layer2Str = w2.getTagWithKey(Tags.KEY_LAYER);
			if (layer2Str!=null) {
				try {
					layer2 = Integer.parseInt(layer2Str);
				} catch (NumberFormatException e) {
					// FIXME should validate here
				}
			}
			int result = layer2 == layer1 ? 0 : layer2 > layer1 ? +1 : -1;
			if (result == 0) {
				FeatureStyle fs1 = getAndSetStyle(w1);
				Style style1 = fs1.getPaint().getStyle();
				FeatureStyle fs2 = getAndSetStyle(w2);
				Style style2 = fs2.getPaint().getStyle();				
				result = style2 == style1 ? 0 : style2 == Style.STROKE ? -1 : +1;
			}
			return result;
		}
	};
	
	/**
	 * 
	 * @return true if too many nodes are on screen for editing
	 */
	public boolean tooManyNodes() {
		return false;
// TODO code is currently disabled because it is not quite satisfactory
//		return nodesOnScreenCount>(maxOnScreenNodes-unusedNodeSpace);
	}

	private void paintStorageBox(final Canvas canvas, List<BoundingBox> list) {
		if (!tmpLocked || alwaysDrawBoundingBoxes) {
			Canvas c = canvas;
			Bitmap b = null;
			// Clipping with Op.DIFFERENCE is not supported when a device uses hardware acceleration
			// drawing to a bitmap however will currently not be accelerated
			if (!hasFullClippingSupport(canvas)) {
				b = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
				c = new Canvas(b);
			} else {
				c.save();
			}
			int screenWidth = getWidth();
			int screenHeight = getHeight();
			BoundingBox viewBox = getViewBox();
			path.reset();
			RectF screen = new RectF(0, 0, getWidth(), getHeight());
			for (BoundingBox bb:list) {
				if (viewBox.intersects(bb)) { // only need to do this if we are on screen
					float left = GeoMath.lonE7ToX(screenWidth, viewBox, bb.getLeft());
					float right = GeoMath.lonE7ToX(screenWidth, viewBox, bb.getRight());
					float bottom = GeoMath.latE7ToY(screenHeight, screenWidth, viewBox , bb.getBottom());
					float top = GeoMath.latE7ToY(screenHeight, screenWidth, viewBox, bb.getTop());
					RectF rect = new RectF(left, top, right, bottom);
					rect.intersect(screen);
					path.addRect(rect, Path.Direction.CW);
				}
			}

			Paint boxpaint = DataStyle.getCurrent(DataStyle.VIEWBOX).getPaint();
			c.clipPath(path, Region.Op.DIFFERENCE);
			c.drawRect(screen, boxpaint);

			if (!hasFullClippingSupport(canvas)) {
				canvas.drawBitmap(b, 0, 0, null);
			} else {
				c.restore();
			}
		}
	}
	
	/**
	 * Paints the given node on the canvas.
	 * 
	 * @param canvas Canvas, where the node shall be painted on.
	 * @param node Node which shall be painted.
	 * @param hwAccelarationWorkaround TODO
	 * @param drawTolerance 
	 */
	private void paintNode(final Canvas canvas, final Node node, boolean hwAccelarationWorkaround, boolean drawTolerance) {
		int lat = node.getLat();
		int lon = node.getLon();
		boolean isSelected = tmpDrawingSelectedNodes != null && tmpDrawingSelectedNodes.contains(node);

		BoundingBox viewBox = getViewBox();
		float x = GeoMath.lonE7ToX(getWidth(), viewBox, lon);
		float y = GeoMath.latE7ToY(getHeight(), getWidth(), viewBox, lat);

		boolean isTagged = node.isTagged();

		boolean filteredObject = false;
		boolean filterMode = tmpFilter != null; // we have an active filter
		if (filterMode) {
			filteredObject = tmpFilter.include(node, isSelected); 
		}
		
		//draw tolerance
		if (drawTolerance && (!filterMode || (filterMode && filteredObject))) {
			if (prefs.isToleranceVisible() && tmpClickableElements == null) {
				drawNodeTolerance(canvas, node.getState(), lat, lon, isTagged, x, y, nodeTolerancePaint);
			} else if (tmpClickableElements != null && tmpClickableElements.contains(node)) {
				drawNodeTolerance(canvas, node.getState(), lat, lon, isTagged, x, y, nodeTolerancePaint2);
			}
		}

		String featureKey;
		String featureKeyThin;
		String featureKeyTagged;
		if (isSelected && tmpDrawingInEditRange) {
			// general node style
			featureKey = DataStyle.SELECTED_NODE;
			// style for house numbers
			featureKeyThin = DataStyle.SELECTED_NODE_THIN;
			// style for tagged nodes or otherwise important
			featureKeyTagged = DataStyle.SELECTED_NODE_TAGGED;
			if (tmpDrawingSelectedNodes.size() == 1 && tmpDrawingSelectedWays == null && prefs.largeDragArea() && tmpDrawingEditMode.elementsGeomEditiable()) { // don't draw large areas in multi-select mode
				canvas.drawCircle(x, y, DataStyle.getCurrent().largDragToleranceRadius, DataStyle.getCurrent(DataStyle.NODE_DRAG_RADIUS).getPaint());
			}
		} else if ((tmpDrawingSelectedRelationNodes != null && tmpDrawingSelectedRelationNodes.contains(node)) && tmpDrawingInEditRange) {
			// general node style
			featureKey = DataStyle.SELECTED_RELATION_NODE;
			// style for house numbers
			featureKeyThin = DataStyle.SELECTED_RELATION_NODE_THIN;
			// style for tagged nodes or otherwise important
			featureKeyTagged = DataStyle.SELECTED_RELATION_NODE_TAGGED;
			isSelected = true;
		} else if (node.hasProblem(context)) {
			// general node style
			featureKey = DataStyle.PROBLEM_NODE;
			// style for house numbers
			featureKeyThin = DataStyle.PROBLEM_NODE_THIN;
			// style for tagged nodes or otherwise important
			featureKeyTagged = DataStyle.PROBLEM_NODE_TAGGED;
		} else {
			// general node style
			featureKey = DataStyle.NODE;
			// style for house numbers
			featureKeyThin = DataStyle.NODE_THIN;
			// style for tagged nodes or otherwise important
			featureKeyTagged = DataStyle.NODE_TAGGED;
		}

		boolean noIcon = true;		
		boolean isTaggedAndInZoomLimit = isTagged && inNodeIconZoomRange;
		
		if (filterMode && !filteredObject) {
			featureKey = DataStyle.HIDDEN_NODE;
			featureKeyThin = featureKey;
			featureKeyTagged = featureKey;
			isTaggedAndInZoomLimit = false;
		}
		
		if (isTaggedAndInZoomLimit && showIcons) {
			noIcon = tmpPresets == null || !paintNodeIcon(node, canvas, x, y, isSelected ? featureKeyTagged : null);
			if (noIcon) {
				String houseNumber = node.getTagWithKey(Tags.KEY_ADDR_HOUSENUMBER);
				if (houseNumber != null && !"".equals(houseNumber)) { // draw house-numbers
					paintHouseNumber(x,y,canvas,featureKeyThin,houseNumber);
					return;
				}
			} 
		}
		
		if (noIcon) { 
			// draw regular nodes or without icons
			Paint p = DataStyle.getCurrent(isTagged ? featureKeyTagged : featureKey).getPaint();
			float strokeWidth = p.getStrokeWidth();
			if (hwAccelarationWorkaround) { //FIXME we don't actually know if this is slower than drawPoint
				canvas.drawCircle(x, y, strokeWidth/2, p);
			} else {
				canvas.drawPoint(x, y, p);
			}
			if (isTaggedAndInZoomLimit) {
				paintNodeLabel(x, y, canvas, featureKeyThin, strokeWidth, node);
			}
		}
	}
	
	/**
	 * Draw a circle with center at x,y with the house number in it
	 * @param x
	 * @param y
	 * @param canvas
	 * @param featureKeyThin
	 * @param houseNumber
	 */
    private void paintHouseNumber(final float x, final float y, final Canvas canvas, final String featureKeyThin, final String houseNumber) {
		Paint paint2 = DataStyle.getCurrent(featureKeyThin).getPaint();
		canvas.drawCircle(x, y, houseNumberRadius, paint2);
		canvas.drawText(houseNumber, x - (paint2.measureText(houseNumber) / 2), y + verticalNumberOffset, paint2); 
	}
	
	/**
	 * Praint a label under the node, does not try to do collision avoidance
	 * @param x
	 * @param y
	 * @param canvas
	 * @param featureKeyThin
	 * @param strokeWidth
	 * @param node
	 */
    private void paintNodeLabel(final float x, final float y, final Canvas canvas, final String featureKeyThin, final float strokeWidth, final Node node) {
		Paint paint2 = DataStyle.getCurrent(featureKeyThin).getPaint();
		SortedMap<String, String> tags = node.getTags();
		String label = labelcache.get(tags); // may be null!
		if (label == null) {
			if (!labelcache.containsKey(tags)) {
				label = node.getTagWithKey(Tags.KEY_NAME);
				if (label == null && tmpPresets != null) { 
					PresetItem match = Preset.findBestMatch(tmpPresets,node.getTags());
					if (match != null) {
						label = match.getTranslatedName();
					} else {
						label  = node.getPrimaryTag();
						// if label is still null, leave it as is
					}
				}
				labelcache.put(tags,label);
				if (label==null) {
					return;
				}
			} else {
				return;
			}
		}
		canvas.drawText(label, x - (paint2.measureText(label) / 2), y + strokeWidth + 2*verticalNumberOffset, paint2);
	}
	
	/**
	 * Retrieve icon for the element, caching it if it isn't in the cache
	 * 
	 * @param element
	 * @return icon or null if none is found
	 */
    private Bitmap getIcon(OsmElement element) {
		SortedMap<String, String> tags = element.getTags();
		Bitmap icon = iconcache.get(tags); // may be null!
		if (icon == null && tmpPresets != null) {
			if (iconcache.containsKey(tags)) {
				// no point in trying to match
				return icon;
			}
			// icon not cached, ask the preset, render to a bitmap and cache result
			PresetItem match = null;
			if (element instanceof Way) { 
				// don't show building icons, but only icons for buildings
				SortedMap<String,String> tempTags = new TreeMap<String,String>(tags);
				if (tempTags.remove(Tags.KEY_BUILDING) != null || element.hasTag(Tags.KEY_INDOOR,Tags.VALUE_ROOM)) {
					match = Preset.findBestMatch(tmpPresets,tempTags);
				} 
			} else {
				match = Preset.findBestMatch(tmpPresets,tags);
			}
			if (match != null) {
				Drawable iconDrawable = match.getMapIcon();
				if (iconDrawable != null) {
					icon = Bitmap.createBitmap(iconRadius*2, iconRadius*2, Config.ARGB_8888);
					// icon.eraseColor(Color.WHITE); // replace nothing with white?
					iconDrawable.draw(new Canvas(icon));
				}
			}
			iconcache.put(tags, icon);
		}
		return icon;
	}
	
	/**
	 * Paints an icon for an element. tmpPreset needs to be available (i.e. not null).
	 * @param element the element whose icon should be painted
	 * @param canvas the canvas on which to draw
	 * @param x the x position where the center of the icon goes
	 * @param y the y position where the center of the icon goes
	 */
	private boolean paintNodeIcon(OsmElement element, Canvas canvas, float x, float y, String featureKey) {
		Bitmap icon = getIcon(element);
		if (icon != null) {
			float w2 = icon.getWidth()/2f;
			float h2 = icon.getHeight()/2f;
			if (featureKey != null) { // selected
				RectF r = new RectF(x - w2 - iconSelectedBorder, y - h2 - iconSelectedBorder, x + w2 + iconSelectedBorder, y + h2 + iconSelectedBorder);
				canvas.drawRoundRect(r, iconSelectedBorder, iconSelectedBorder, DataStyle.getCurrent(featureKey).getPaint());
			}
			// we have an icon! draw it.
			canvas.drawBitmap(icon, x - w2, y - h2, null);
			return true;
		}
		return false;
	}

	/**
	 * @param canvas
	 * @param lat
	 * @param lon
	 * @param isTagged TODO
	 * @param x
	 * @param y
	 * @param node
	 */
	private void drawNodeTolerance(final Canvas canvas, final Byte nodeState, final int lat, final int lon,
			boolean isTagged, final float x, final float y, Paint paint) {
		if (nodeState != OsmElement.STATE_UNCHANGED || delegator.isInDownload(lat, lon)) {
			canvas.drawCircle(x, y, isTagged ? paint.getStrokeWidth() : wayTolerancePaint.getStrokeWidth()/2, paint);
		}
	}

	/**
	 * Paints the given way on the canvas.
	 * 
	 * @param canvas Canvas, where the node shall be painted on.
	 * @param way way which shall be painted.
	 * @param drawTolerance 
	 */
	private void paintWay(final Canvas canvas, final Way way, final boolean displayHandles, boolean drawTolerance) {
		float[] linePoints = pointListToLinePointsArray(way.getNodes());
		Paint paint;
		
		boolean isSelected = tmpDrawingInEditRange // if we are not in editing range don't show selected way ... may be a better idea to do so
				&& tmpDrawingSelectedWays != null && tmpDrawingSelectedWays.contains(way) ;
		boolean isMemberOfSelectedRelation = tmpDrawingInEditRange 
				&& tmpDrawingSelectedRelationWays != null && tmpDrawingSelectedRelationWays.contains(way);	
		
		//draw way tolerance
		if (drawTolerance) {
			if (prefs.isToleranceVisible() && tmpClickableElements == null) {
				canvas.drawLines(linePoints, wayTolerancePaint);
			} else if (tmpClickableElements != null && tmpClickableElements.contains(way)) {
				canvas.drawLines(linePoints, wayTolerancePaint2);
			}
		}
				
		//draw selectedWay highlighting
		if (isSelected) {
			paint = DataStyle.getCurrent(DataStyle.SELECTED_WAY).getPaint();
			canvas.drawLines(linePoints, paint);
			paint = DataStyle.getCurrent(DataStyle.WAY_DIRECTION).getPaint();
			drawWayArrows(canvas, linePoints, false, paint, displayHandles && tmpDrawingSelectedWays.size()==1);
		} else if (isMemberOfSelectedRelation) {
			paint = DataStyle.getCurrent(DataStyle.SELECTED_RELATION_WAY).getPaint();
			canvas.drawLines(linePoints, paint);
			paint = DataStyle.getCurrent(DataStyle.WAY_DIRECTION).getPaint();
			drawWayArrows(canvas, linePoints, false, paint, false);
		}

		int onewayCode = way.getOneway();
		if (onewayCode != 0) {
			FeatureStyle fp = DataStyle.getCurrent(DataStyle.ONEWAY_DIRECTION);
			drawWayArrows(canvas, linePoints, (onewayCode == -1), fp.getPaint(), false);
		} else if (way.getTagWithKey(Tags.KEY_WATERWAY) != null) { // waterways flow in the way direction
			FeatureStyle fp = DataStyle.getCurrent(DataStyle.ONEWAY_DIRECTION);
			drawWayArrows(canvas, linePoints, false, fp.getPaint(), false);
		}
		
		// 
		FeatureStyle fp; // no need to get the default here
		
		if (way.hasProblem(context)) {
			fp = DataStyle.getCurrent(DataStyle.PROBLEM_WAY);
		} else {
			fp = getAndSetStyle(way);
		}
			
		// draw the way itself
		// canvas.drawLines(linePoints, fp.getPaint()); doesn't work properly with HW acceleration
		if (linePoints.length > 2) {
			path.reset();
			path.moveTo(linePoints[0], linePoints[1]);
			for (int i=0;i<(linePoints.length);i=i+4) {
				path.lineTo(linePoints[i+2], linePoints[i+3]);
			}
			canvas.drawPath(path, fp.getPaint());
		}
		
		// display icons on closed ways
		if (showIcons && showWayIcons && zoomLevel > SHOW_ICONS_LIMIT && way.isClosed()) {
			int vs = linePoints.length;
			if (vs < way.nodeCount()*2) {
				return;
			}
			double A = 0;
			double Y = 0;
			double X = 0;
			for (int i = 0; i < vs ; i=i+2 ) { // calc centroid
				double x1 = linePoints[i];
				double y1 = linePoints[i+1];
				double x2 = linePoints[(i+2) % vs];
				double y2 = linePoints[(i+3) % vs];
				double d = x1*y2 - x2*y1;
				A = A + d;
				X = X + (x1+x2)*d;
				Y = Y + (y1+y2)*d;
			}
			Y = Y/(3*A);
			X = X/(3*A);
			if (tmpPresets == null || !paintNodeIcon(way, canvas, (float)X, (float)Y, isSelected?DataStyle.SELECTED_NODE_TAGGED:null)) {
				String houseNumber = way.getTagWithKey(Tags.KEY_ADDR_HOUSENUMBER);
				if (houseNumber != null && !"".equals(houseNumber)) { // draw house-numbers
					paintHouseNumber((float)X,(float)Y,canvas,isSelected?DataStyle.SELECTED_NODE_THIN:DataStyle.NODE_THIN,houseNumber);
					return;
				}
			}
		}
	}
	
	/**
	 * Paints the given way on the canvas with the "hidden" style.
	 * 
	 * @param canvas Canvas, where the node shall be painted on.
	 * @param way way which shall be painted.
	 */
	private void paintHiddenWay(final Canvas canvas, final Way way) {
		float[] linePoints = pointListToLinePointsArray(way.getNodes());
			
		// 
		FeatureStyle fp = DataStyle.getCurrent(DataStyle.HIDDEN_WAY);
					
		// draw the way itself
		// canvas.drawLines(linePoints, fp.getPaint()); doesn't work properly with HW acceleration
		if (linePoints.length > 2) {
			path.reset();
			path.moveTo(linePoints[0], linePoints[1]);
			for (int i=0;i<(linePoints.length);i=i+4) {
				path.lineTo(linePoints[i+2], linePoints[i+3]);
			}
			canvas.drawPath(path, fp.getPaint());
		}
	}

	/**
	 * Determine the style to use for way and cache it in the way object
	 * @param way
	 * @return
	 */
	private FeatureStyle getAndSetStyle(final Way way) {
		FeatureStyle fp;
		FeatureStyle wayFp = way.getFeatureProfile();
		if (wayFp == null) {
			fp = DataStyle.getCurrent(DataStyle.WAY); // default for ways
			// three levels of hierarchy for roads and special casing of tracks, two levels for everything else
			String highwayType = way.getTagWithKey(Tags.KEY_HIGHWAY);
			if (highwayType != null) {
				FeatureStyle tempFp = DataStyle.getCurrent("way-highway");
				if (tempFp != null) {
					fp = tempFp;
				}
				tempFp = DataStyle.getCurrent("way-highway-" + highwayType);
				if (tempFp != null) {
					fp = tempFp;
				}
				String highwaySubType;
				if (highwayType.equals("track")) { // special case
					highwaySubType = way.getTagWithKey("tracktype");
				} else {
					highwaySubType = way.getTagWithKey(highwayType);
				}
				if (highwaySubType != null) {
					tempFp = DataStyle.getCurrent("way-highway-" + highwayType + "-" + highwaySubType);
					if (tempFp != null) {
						fp = tempFp;
					}
				} 
			} else {
				// order in the array defines precedence
				String[] tags = {"building","railway","leisure","landuse","waterway","natural","addr:interpolation","boundary","amenity","shop","power",
						"aerialway","military","historic","indoor","building:part"};
				FeatureStyle tempFp = null;
				for (String tag:tags) {
					tempFp = getProfile(tag, way);
					if (tempFp != null) {
						fp = tempFp;
						break;
					}
				}
				if (tempFp == null) {
					ArrayList<Relation> relations = way.getParentRelations();
					// check for any relation memberships with low prio, take first one
					String[] relationTags = {"boundary","leisure","landuse","natural","waterway","building"};
					if (relations != null) { 
						for (Relation r : relations) {
							for (String tag:relationTags) {
								tempFp = getProfile(tag, r);
								if (tempFp != null) {
									fp = tempFp;
									break;
								} 
							}
							if (tempFp != null) { // break out of loop over relations
								break;
							}
						}
					}
				}
			}
			way.setFeatureProfile(fp);
		} else {
			fp = wayFp;
		}
		return fp;
	}

	private void paintHandles(Canvas canvas) {
		if (handles != null && handles.size() > 0) {
			canvas.save();
			float lastX = 0;
			float lastY = 0;
			for (long l:handles.values()) {
				// draw handle
				// canvas.drawCircle(x0 + xDelta/2, y0 + yDelta/2, 5, Profile.getCurrent(Profile.HANDLE).getPaint());
				// canvas.drawPoint(x0 + xDelta/2, y0 + yDelta/2, Profile.getCurrent(Profile.HANDLE).getPaint());

				float X = Float.intBitsToFloat((int)(l>>>32));
				float Y = Float.intBitsToFloat((int)(l));
				canvas.translate(X-lastX, Y-lastY);
				lastX = X;
				lastY = Y;
				canvas.drawPath(DataStyle.getCurrent().x_path, DataStyle.getCurrent(DataStyle.HANDLE).getPaint());
			}
			canvas.restore();
			handles.clear(); // this is hopefully faster than allocating a new set
		}
	}	
	
	private FeatureStyle getProfile(String tag, OsmElement e) {
		String mainType = e.getTagWithKey(tag);
		FeatureStyle fp = null;
		if (mainType != null) {
			FeatureStyle tempFp = DataStyle.getCurrent("way-" + tag);
			if (tempFp != null) {
				fp = tempFp;
			}
			tempFp = DataStyle.getCurrent("way-" + tag + "-" + mainType);
			if (tempFp != null) {
				fp = tempFp;
			}
		}
		return fp;
	}
	
	/**
	 * Draws directional arrows for a way
	 * @param canvas the canvas on which to draw
	 * @param linePoints line segment array in the format returned by {@link #pointListToLinePointsArray(Iterable)}.
	 * @param reverse if true, the arrows will be painted in the reverse direction
	 * @param paint the paint to use for drawing the arrows
	 * @param addHandles if true draw arrows at 1/4 and 3/4 of the length and save the middle pos. for drawing a handle
	 */
	private void drawWayArrows(Canvas canvas, float[] linePoints, boolean reverse, Paint paint, boolean addHandles) {
		double minLen = DataStyle.getCurrent().minLenForHandle;
		int ptr = 0;
		while (ptr < linePoints.length) {
			
			float x1 = linePoints[ptr++];
			float y1 = linePoints[ptr++];
			float x2 = linePoints[ptr++];
			float y2 = linePoints[ptr++];

			float xDelta = x2-x1;
			float yDelta = y2-y1;
			
			boolean secondArrow = false;
			if (addHandles) {
				double len = Math.hypot(xDelta,yDelta);
				if (len > minLen) {
					if (handles == null) handles = new LongHashSet();
					handles.put(((long)(Float.floatToRawIntBits(x1 + xDelta/2)) <<32) + (long)Float.floatToRawIntBits(y1 + yDelta/2));
					xDelta = xDelta / 4;
					yDelta = yDelta / 4;
					secondArrow = true;
				} else {
					xDelta = xDelta / 2;
					yDelta = yDelta / 2;
				}
			} else {
				xDelta = xDelta / 2;
				yDelta = yDelta / 2;
			}
			
			float x = x1 + xDelta;
			float y = y1 + yDelta;
			float angle = (float)(Math.atan2(y2 - y1, x2 - x1) * 180 / Math.PI);
			
			canvas.save();
			canvas.translate(x,y);
			canvas.rotate(reverse ? angle-180 : angle);
			canvas.drawPath(DataStyle.WAY_DIRECTION_PATH, paint);
			canvas.restore();
			
			if (secondArrow) {
				canvas.save();
				canvas.translate(x+2*xDelta,y+2*yDelta);
				canvas.rotate(reverse ? angle-180 : angle);
				canvas.drawPath(DataStyle.WAY_DIRECTION_PATH, paint);
				canvas.restore();
			}
		}
	}

	/**
	 * Converts a geographical way/path/track to a list of screen-coordinate points for drawing.
	 * Only segments that are inside the ViewBox are included.
	 * @param nodes An iterable (e.g. List or array) with GeoPoints of the line that should be drawn
	 *              (e.g. a Way or a GPS track)
	 * @return an array of floats in the format expected by {@link Canvas#drawLines(float[], Paint)}.
	 */
	private float[] pointListToLinePointsArray(final List<? extends GeoPoint> nodes) {
		ArrayList<Float> points = new ArrayList<Float>();
		BoundingBox box = getViewBox();
		
		//loop over all nodes
		GeoPoint prevNode = null;
		GeoPoint lastDrawnNode = null;
		float prevX=0f;
		float prevY=0f;
		int w = getWidth();
		int h = getHeight();
		boolean thisIntersects = false;
		boolean nextIntersects = false;
		for (int i=0;i<nodes.size();i++) {
			GeoPoint node = nodes.get(i);
			int nodeLon = node.getLon();
			int nodeLat = node.getLat();
			boolean interrupted = false;
			if (node instanceof InterruptibleGeoPoint) {
				interrupted = ((InterruptibleGeoPoint)node).isInterrupted();
			}
			nextIntersects = true;
			GeoPoint nextNode = null;
			if (i<nodes.size()-1) {
				nextNode = nodes.get(i+1);
				nextIntersects = box.intersects(nextNode.getLat(),nextNode.getLon(),nodeLat, nodeLon);
			}
			float X = Float.MIN_VALUE;
			float Y = Float.MIN_VALUE;
			if (!interrupted && prevNode != null) {
				if (thisIntersects || nextIntersects 
						|| (!(nextNode != null && lastDrawnNode != null) || box.intersects(nextNode.getLat(), nextNode.getLon(), lastDrawnNode.getLat(), lastDrawnNode.getLon()))) {
					X = GeoMath.lonE7ToX(w, box, nodeLon);
					Y = GeoMath.latE7ToY(h, w, box, nodeLat);
					if (prevX == Float.MIN_VALUE) { // last segment didn't intersect
						prevX = GeoMath.lonE7ToX(w, box, prevNode.getLon());
						prevY = GeoMath.latE7ToY(h, w, box, prevNode.getLat());
					}
					// Line segment needs to be drawn
					points.add(prevX);
					points.add(prevY);
					points.add(X);
					points.add(Y);
					lastDrawnNode = node;
				}
			} 
			prevNode = node;
			prevX = X;
			prevY = Y;
			thisIntersects = nextIntersects;
		}
		
		// convert from ArrayList<Float> to float[]
		float[] result = new float[points.size()];
		int i = 0;
		for (Float f : points) result[i++] = f;
		return result;
	}
	

	/**
	 * ${@inheritDoc}.
	 */
	@Override
	public BoundingBox getViewBox() {
		return myViewBox;
	}
	
	/**
	 * @param aSelectedNode the currently selected node to edit.
	 */
	void setSelectedNodes(final List<Node> aSelectedNodes) {
		tmpDrawingSelectedNodes = aSelectedNodes;
	}
	
	/**
	 * 
	 * @param aSelectedWay the currently selected way to edit.
	 */
	void setSelectedWays(final List<Way> aSelectedWays) {
		tmpDrawingSelectedWays = aSelectedWays;
	}
	
	public Preferences getPrefs() {
		return prefs;
	}
	
	public void setPrefs(Context ctx, final Preferences aPreference) {
		prefs = aPreference;
		TileLayerServer.setBlacklist(prefs.getServer().getCachedCapabilities().imageryBlacklist);
		synchronized(mOverlays) {
			for (MapViewOverlay osmvo : mOverlays) {
				if (osmvo instanceof MapTilesOverlay && !(osmvo instanceof MapOverlayTilesOverlay)) {
					final TileLayerServer backgroundTS = TileLayerServer.get(ctx, prefs.backgroundLayer(), true);
					((MapTilesOverlay)osmvo).setRendererInfo(backgroundTS);
				} else if (osmvo instanceof MapOverlayTilesOverlay) {
					final TileLayerServer overlayTS = TileLayerServer.get(ctx, prefs.overlayLayer(), true);
					((MapOverlayTilesOverlay)osmvo).setRendererInfo(overlayTS);
				}
			}
		}
		showIcons = prefs.getShowIcons();
		showWayIcons = prefs.getShowWayIcons();
		iconcache.clear();
		alwaysDrawBoundingBoxes = prefs.getAlwaysDrawBoundingBoxes();
	}

	public void updateProfile () {
		// changes when profile changes
		nodeTolerancePaint = DataStyle.getCurrent(DataStyle.NODE_TOLERANCE).getPaint();
		nodeTolerancePaint2 = DataStyle.getCurrent(DataStyle.NODE_TOLERANCE_2).getPaint();
		wayTolerancePaint = DataStyle.getCurrent(DataStyle.WAY_TOLERANCE).getPaint();
		wayTolerancePaint2 = DataStyle.getCurrent(DataStyle.WAY_TOLERANCE_2).getPaint();
	}
	
	void setOrientation(final float orientation) {
		this.orientation = orientation;
	}
	
	void setLocation(Location location) {
		displayLocation = location;
	}

	void setTracker(TrackerService tracker) {
		this.tracker = tracker;
	}

	void setDelegator(final StorageDelegator delegator) {
		this.delegator = delegator;
	}
	
	public void setViewBox(final BoundingBox viewBox) {
		myViewBox = viewBox;
		try {
			myViewBox.setRatio(this, (float) getWidth()/ getHeight(), false);
		} catch (OsmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void showCrosshairs(float x, float y) {
		showCrosshairs = true;
		// store as lat lon for redraws on translation and zooming
		crosshairsLat = GeoMath.yToLatE7(getHeight(), getWidth(), getViewBox(), y);
		crosshairsLon = GeoMath.xToLonE7(getWidth() , getViewBox(), x);
	}
	
	public void hideCrosshairs() {
		showCrosshairs = false;
	}
	
	/**
	 * You can add/remove/reorder your Overlays using the List of
	 * {@link MapViewOverlay}. The first (index 0) Overlay gets drawn
	 * first, the one with the highest as the last one.
	 */
	public List<MapViewOverlay> getOverlays() {
		return mOverlays;
	}
	
	/**
	 * ${@inheritDoc}.
	 */
	@Override
	public int getZoomLevel() {
		return zoomLevel;
	}
	
	/**
	 * This calculates the best tile zoom level to use (not the actual zoom level of the map!)
	 * @param viewPort
	 * @return the tile zoom level
	 */
    private int calcZoomLevel(Canvas canvas) {
		final TileLayerServer s = getOpenStreetMapTilesOverlay().getRendererInfo();
		if (!s.isMetadataLoaded()) // protection on startup
			return 0;
		
		// Calculate lat/lon of view extents
		final double latBottom = getViewBox().getBottom() / 1E7; // GeoMath.yToLatE7(viewPort.height(), getViewBox(), viewPort.bottom) / 1E7d;
		final double lonRight  = getViewBox().getRight() / 1E7; // GeoMath.xToLonE7(viewPort.width() , getViewBox(), viewPort.right ) / 1E7d;
		final double latTop    = getViewBox().getTop() / 1E7; // GeoMath.yToLatE7(viewPort.height(), getViewBox(), viewPort.top   ) / 1E7d;
		final double lonLeft   = getViewBox().getLeft() / 1E7; // GeoMath.xToLonE7(viewPort.width() , getViewBox(), viewPort.left  ) / 1E7d;
		
		// Calculate tile x/y scaled 0.0 to 1.0
		final double xTileRight  = (lonRight + 180d) / 360d;
		final double xTileLeft   = (lonLeft  + 180d) / 360d;
		final double yTileBottom = (1d - Math.log(Math.tan(Math.toRadians(latBottom)) + 1d / Math.cos(Math.toRadians(latBottom))) / Math.PI) / 2d;
		final double yTileTop    = (1d - Math.log(Math.tan(Math.toRadians(latTop   )) + 1d / Math.cos(Math.toRadians(latTop   ))) / Math.PI) / 2d;
		
		// Calculate the ideal zoom to fit into the view
		final double xTiles = (canvas.getWidth()  / (xTileRight  - xTileLeft)) / s.getTileWidth();
		final double yTiles = (canvas.getHeight() / (yTileBottom - yTileTop )) / s.getTileHeight();
		final double xZoom = Math.log(xTiles) / Math.log(2d);
		final double yZoom = Math.log(yTiles) / Math.log(2d);
		
		// Zoom out to the next integer step
		int zoom = (int)Math.floor(Math.max(0, Math.min(xZoom, yZoom)));
		zoom = Math.min(zoom, s.getMaxZoomLevel());	
		
		return zoom;
	}

	public Location getLocation() {
		return displayLocation;
	}
	
	/**
	 * Set the flag that determines if the arror is just an outline or not
	 * @param follow
	 */
	public void setFollowGPS(boolean follow) {
		isFollowingGPS = follow;
	}

	/**
	 * Return a list of the names of the currently used layers
	 * @return
	 */
	public ArrayList<String> getImageryNames() {
		ArrayList<String>result = new ArrayList<String>();
		synchronized(mOverlays) {
			for (MapViewOverlay osmvo : mOverlays) {
				if ((osmvo instanceof MapTilesOverlay)) {
					result.add(((MapTilesOverlay)osmvo).getRendererInfo().getName());
				}
			}
		}
		return result;
	}
}
