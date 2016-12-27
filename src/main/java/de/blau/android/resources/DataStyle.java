package de.blau.android.resources;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Join;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.PixelXorXfermode;
import android.graphics.Typeface;
import android.os.Environment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import de.blau.android.R;
import de.blau.android.contract.Paths;
import de.blau.android.resources.DataStyle.FeatureStyle.DashPath;
import de.blau.android.util.Density;
import de.blau.android.util.FileUtil;

public class DataStyle  extends DefaultHandler {
	
    final static String FILE_PATH_STYLE_SUFFIX = "-profile.xml";
	
	// constants for the internal profiles
	public final static String GPS_TRACK = "gps_track";
	
	public final static String INFOTEXT = "infotext";
	public final static String ATTRIBUTION_TEXT = "attribution_text";
	public final static String VIEWBOX = "viewbox";
	public final static String WAY_TOLERANCE = "way_tolerance";
	public final static String WAY_TOLERANCE_2 = "way_tolerance_2";
	public final static String WAY = "way";
	public final static String SELECTED_WAY = "selected_way";
	public final static String SELECTED_RELATION_WAY = "selected_relation_way";
	public final static String PROBLEM_WAY = "problem_way";
	public final static String HIDDEN_WAY = "hidden_way";
	public final static String NODE_TOLERANCE = "node_tolerance";
	public final static String NODE_TOLERANCE_2 = "node_tolerance_2";
	public final static String NODE = "node";
	public final static String NODE_THIN = "node_thin";
	public static final String NODE_TAGGED = "node_tagged";
	public static final String NODE_DRAG_RADIUS = "node_drag_radius";
	public final static String PROBLEM_NODE = "problem_node";
	public final static String PROBLEM_NODE_THIN = "problem_node_thin";
	public static final String PROBLEM_NODE_TAGGED = "problem_node_tagged";
	public final static String SELECTED_NODE = "selected_node";
	public final static String SELECTED_NODE_THIN = "selected_node_thin";
	public static final String SELECTED_NODE_TAGGED = "selected_node_tagged";
	public final static String SELECTED_RELATION_NODE = "selected_relation_node";
	public final static String SELECTED_RELATION_NODE_THIN = "selected_relation_node_thin";
	public static final String SELECTED_RELATION_NODE_TAGGED = "selected_relation_node_tagged";
	public final static String HIDDEN_NODE = "hidden_node";
	public final static String WAY_DIRECTION = "way_direction";
	public final static String ONEWAY_DIRECTION = "oneway_direction";	
	public final static String LARGE_DRAG_AREA = "large_drag_area";
	public final static String MARKER_SCALE = "marker_scale";
	public final static String GPS_POS = "gps_pos";
	public final static String GPS_POS_FOLLOW = "gps_pos_follow";
	public final static String GPS_ACCURACY = "gps_accuracy";
	public final static String OPEN_NOTE = "open_note";
	public final static String CLOSED_NOTE = "closed_note";
	public final static String CROSSHAIRS = "crosshairs";
	public final static String HANDLE = "handle";
	public final static String LABELTEXT = "labeltext";
	
	
	public class FeatureStyle {
		
		String name;
		boolean editable;
		boolean internal;
		boolean updateWidth;
		Paint paint;
		float widthFactor;
		DashPath dashPath = null;
		
		public class DashPath {
			public float[] intervals;
			public float phase;
		}
		
		FeatureStyle (String n, Paint p) {
			// Log.i("FeatureStyle","setting up feature " + n);
			name = n;
			editable = true;
			internal = false;
			updateWidth = true;
			if (p != null) {
				paint = new Paint(p);
			}
			else {
				paint = new Paint();
			}
				
			widthFactor = 1.0f;
		}
		
		FeatureStyle (String n) {
			this(n, (Paint)null);
		}
		
		FeatureStyle(String n, FeatureStyle fp)
		{
			if (fp == null) {
				Log.i("FeatureStyle","setting up feature " + n + " profile is null!");
				return;
			}
			// Log.i("FeatureStyle","setting up feature " + n + " from " + fp.getName());
			name = n;
			editable = fp.editable;
			internal = fp.internal;
			updateWidth = fp.updateWidth;
			paint = new Paint(fp.paint);
			widthFactor = fp.widthFactor;
			if (fp.dashPath != null) {
				dashPath = new DashPath();
				dashPath.intervals = fp.dashPath.intervals.clone();
				dashPath.phase = fp.dashPath.phase;
			}
		}
		
		public String getName() {
			return name;
		}
		
		public Paint getPaint() {
			return paint;
		}
		
		public void setColor(int c)
		{
			paint.setColor(c);
		}
		
		public void setWidthFactor(float f)
		{
			widthFactor = f;
		}
		
		public float getWidthFactor()
		{
			return widthFactor;
		}
		
		public void setStrokeWidth(float width) {
			if (updateWidth) {
				paint.setStrokeWidth(width*widthFactor);
				if (dashPath != null) {
					float[] intervals = dashPath.intervals.clone();
					for (int i=0;i<intervals.length;i++) {
						intervals[i] = dashPath.intervals[i] * width * widthFactor;
					}
					DashPathEffect dp = new DashPathEffect(intervals, dashPath.phase);
					paint.setPathEffect(dp);
				}
			}
		}
		
		public void dontUpdate() {
			updateWidth = false;
		}
		
		public boolean updateWidth() {
			return updateWidth;
		}
		
		public boolean isEditable() {
			return editable;
		}
		
		public void setEditable(boolean e) {
			editable = e;
		}
		
		public DashPath getDashPath() {
			return dashPath;
		}
		
		public void setDashPath(float[] i, float p) {
			dashPath= new DashPath();
			dashPath.intervals = i;
			dashPath.phase = p;
		}
		
		public boolean isInternal() {
			return internal;
		}
		
		public void setInternal(boolean i) {
			internal = i;
		}	
	}

	String name;
	HashMap<String,FeatureStyle> featureStyles;
	
	public static DataStyle currentStyle;
	public static HashMap<String,DataStyle> availableStyles;
	
	public static final float NODE_OVERLAP_TOLERANCE_VALUE = 10f;

	private static final int TOLERANCE_ALPHA = 40;
	private static final int TOLERANCE_ALPHA_2 = 128;
	
	/**
	 * GPS arrow
	 */
	public Path orientation_path = new Path();
	
	/**
	 * Crosshairs
	 */
	public Path crosshairs_path = new Path();
	
	/**
	 * X
	 */
	public Path x_path = new Path();
	
	/**
	 * Arrow indicating the direction of one-way streets. Set/updated in updateStrokes 
	 */
	public static final Path WAY_DIRECTION_PATH = new Path();
	
	private static final String BUILTIN_STYLE_NAME = "Default";
	
	public float nodeToleranceValue;
	public float wayToleranceValue;
	public float largDragCircleRadius;
	public float largDragToleranceRadius;
	public float minLenForHandle;

	private final Context ctx;
	
	public DataStyle(final Context ctx) {
		this.ctx = ctx;
		// create default 
		init(ctx.getResources());

		getStylesFromFile(ctx);
		Log.i("Style","profile " + currentStyle.name);
	}

	
	public DataStyle(String n, DataStyle from) {
		// copy existing profile
		this.ctx = from.ctx;
		name = n;
		featureStyles = new HashMap<String, FeatureStyle>();
		for (FeatureStyle fp : from.featureStyles.values()) {
			featureStyles.put(fp.getName(),new FeatureStyle(fp.getName(), fp));
		}
	}
	
	public DataStyle(Context ctx, InputStream is) {
		this.ctx = ctx;
		// create a profile from a file
		init(ctx.getResources()); // defaults for internal styles 
		read(is);
	}
	
	/**
	 * initialize the minimum required internal style for a new profile
	 * @param resources
	 */
	private void init(final Resources resources) {
		nodeToleranceValue = Density.dpToPx(ctx,40f); // TODO move to constant
		wayToleranceValue = Density.dpToPx(ctx,40f);
		largDragCircleRadius = Density.dpToPx(ctx,70f);
		largDragToleranceRadius = Density.dpToPx(ctx,100f);
		minLenForHandle = 5 * nodeToleranceValue;
		
		orientation_path.moveTo(0, Density.dpToPx(ctx,-20));
		orientation_path.lineTo(Density.dpToPx(ctx,15), Density.dpToPx(ctx,20));
		orientation_path.lineTo(0, Density.dpToPx(ctx,10));
		orientation_path.lineTo(Density.dpToPx(ctx,-15), Density.dpToPx(ctx,20));
		orientation_path.lineTo(0, Density.dpToPx(ctx,-20));
	
		int arm = Density.dpToPx(ctx,10);
		crosshairs_path.moveTo(0, -arm);
		crosshairs_path.lineTo(0, arm);
		crosshairs_path.moveTo(arm, 0);
		crosshairs_path.lineTo(-arm, 0);
		
		arm = Density.dpToPx(ctx,4);
		x_path.moveTo(-arm, -arm);
		x_path.lineTo(arm, arm);
		x_path.moveTo(arm, -arm);
		x_path.lineTo(-arm, arm);
		
		@SuppressWarnings("deprecation")
		PixelXorXfermode whiteXor = new PixelXorXfermode(Color.WHITE);
		
		Log.i("Style","setting up default profile elements");
		featureStyles = new HashMap<String, FeatureStyle>();

		Paint standardPath = new Paint();
		standardPath.setStyle(Style.STROKE);
		// As nodes cover the line ends/joins, the line ending styles are irrelevant for most paints
		// However, at least on the software renderer, the default styles (Cap = BUTT, Join = MITER)
		// have slightly better performance than the round styles.

		FeatureStyle fp = new FeatureStyle(WAY, standardPath);
		fp.setColor(Color.BLACK);
		featureStyles.put(fp.getName(), fp);
		
		fp = new FeatureStyle(PROBLEM_WAY, standardPath);
		fp.setColor(ContextCompat.getColor(ctx,R.color.problem));
		fp.setWidthFactor(1.5f);
		featureStyles.put(fp.getName(), fp);

		fp = new FeatureStyle(VIEWBOX, standardPath);
		fp.setColor(ContextCompat.getColor(ctx,R.color.grey));
		fp.dontUpdate();
		fp.getPaint().setStyle(Style.FILL);
		fp.getPaint().setAlpha(125);
		featureStyles.put(fp.getName(), fp);
		
		fp = new FeatureStyle(HANDLE);
		fp.dontUpdate();
		fp.setColor(Color.BLACK);
		fp.setWidthFactor(1f);
		fp.getPaint().setStyle(Style.STROKE);
		// fp.getPaint().setStrokeCap(Cap.ROUND);
		fp.getPaint().setXfermode(whiteXor);
		fp.getPaint().setStrokeWidth(Density.dpToPx(ctx,1.0f));
		featureStyles.put(fp.getName(), fp);
		
		fp = new FeatureStyle(NODE);
		fp.setColor(ContextCompat.getColor(ctx,R.color.ccc_red));
		fp.setWidthFactor(1f);
		featureStyles.put(fp.getName(), fp);
		
		fp = new FeatureStyle(NODE_TAGGED);
		fp.setColor(ContextCompat.getColor(ctx,R.color.ccc_red));
		fp.setWidthFactor(1.5f);
		featureStyles.put(fp.getName(), fp);

		fp = new FeatureStyle(NODE_THIN);
		fp.dontUpdate();
		fp.getPaint().setStrokeWidth(Density.dpToPx(ctx,1.0f));
		fp.setColor(ContextCompat.getColor(ctx,R.color.ccc_red));
		fp.getPaint().setStyle(Style.STROKE);
		fp.getPaint().setTypeface(Typeface.SANS_SERIF);
		fp.getPaint().setTextSize(Density.dpToPx(ctx,12));
		featureStyles.put(fp.getName(), fp);
		
		fp = new FeatureStyle(PROBLEM_NODE);
		fp.setColor(ContextCompat.getColor(ctx,R.color.problem));
		fp.setWidthFactor(1f);
		featureStyles.put(fp.getName(), fp);
		
		fp = new FeatureStyle(PROBLEM_NODE_TAGGED);
		fp.setColor(ContextCompat.getColor(ctx,R.color.problem));
		fp.setWidthFactor(1.5f);
		featureStyles.put(fp.getName(), fp);

		fp = new FeatureStyle(PROBLEM_NODE_THIN);
		fp.dontUpdate();
		fp.getPaint().setStrokeWidth(Density.dpToPx(ctx,1.0f));
		fp.setColor(ContextCompat.getColor(ctx,R.color.problem));
		fp.getPaint().setStyle(Style.STROKE);
		fp.getPaint().setTypeface(Typeface.SANS_SERIF);
		fp.getPaint().setTextSize(Density.dpToPx(ctx,12));
		featureStyles.put(fp.getName(), fp);
		
		fp = new FeatureStyle(HIDDEN_NODE);
		fp.dontUpdate();
		fp.getPaint().setStrokeWidth(Density.dpToPx(ctx,1.0f));
		fp.setColor(ContextCompat.getColor(ctx,R.color.light_grey));
		fp.getPaint().setStyle(Style.STROKE);
		fp.getPaint().setTypeface(Typeface.SANS_SERIF);
		fp.getPaint().setTextSize(Density.dpToPx(ctx,12));
		featureStyles.put(fp.getName(), fp);
		
		fp = new FeatureStyle(GPS_TRACK,featureStyles.get(WAY)); 	
		fp.setColor(Color.BLUE);
		fp.getPaint().setStrokeCap(Cap.ROUND);
		fp.getPaint().setStrokeJoin(Join.ROUND);
		featureStyles.put(fp.getName(), fp);
		
		fp = new FeatureStyle(WAY_TOLERANCE,featureStyles.get(WAY)); 	
		fp.setColor(ContextCompat.getColor(ctx,R.color.ccc_ocher));
		fp.dontUpdate();
		fp.getPaint().setAlpha(TOLERANCE_ALPHA);
		fp.getPaint().setStrokeWidth(Density.dpToPx(ctx,wayToleranceValue));
		featureStyles.put(fp.getName(), fp);
		
		fp = new FeatureStyle(WAY_TOLERANCE_2,featureStyles.get(WAY)); 	
		fp.setColor(ContextCompat.getColor(ctx,R.color.ccc_ocher));
		fp.dontUpdate();
		fp.getPaint().setAlpha(TOLERANCE_ALPHA_2);
		fp.getPaint().setStrokeWidth(Density.dpToPx(ctx,wayToleranceValue));
		featureStyles.put(fp.getName(), fp);
		
		fp = new FeatureStyle(SELECTED_NODE);
		fp.setColor(ContextCompat.getColor(ctx,R.color.ccc_beige));
		fp.setWidthFactor(1.5f);
		featureStyles.put(fp.getName(), fp);
		
		fp = new FeatureStyle(SELECTED_RELATION_NODE, featureStyles.get(SELECTED_NODE));
		fp.setColor(ContextCompat.getColor(ctx,R.color.relation));
		featureStyles.put(fp.getName(), fp);
		
		fp = new FeatureStyle(NODE_DRAG_RADIUS );
		fp.setColor(ContextCompat.getColor(ctx,R.color.ccc_beige));
		fp.dontUpdate();
		fp.getPaint().setStyle(Style.STROKE);
		fp.getPaint().setAlpha(150);
		fp.getPaint().setStrokeWidth(Density.dpToPx(ctx,10f));
		featureStyles.put(fp.getName(), fp);
		
		fp = new FeatureStyle(SELECTED_NODE_TAGGED);
		fp.setColor(ContextCompat.getColor(ctx,R.color.ccc_beige));
		fp.setWidthFactor(2f);
		featureStyles.put(fp.getName(), fp);
		
		fp = new FeatureStyle(SELECTED_RELATION_NODE_TAGGED, featureStyles.get(SELECTED_NODE_TAGGED));
		fp.setColor(ContextCompat.getColor(ctx,R.color.relation));
		featureStyles.put(fp.getName(), fp);

		fp = new FeatureStyle(SELECTED_NODE_THIN);
		fp.dontUpdate();
		fp.getPaint().setStrokeWidth(Density.dpToPx(ctx,1.0f));
		fp.setColor(ContextCompat.getColor(ctx,R.color.ccc_beige));
		fp.getPaint().setStyle(Style.STROKE);
		fp.getPaint().setTypeface(Typeface.SANS_SERIF);
		fp.getPaint().setTextSize(Density.dpToPx(ctx,12));
		featureStyles.put(fp.getName(), fp);
		
		fp = new FeatureStyle(SELECTED_RELATION_NODE_THIN, featureStyles.get(SELECTED_NODE_THIN));
		fp.setColor(ContextCompat.getColor(ctx,R.color.relation));
		featureStyles.put(fp.getName(), fp);
		
		fp = new FeatureStyle(GPS_POS,featureStyles.get(GPS_TRACK)); 
		fp.getPaint().setStyle(Style.FILL);
		fp.setWidthFactor(2f);
		featureStyles.put(fp.getName(), fp);
		
		fp = new FeatureStyle(GPS_POS_FOLLOW,featureStyles.get(GPS_POS)); 
		fp.getPaint().setStyle(Style.STROKE);
		fp.getPaint().setStrokeWidth(Density.dpToPx(ctx,4.0f));
		fp.dontUpdate();
		featureStyles.put(fp.getName(), fp);

		fp = new FeatureStyle(GPS_ACCURACY,featureStyles.get(GPS_POS));
		fp.getPaint().setStyle(Style.FILL_AND_STROKE);
		fp.getPaint().setAlpha(TOLERANCE_ALPHA);
		fp.dontUpdate();
		featureStyles.put(fp.getName(), fp);

		fp = new FeatureStyle(SELECTED_WAY,featureStyles.get(WAY)); 	
		fp.setColor(ContextCompat.getColor(ctx,R.color.ccc_beige));
		fp.setWidthFactor(2f);
		fp.getPaint().setStrokeCap(Cap.ROUND);
		fp.getPaint().setStrokeJoin(Join.ROUND);
		featureStyles.put(fp.getName(), fp);
		
		fp = new FeatureStyle(HIDDEN_WAY,featureStyles.get(WAY)); 	
		fp.setColor(ContextCompat.getColor(ctx,R.color.light_grey));
		fp.getPaint().setAlpha(TOLERANCE_ALPHA);
		fp.setWidthFactor(0.5f);
		fp.getPaint().setStrokeCap(Cap.ROUND);
		fp.getPaint().setStrokeJoin(Join.ROUND);
		featureStyles.put(fp.getName(), fp);
		
		fp = new FeatureStyle(SELECTED_RELATION_WAY,featureStyles.get(SELECTED_WAY));
		fp.setColor(ContextCompat.getColor(ctx,R.color.relation));
		featureStyles.put(fp.getName(), fp);

		fp = new FeatureStyle(NODE_TOLERANCE);
		fp.setColor(ContextCompat.getColor(ctx,R.color.ccc_ocher));
		fp.dontUpdate();
		fp.getPaint().setStyle(Style.FILL);
		fp.getPaint().setAlpha(TOLERANCE_ALPHA);
		fp.getPaint().setStrokeWidth(Density.dpToPx(ctx,nodeToleranceValue));
		featureStyles.put(fp.getName(), fp);
		
		fp = new FeatureStyle(NODE_TOLERANCE_2);
		fp.setColor(ContextCompat.getColor(ctx,R.color.ccc_ocher));
		fp.dontUpdate();
		fp.getPaint().setStyle(Style.FILL);
		fp.getPaint().setAlpha(TOLERANCE_ALPHA_2);
		fp.getPaint().setStrokeWidth(Density.dpToPx(ctx,nodeToleranceValue));
		featureStyles.put(fp.getName(), fp);

		fp = new FeatureStyle(INFOTEXT);
		fp.setColor(Color.BLACK);
		fp.dontUpdate();
		fp.getPaint().setTypeface(Typeface.SANS_SERIF);
		fp.getPaint().setTextSize(Density.dpToPx(ctx,12));
		featureStyles.put(fp.getName(), fp);
		
		fp = new FeatureStyle(ATTRIBUTION_TEXT);
		fp.setColor(Color.WHITE);
		fp.dontUpdate();
		fp.getPaint().setTypeface(Typeface.SANS_SERIF);
		fp.getPaint().setTextSize(Density.dpToPx(ctx,12));
		fp.getPaint().setShadowLayer(1, 0, 0, Color.BLACK);
		featureStyles.put(fp.getName(), fp);
		
		fp = new FeatureStyle(LABELTEXT);
		fp.setColor(Color.BLACK);
		fp.dontUpdate();
		fp.getPaint().setTypeface(Typeface.SANS_SERIF);
		fp.getPaint().setTextSize(Density.dpToPx(ctx,12));
		fp.getPaint().setXfermode(whiteXor);
		featureStyles.put(fp.getName(), fp);
		
		fp = new FeatureStyle(WAY_DIRECTION);
		fp.setColor(ContextCompat.getColor(ctx,R.color.ccc_red));
		fp.setWidthFactor(0.8f);
		fp.getPaint().setStyle(Style.STROKE);
		fp.getPaint().setStrokeCap(Cap.SQUARE);
		fp.getPaint().setStrokeJoin(Join.MITER);
		featureStyles.put(fp.getName(), fp);
		
		fp = new FeatureStyle(ONEWAY_DIRECTION,featureStyles.get(WAY_DIRECTION));
		fp.setColor(ContextCompat.getColor(ctx,R.color.ccc_blue));
		fp.setWidthFactor(0.5f);
		featureStyles.put(fp.getName(), fp);
		
		fp = new FeatureStyle(OPEN_NOTE);
		fp.setColor(ContextCompat.getColor(ctx,R.color.bug_open));
		fp.getPaint().setAlpha(100);
		featureStyles.put(fp.getName(), fp);
		
		fp = new FeatureStyle(CLOSED_NOTE);
		fp.setColor(ContextCompat.getColor(ctx,R.color.bug_closed));
		fp.getPaint().setAlpha(100);
		featureStyles.put(fp.getName(), fp);
		
		fp = new FeatureStyle(CROSSHAIRS); 
		fp.setColor(Color.BLACK);
		fp.getPaint().setStyle(Style.STROKE);
		fp.getPaint().setStrokeWidth(Density.dpToPx(ctx,1.0f));
		fp.getPaint().setXfermode(whiteXor);
		fp.dontUpdate();
		featureStyles.put(fp.getName(), fp);
		
		synchronized (this) {
			if (availableStyles == null) {
				name = BUILTIN_STYLE_NAME;
				currentStyle = this;
				availableStyles = new HashMap<String,DataStyle>();
				availableStyles.put(name,this);
			}
		}
		Log.i("Style","... done");
	}

	public static void setAntiAliasing(final boolean aa) {
		for (FeatureStyle fp : currentStyle.featureStyles.values()) {
			fp.getPaint().setAntiAlias(aa);
		}
	}

	/**
	 * Sets the stroke width of all Elements corresponding to the width of the viewbox (=zoomfactor).
	 */
	public static void updateStrokes(final float newStrokeWidth) {
	

		for (FeatureStyle fp : currentStyle.featureStyles.values()) {
			fp.setStrokeWidth(newStrokeWidth);
		}

		// hardwired (for now)
		WAY_DIRECTION_PATH.rewind();
		float wayDirectionPathOffset = newStrokeWidth * 2.0f;
		WAY_DIRECTION_PATH.moveTo(-wayDirectionPathOffset, -wayDirectionPathOffset);
		WAY_DIRECTION_PATH.lineTo(0,0);
		WAY_DIRECTION_PATH.lineTo(-wayDirectionPathOffset, +wayDirectionPathOffset);
	}

	/**
	 * get FeatureStyle specified by key from current profile
	 * @param key
	 * @return
	 */
	public static FeatureStyle getCurrent(final String key) {
		return currentStyle.featureStyles.get(key);
	}
	
	/**
	 * 
	 * @return
	 */
	public static DataStyle getCurrent() {
		return currentStyle;
	}
	
	/**
	 * get FeatureStyle specified by key from this profile
	 * @param key
	 * @return
	 */
	public FeatureStyle get(final String key) {
		return featureStyles.get(key);
	}
	
	/**
	 * return specific named profile
	 * @param n
	 * @return
	 */
	public static DataStyle getStyle(String n) {
		if (availableStyles == null)
			return null;
		return availableStyles.get(n);
	}
	
	/**
	 * return list of available Styles (Defaut entry first, rest sorted)
	 * @return
	 */
	public static String[] getStyleList(Activity activity) {
		if (availableStyles == null) { // shouldn't happen
			DataStyle p = new DataStyle(activity);
			Log.e("Style","getStyleList called before initialized");
		}
		String[] res = new String[availableStyles.size()];
		
		res[0] = BUILTIN_STYLE_NAME;
		String keys[] = (new TreeMap<String, DataStyle>(availableStyles)).keySet().toArray(new String[0]); // sort the list
		int j = 1;
		for (int i=0;i<res.length;i++) {
			if (!keys[i].equals(BUILTIN_STYLE_NAME)) {
				res[j] = keys[i];
				j++;
			}
		}    // probably silly way of doing this
		return res;
	}

	/**
	 * switch to Style with name n
	 * @param n
	 * @return
	 */
	public static boolean switchTo(String n) {
		DataStyle p = getStyle(n);
		if (p != null) {
			currentStyle = p;
			Log.i("Style","Switching to " + n);
			return true;
		}
		return false;
	}
	

	/**
	 * dump this Style in XML format, not very abstracted and closely tied to the implementation
	 * @param s
	 * @throws IllegalArgumentException
	 * @throws IllegalStateException
	 * @throws IOException
	 */
	public void toXml(final XmlSerializer s) throws IllegalArgumentException,
			IllegalStateException, IOException {
		
		s.startTag("", "profile");
		s.attribute("", "name", name);

		for (FeatureStyle fp : featureStyles.values()) {
			if  (fp != null) {
				s.startTag("", "feature");
				s.attribute("", "name", fp.getName());
				s.attribute("", "internal", Boolean.toString(fp.isInternal()));
				boolean updateWidth = fp.updateWidth();
				s.attribute("", "updateWidth", Boolean.toString(updateWidth));
				s.attribute("", "widthFactor", Float.toString(fp.getWidthFactor()));
				s.attribute("", "editable", Boolean.toString(fp.isEditable())); 
				//
				s.attribute("", "color", Integer.toHexString(fp.getPaint().getColor()));
				// alpha should be contained in color
				s.attribute("", "style", fp.getPaint().getStyle().toString());
				s.attribute("", "cap", fp.getPaint().getStrokeCap().toString());
				s.attribute("", "join", fp.getPaint().getStrokeJoin().toString());
				if (!updateWidth) {
					s.attribute("", "strokewidth", Float.toString(fp.getPaint().getStrokeWidth()));
				}
				Typeface tf = fp.getPaint().getTypeface();
				if (tf != null) {
					s.attribute("", "typefacestyle", Integer.toString(tf.getStyle()));
					s.attribute("", "textsize", Float.toString(fp.getPaint().getTextSize()));
				}
				DashPath dp = fp.getDashPath();
				if (dp != null) {
					s.startTag("", "dash");
					s.attribute("", "phase",Float.toString(dp.phase));
					for (int i=0;i<dp.intervals.length;i++) {
						s.startTag("", "interval");
						s.attribute("", "length",Float.toString(dp.intervals[i]));
						s.endTag("", "interval");
					}
					s.endTag("", "dash");
				}
				s.endTag("", "feature");
			}
			else {
				Log.d("Style", "null fp");
			}
		}
		s.endTag("", "profile");
	}
	
	/**
	 * save this profile to SDCARD
	 */
	void save() {
		String filename = name + FILE_PATH_STYLE_SUFFIX;
		OutputStream outputStream = null;
		try {
			File outDir = FileUtil.getPublicDirectory();
			File outfile = new File(outDir, filename);
			outputStream = new BufferedOutputStream(new FileOutputStream(outfile));
			XmlSerializer serializer = XmlPullParserFactory.newInstance().newSerializer();
			serializer.setOutput(outputStream, "UTF-8");
			serializer.startDocument("UTF-8", null);
			this.toXml(serializer);
			serializer.endDocument();
		} catch (Exception e) {
			Log.e("Style", "Save failed - " + filename + " " + e);
		} finally {
			try {outputStream.close();} catch (Exception ex) {}
		}
	}
	
	/**
	 * start parsing a config file
	 * @param in
	 * @throws SAXException
	 * @throws IOException
	 * @throws ParserConfigurationException
	 */
	public void start(final InputStream in) throws SAXException, IOException, ParserConfigurationException {
		SAXParserFactory factory = SAXParserFactory.newInstance();
		SAXParser saxParser = factory.newSAXParser();
		saxParser.parse(in, this);
	}
	
	/**
	 * vars for the XML parser
	 */
	FeatureStyle tempFeatureStyle;
	ArrayList<Float> tempIntervals;
	float	tempPhase;
	
	@Override
	public void startElement(final String uri, final String element, final String qName, final Attributes atts) {
		try {
			if (element.equals("profile")) {
				name = atts.getValue("name");
				if (featureStyles == null) {
					Log.i("Style","Allocating new list of feature profiles for profile " + name);
					featureStyles = new HashMap<String, FeatureStyle>();
				}
			} else if (element.equals("feature")) {
//				Log.i("Style", atts.getLength() + " attributes");
//				for (int i=0;i<atts.getLength();i++) {
//					Log.i("Style",atts.getLocalName(i) + "=" + atts.getValue(i));
//				}
				tempFeatureStyle = new FeatureStyle(atts.getValue("name"));
				if (tempFeatureStyle.name.equals(LARGE_DRAG_AREA)) {
					// special handling
					largDragCircleRadius = Density.dpToPx(ctx,Float.parseFloat(atts.getValue("radius")));
					largDragToleranceRadius = Density.dpToPx(ctx,Float.parseFloat(atts.getValue("touchRadius")));
					return;
				}
				if (tempFeatureStyle.name.equals(MARKER_SCALE)) {
					float scale = Float.parseFloat(atts.getValue("scale"));
					orientation_path = new Path();
					orientation_path.moveTo(0, Density.dpToPx(ctx,-20) *scale);
					orientation_path.lineTo(Density.dpToPx(ctx,15) *scale, Density.dpToPx(ctx,20) *scale);
					orientation_path.lineTo(0, Density.dpToPx(ctx,10) *scale);
					orientation_path.lineTo(Density.dpToPx(ctx,-15) *scale, Density.dpToPx(ctx,20) *scale);
					orientation_path.lineTo(0, Density.dpToPx(ctx,-20) *scale);
				
					crosshairs_path = new Path();
					int arm = (int) Density.dpToPx(ctx,10*scale);
					crosshairs_path.moveTo(0, -arm);
					crosshairs_path.lineTo(0, arm);
					crosshairs_path.moveTo(arm, 0);
					crosshairs_path.lineTo(-arm, 0);
					
					x_path = new Path();
					arm = (int) Density.dpToPx(ctx,3*scale);
					x_path.moveTo(-arm, -arm);
					x_path.lineTo(arm, arm);
					x_path.moveTo(arm, -arm);
					x_path.lineTo(-arm, arm);
					return;
				}
				
				tempFeatureStyle.setInternal(Boolean.valueOf(atts.getValue("internal")).booleanValue());
				if (!Boolean.valueOf(atts.getValue("updateWidth")).booleanValue()) {
					tempFeatureStyle.dontUpdate();
				}
				tempFeatureStyle.setWidthFactor(Float.parseFloat(atts.getValue("widthFactor")));
				tempFeatureStyle.setEditable(Boolean.valueOf(atts.getValue("editable")).booleanValue());
				tempFeatureStyle.setColor((int)Long.parseLong(atts.getValue("color"),16)); // workaround highest bit set problem
				
				Style style = Style.valueOf(atts.getValue("style"));
				tempFeatureStyle.getPaint().setStyle(style);
				if (style != Style.STROKE && tempFeatureStyle.getName().startsWith("way-")) { // hack for filled polygons
					tempFeatureStyle.getPaint().setAlpha(125);
				}
				
				tempFeatureStyle.getPaint().setStrokeCap(Cap.valueOf(atts.getValue("cap")));
				tempFeatureStyle.getPaint().setStrokeJoin(Join.valueOf(atts.getValue("join")));
				if (!tempFeatureStyle.updateWidth()) {
					float strokeWidth = Density.dpToPx(ctx,Float.parseFloat(atts.getValue("strokewidth")));
					tempFeatureStyle.getPaint().setStrokeWidth(strokeWidth);
					// special case if we are setting internal tolerance values
					if (tempFeatureStyle.name.equals(NODE_TOLERANCE)) {
						nodeToleranceValue = strokeWidth;
					} else if (tempFeatureStyle.name.equals(WAY_TOLERANCE)) {
						wayToleranceValue = strokeWidth;
					} 
				}
				if (atts.getValue("typefacestyle") != null) {
					tempFeatureStyle.getPaint().setTypeface(Typeface.defaultFromStyle(Integer.parseInt(atts.getValue("typefacestyle"))));
					tempFeatureStyle.getPaint().setTextSize(Density.dpToPx(ctx,Float.parseFloat(atts.getValue("textsize"))));
					if (atts.getValue("shadow") != null)
						tempFeatureStyle.getPaint().setShadowLayer(Integer.parseInt(atts.getValue("shadow")), 0, 0, Color.BLACK);
				}
				// Log.i("Style","startElement finished parsing feature");
			} else if (element.equals("dash")) {
				tempPhase = Float.parseFloat(atts.getValue("phase"));
				tempIntervals = new ArrayList<Float>();
			} else if (element.equals("interval")) {
				tempIntervals.add(Float.valueOf(Float.parseFloat(atts.getValue("length"))));
			} 
		} catch (Exception e) {
			Log.e("Profil", "Parse Exception", e);
		}
	}
	
	@Override
	public void endElement(final String uri, final String element, final String qName) {
		if (element == null) {Log.i("Style","element is null"); return;}
		if (element.equals("profile")) {
	
		} else if (element.equals("feature")) {
			if (tempFeatureStyle == null) {
				Log.i("Style","FeatureStyle is null");
				return;
			}
			if (tempFeatureStyle.getName() == null) {
				Log.i("Style","FeatureStyle name is null");
				return;
			}
			// overwrites existing profiles
			featureStyles.put(tempFeatureStyle.getName(),tempFeatureStyle);
			// Log.i("Style","adding to list of features");
		} else if (element.equals("dash")) {
			float[] tIntervals = new float[tempIntervals.size()];
			for (int i=0; i<tIntervals.length;i++) {tIntervals[i] = tempIntervals.get(i).floatValue();}
			tempFeatureStyle.setDashPath(tIntervals, tempPhase);
		} else if (element.equals("interval")) {
			
		} 
	}
	
	boolean read(InputStream is) {
		InputStream inputStream = null;
		try {
			inputStream = new BufferedInputStream(is);
			start(inputStream);
		} catch (Exception e) {
			Log.e("Style", "Read failed " + e);
			e.printStackTrace();
		}
		return true;
	}
	
	class StyleFilter implements FilenameFilter {
		@Override
		public boolean accept(File dir, String name) {
			return name.endsWith(FILE_PATH_STYLE_SUFFIX);
		}
	}
	
	/**
	 * searches directories for profile files and creates new profiles from them
	 * @param ctx
	 */
	void getStylesFromFile(Context ctx) {
		// assets directory
		AssetManager assetManager = ctx.getAssets();
		//
		try {
			String[] fileList = assetManager.list("");
			if (fileList != null) {
				for (String fn:fileList) {
					if (fn.endsWith(FILE_PATH_STYLE_SUFFIX)) {
						Log.i("Style","Creating profile from file in assets directory " + fn);
						InputStream is = assetManager.open(fn);
						DataStyle p = new DataStyle(ctx, is);
						availableStyles.put(p.name,p);
					}
				}
			}
		} catch (Exception ex) { Log.i("Style", ex.toString());}
		
		// from sdcard
		File sdcard = Environment.getExternalStorageDirectory();
		File indir = new File(sdcard, Paths.DIRECTORY_PATH_VESPUCCI);
		if (indir != null) {
			File[] list = indir.listFiles(new StyleFilter());
			if (list != null) {
				for (File f:list) {
					Log.i("Style","Creating profile from " + f.getName());
					try {
						InputStream is = new FileInputStream(f);
						DataStyle p = new DataStyle(ctx, is);
						// overwrites profile with same name
						availableStyles.put(p.name,p);
					} catch (Exception ex) { Log.i("Style", ex.toString());}
				}
			}
			
		}
	}
	
	public static String getBuiltinStyleName() {
		return BUILTIN_STYLE_NAME;
	}
}


