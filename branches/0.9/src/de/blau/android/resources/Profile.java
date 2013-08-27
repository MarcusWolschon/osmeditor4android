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
import java.io.Serializable;
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
import android.graphics.Typeface;
import android.os.Environment;
import android.util.Log;
import de.blau.android.R;
import de.blau.android.exception.OsmParseException;
import de.blau.android.resources.Profile.FeatureProfile;
import de.blau.android.resources.Profile.FeatureProfile.DashPath;
import de.blau.android.util.SavingHelper;

public class Profile  extends DefaultHandler {
	
	// constants for the internal profiles
	public final static String GPS_TRACK = "gps_track";
	public final static String NODE_TOLERANCE = "node_tolerance";
	public final static String INFOTEXT = "infotext";
	public final static String VIEWBOX = "viewbox";
	public final static String WAY = "way";
	public final static String SELECTED_WAY = "selected_way";
	public final static String PROBLEM_WAY = "problem_way";
	public final static String NODE = "node";
	public final static String NODE_THIN = "node_thin";
	public static final String NODE_TAGGED = "node_tagged";
	public final static String PROBLEM_NODE = "problem_node";
	public final static String PROBLEM_NODE_THIN = "problem_node_thin";
	public static final String PROBLEM_NODE_TAGGED = "problem_node_tagged";
	public final static String SELECTED_NODE = "selected_node";
	public final static String SELECTED_NODE_THIN = "selected_node_thin";
	public static final String SELECTED_NODE_TAGGED = "selected_node_tagged";
	public final static String WAY_DIRECTION = "way_direction";
	public final static String ONEWAY_DIRECTION = "oneway_direction";
	public final static String WAY_TOLERANCE = "way_tolerance";
	public final static String GPS_POS = "gps_pos";
	public final static String GPS_POS_FOLLOW = "gps_pos_follow";
	public final static String GPS_ACCURACY = "gps_accuracy";
	public final static String OPEN_NOTE = "open_note";
	public final static String CLOSED_NOTE = "closed_note";
	public final static String CROSSHAIRS = "crosshairs";
	
	
	public class FeatureProfile {
		
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
		
		FeatureProfile (String n, Paint p) {
			Log.i("FeatureProfile","setting up feature " + n);
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
		
		FeatureProfile (String n) {
			this(n, (Paint)null);
		}
		
		FeatureProfile(String n, FeatureProfile fp)
		{
			if (fp == null) {
				Log.i("FeatureProfile","setting up feature " + n + " profile is null!");
				return;
			}
			Log.i("FeatureProfile","setting up feature " + n + " from " + fp.getName());
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
	HashMap<String,FeatureProfile> featureProfiles;
	
	public static Profile currentProfile;
	public static HashMap<String,Profile> availableProfiles;
	
	public static final float NODE_OVERLAP_TOLERANCE_VALUE = 10f;

	private static final int TOLERANCE_ALPHA = 40;
	
	/**
	 * GPS arrow
	 */
	public static final Path ORIENTATION_PATH = new Path();
	
	/**
	 * Crosshairs
	 */
	public static final Path CROSSHAIRS_PATH = new Path();
	
	/**
	 * Arrow indicating the direction of one-way streets. Set/updated in updateStrokes 
	 */
	public static final Path WAY_DIRECTION_PATH = new Path();
	
	private static final String DEFAULT_PROFILE_NAME = "Default";
	
	static Resources myRes;
	
	public float nodeToleranceValue = 40f;
	public float wayToleranceValue = 40f;
	
	public Profile(final Context ctx) {
		// create default 
		myRes = ctx.getResources();
		init(myRes);

		getProfilesFromFile(ctx);
		Log.i("Profile","profile " + currentProfile.name);
	}

	
	public Profile(String n, Profile from) {
		// copy existing profile
		name = n;
		featureProfiles = new HashMap<String, FeatureProfile>();
		for (FeatureProfile fp : from.featureProfiles.values()) {
			featureProfiles.put(fp.getName(),new FeatureProfile(fp.getName(), fp));
		}
	}
	
	public Profile(InputStream is) {
		// create a profile from a file
		init(myRes); // defaults for internal styles 
		read(is);
	}
	
	/**
	 * initialize the minimum required internal style for a new profile
	 * @param resources
	 */
	private void init(final Resources resources) {
		
		ORIENTATION_PATH.moveTo(0,-20);
		ORIENTATION_PATH.lineTo(15, 20);
		ORIENTATION_PATH.lineTo(0, 10);
		ORIENTATION_PATH.lineTo(-15, 20);
		ORIENTATION_PATH.lineTo(0, -20);
		
		CROSSHAIRS_PATH.moveTo(0, -10);
		CROSSHAIRS_PATH.lineTo(0, 10);
		CROSSHAIRS_PATH.moveTo(10, 0);
		CROSSHAIRS_PATH.lineTo(-10, 0);
		
		Log.i("Profile","setting up default profile elements");
		featureProfiles = new HashMap<String, FeatureProfile>();

		Paint standardPath = new Paint();
		standardPath.setStyle(Style.STROKE);
		// As nodes cover the line ends/joins, the line ending styles are irrelevant for most paints
		// However, at least on the software renderer, the default styles (Cap = BUTT, Join = MITER)
		// have slightly better performance than the round styles.

		FeatureProfile fp = new FeatureProfile(WAY, standardPath);
		fp.setColor(Color.BLACK);
		featureProfiles.put(fp.getName(), fp);
		
		fp = new FeatureProfile(PROBLEM_WAY, standardPath);
		fp.setColor(resources.getColor(R.color.problem));
		fp.setWidthFactor(1.5f);
		featureProfiles.put(fp.getName(), fp);

		fp = new FeatureProfile(VIEWBOX, standardPath);
		fp.setColor(resources.getColor(R.color.grey));
		fp.dontUpdate();
		fp.getPaint().setStyle(Style.FILL);
		fp.getPaint().setAlpha(125);
		featureProfiles.put(fp.getName(), fp);
		
		fp = new FeatureProfile(NODE);
		fp.setColor(resources.getColor(R.color.ccc_red));
		fp.setWidthFactor(1f);
		featureProfiles.put(fp.getName(), fp);
		
		fp = new FeatureProfile(NODE_TAGGED);
		fp.setColor(resources.getColor(R.color.ccc_red));
		fp.setWidthFactor(1.5f);
		featureProfiles.put(fp.getName(), fp);

		fp = new FeatureProfile(NODE_THIN);
		fp.dontUpdate();
		fp.getPaint().setStrokeWidth(1.0f);
		fp.setColor(resources.getColor(R.color.ccc_red));
		fp.getPaint().setStyle(Style.STROKE);
		featureProfiles.put(fp.getName(), fp);
		
		fp = new FeatureProfile(PROBLEM_NODE);
		fp.setColor(resources.getColor(R.color.problem));
		fp.setWidthFactor(1f);
		featureProfiles.put(fp.getName(), fp);
		
		fp = new FeatureProfile(PROBLEM_NODE_TAGGED);
		fp.setColor(resources.getColor(R.color.problem));
		fp.setWidthFactor(1.5f);
		featureProfiles.put(fp.getName(), fp);

		fp = new FeatureProfile(PROBLEM_NODE_THIN);
		fp.dontUpdate();
		fp.getPaint().setStrokeWidth(1.0f);
		fp.setColor(resources.getColor(R.color.problem));
		fp.getPaint().setStyle(Style.STROKE);
		featureProfiles.put(fp.getName(), fp);
		
		fp = new FeatureProfile(GPS_TRACK,featureProfiles.get(WAY)); 	
		fp.setColor(Color.BLUE);
		fp.getPaint().setStrokeCap(Cap.ROUND);
		fp.getPaint().setStrokeJoin(Join.ROUND);
		featureProfiles.put(fp.getName(), fp);
		
		fp = new FeatureProfile(WAY_TOLERANCE,featureProfiles.get(WAY)); 	
		fp.setColor(resources.getColor(R.color.ccc_ocher));
		fp.dontUpdate();
		fp.getPaint().setAlpha(TOLERANCE_ALPHA);
		fp.getPaint().setStrokeWidth(wayToleranceValue);
		featureProfiles.put(fp.getName(), fp);
		
		fp = new FeatureProfile(SELECTED_NODE);
		fp.setColor(resources.getColor(R.color.ccc_beige));
		fp.setWidthFactor(1.5f);
		featureProfiles.put(fp.getName(), fp);
		
		fp = new FeatureProfile(SELECTED_NODE_TAGGED);
		fp.setColor(resources.getColor(R.color.ccc_beige));
		fp.setWidthFactor(2f);
		featureProfiles.put(fp.getName(), fp);

		fp = new FeatureProfile(SELECTED_NODE_THIN);
		fp.dontUpdate();
		fp.getPaint().setStrokeWidth(1.0f);
		fp.setColor(resources.getColor(R.color.ccc_beige));
		fp.getPaint().setStyle(Style.STROKE);
		featureProfiles.put(fp.getName(), fp);
		
		fp = new FeatureProfile(GPS_POS,featureProfiles.get(GPS_TRACK)); 
		fp.getPaint().setStyle(Style.FILL);
		fp.setWidthFactor(2f);
		featureProfiles.put(fp.getName(), fp);
		
		fp = new FeatureProfile(GPS_POS_FOLLOW,featureProfiles.get(GPS_POS)); 
		fp.getPaint().setStyle(Style.STROKE);
		fp.getPaint().setStrokeWidth(4.0f);
		fp.dontUpdate();
		featureProfiles.put(fp.getName(), fp);

		fp = new FeatureProfile(GPS_ACCURACY,featureProfiles.get(GPS_POS));
		fp.getPaint().setStyle(Style.FILL_AND_STROKE);
		fp.getPaint().setAlpha(TOLERANCE_ALPHA);
		fp.dontUpdate();
		featureProfiles.put(fp.getName(), fp);

		fp = new FeatureProfile(SELECTED_WAY,featureProfiles.get(WAY)); 	
		fp.setColor(resources.getColor(R.color.tertiary));
		fp.setWidthFactor(2f);
		fp.getPaint().setStrokeCap(Cap.ROUND);
		fp.getPaint().setStrokeJoin(Join.ROUND);
		featureProfiles.put(fp.getName(), fp);

		fp = new FeatureProfile(NODE_TOLERANCE);
		fp.setColor(resources.getColor(R.color.ccc_ocher));
		fp.dontUpdate();
		fp.getPaint().setStyle(Style.FILL);
		fp.getPaint().setAlpha(TOLERANCE_ALPHA);
		fp.getPaint().setStrokeWidth(nodeToleranceValue);
		featureProfiles.put(fp.getName(), fp);

		fp = new FeatureProfile(INFOTEXT);
		fp.setColor(Color.BLACK);
		fp.dontUpdate();
		fp.getPaint().setTypeface(Typeface.SANS_SERIF);
		fp.getPaint().setTextSize(12);
		featureProfiles.put(fp.getName(), fp);
		
		fp = new FeatureProfile(WAY_DIRECTION);
		fp.setColor(resources.getColor(R.color.ccc_red));
		fp.setWidthFactor(0.8f);
		fp.getPaint().setStyle(Style.STROKE);
		fp.getPaint().setStrokeCap(Cap.SQUARE);
		fp.getPaint().setStrokeJoin(Join.MITER);
		featureProfiles.put(fp.getName(), fp);
		
		fp = new FeatureProfile(ONEWAY_DIRECTION,featureProfiles.get(WAY_DIRECTION));
		fp.setColor(resources.getColor(R.color.ccc_blue));
		fp.setWidthFactor(0.5f);
		featureProfiles.put(fp.getName(), fp);
		
		fp = new FeatureProfile(OPEN_NOTE);
		fp.setColor(resources.getColor(R.color.bug_open));
		fp.getPaint().setAlpha(100);
		featureProfiles.put(fp.getName(), fp);
		
		fp = new FeatureProfile(CLOSED_NOTE);
		fp.setColor(resources.getColor(R.color.bug_closed));
		fp.getPaint().setAlpha(100);
		featureProfiles.put(fp.getName(), fp);
		
		fp = new FeatureProfile(CROSSHAIRS); 
		fp.getPaint().setStyle(Style.STROKE);
		fp.getPaint().setStrokeWidth(1.0f);
		fp.dontUpdate();
		featureProfiles.put(fp.getName(), fp);
		
		if (availableProfiles == null) {
			name = DEFAULT_PROFILE_NAME;
			currentProfile = this;
			availableProfiles = new HashMap<String,Profile>();
			availableProfiles.put(name,this);
		}
		Log.i("Profile","... done");
	}

	public static void setAntiAliasing(final boolean aa) {
		for (FeatureProfile fp : currentProfile.featureProfiles.values()) {
			fp.getPaint().setAntiAlias(aa);
		}
	}

	/**
	 * Sets the stroke width of all Elements corresponding to the width of the viewbox (=zoomfactor).
	 */
	public static void updateStrokes(final float newStrokeWidth) {
	

		for (FeatureProfile fp : currentProfile.featureProfiles.values()) {
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
	 * get FeatureProfile specified by key from current profile
	 * @param key
	 * @return
	 */
	public static FeatureProfile getCurrent(final String key) {
		return currentProfile.featureProfiles.get(key);
	}
	
	/**
	 * 
	 * @return
	 */
	public static Profile getCurrent() {
		return currentProfile;
	}
	
	/**
	 * get FeatureProfile specified by key from this profile
	 * @param key
	 * @return
	 */
	public FeatureProfile get(final String key) {
		return featureProfiles.get(key);
	}
	
	/**
	 * return specific named profile
	 * @param n
	 * @return
	 */
	public static Profile getProfile(String n) {
		return availableProfiles.get(n);
	}
	
	/**
	 * return list of available profiles (Defaut entry first, rest sorted)
	 * @return
	 */
	public static String[] getProfileList() {
		String[] res = new String[availableProfiles.size()];
		
		res[0] = DEFAULT_PROFILE_NAME;
		String keys[] = (new TreeMap<String, Profile>(availableProfiles)).keySet().toArray(new String[0]); // sort the list
		int j = 1;
		for (int i=0;i<res.length;i++) {
			if (!keys[i].equals(DEFAULT_PROFILE_NAME)) {
				res[j] = keys[i];
				j++;
			}
		}    // probably silly way of doing this
		return res;
	}

	/**
	 * switch to profile with name n
	 * @param n
	 * @return
	 */
	public static boolean switchTo(String n) {
		Profile p = getProfile(n);
		if (p != null) {
			currentProfile = p;
			Log.i("Profile","Switching to " + n);
			return true;
		}
		return false;
	}
	

	/**
	 * dump this profile in XML format, not very abstracted and closely tied to the implementation
	 * @param s
	 * @throws IllegalArgumentException
	 * @throws IllegalStateException
	 * @throws IOException
	 */
	public void toXml(final XmlSerializer s) throws IllegalArgumentException,
			IllegalStateException, IOException {
		
		s.startTag("", "profile");
		s.attribute("", "name", name);

		for (FeatureProfile fp : featureProfiles.values()) {
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
				Log.d("Profile", "null fp");
			}
		}
		s.endTag("", "profile");
	}
	
	/**
	 * save this profile to SDCARD
	 */
	void save() {
		File sdcard = Environment.getExternalStorageDirectory();
		File outdir = new File(sdcard, "Vespucci");
		outdir.mkdir(); // ensure directory exists;
		String filename = name + "-profile.xml";
		File outfile = new File(outdir, filename);
		OutputStream outputStream = null;
		try {
			outputStream = new BufferedOutputStream(new FileOutputStream(outfile));
			XmlSerializer serializer = XmlPullParserFactory.newInstance().newSerializer();
			serializer.setOutput(outputStream, "UTF-8");
			serializer.startDocument("UTF-8", null);;
			this.toXml(serializer);
			serializer.endDocument();
		} catch (Exception e) {
			Log.e("Profile", "Save failed - " + filename + " " + e);
		} finally {
			try {outputStream.close();} catch (Exception ex) {};
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
	FeatureProfile tempFeatureProfile;
	ArrayList<Float> tempIntervals;
	float	tempPhase;
	
	@Override
	public void startElement(final String uri, final String element, final String qName, final Attributes atts) {
		try {
			if (element.equals("profile")) {
				name = atts.getValue("name");
				if (featureProfiles == null) {
					Log.i("Profile","Allocating new list of feature profiles for profile " + name);
					featureProfiles = new HashMap<String, FeatureProfile>();
				}
			} else if (element.equals("feature")) {
				Log.i("Profile", atts.getLength() + " attributes");
				for (int i=0;i<atts.getLength();i++) {
					Log.i("Profile",atts.getLocalName(i) + "=" + atts.getValue(i));
				}
				tempFeatureProfile = new FeatureProfile(atts.getValue("name"));
				tempFeatureProfile.setInternal(Boolean.valueOf(atts.getValue("internal")).booleanValue());
				if (!Boolean.valueOf(atts.getValue("updateWidth")).booleanValue()) {
					tempFeatureProfile.dontUpdate();
				}
				tempFeatureProfile.setWidthFactor(Float.parseFloat(atts.getValue("widthFactor")));
				tempFeatureProfile.setEditable(Boolean.valueOf(atts.getValue("editable")).booleanValue());
				tempFeatureProfile.setColor((int)Long.parseLong(atts.getValue("color"),16)); // workaround highest bit set problem
				tempFeatureProfile.getPaint().setStyle(Style.valueOf(atts.getValue("style")));
				
				tempFeatureProfile.getPaint().setStrokeCap(Cap.valueOf(atts.getValue("cap")));
				tempFeatureProfile.getPaint().setStrokeJoin(Join.valueOf(atts.getValue("join")));
				if (!tempFeatureProfile.updateWidth()) {
					float strokeWidth = Float.parseFloat(atts.getValue("strokewidth"));
					tempFeatureProfile.getPaint().setStrokeWidth(strokeWidth);
					// special case if we are setting internal tolerance values
					if (tempFeatureProfile.name.equals(NODE_TOLERANCE)) {
						nodeToleranceValue = strokeWidth;
					} else if (tempFeatureProfile.name.equals(WAY_TOLERANCE)) {
						wayToleranceValue = strokeWidth;
					}
				}
				if (atts.getValue("typefacestyle") != null) {
					tempFeatureProfile.getPaint().setTypeface(Typeface.defaultFromStyle(Integer.parseInt(atts.getValue("typefacestyle"))));
					tempFeatureProfile.getPaint().setTextSize(Float.parseFloat(atts.getValue("textsize")));
				}
				Log.i("Profile","startElement finshed parsing feature");
			} else if (element.equals("dash")) {
				tempPhase = Float.parseFloat(atts.getValue("phase"));
				tempIntervals = new ArrayList<Float>();
			} else if (element.equals("interval")) {
				tempIntervals.add(new Float(Float.parseFloat(atts.getValue("length"))));
			} 
		} catch (Exception e) {
			Log.e("Profil", "Parse Exception", e);
		}
	}
	
	public void endElement(final String uri, final String element, final String qName) {
		if (element == null) {Log.i("Profile","element is null"); return;};
		if (element.equals("profile")) {
	
		} else if (element.equals("feature")) {
			if (tempFeatureProfile == null) {
				Log.i("Profile","FeatureProfile is null");
			}
			if (tempFeatureProfile.getName() == null) {
				Log.i("Profile","FeatureProfile name is null");
			}
			// overwrites existing profiles
			featureProfiles.put(tempFeatureProfile.getName(),tempFeatureProfile);
			Log.i("Profile","adding to list of features");
		} else if (element.equals("dash")) {
			float[] tIntervals = new float[tempIntervals.size()];
			for (int i=0; i<tIntervals.length;i++) {tIntervals[i] = tempIntervals.get(i).floatValue();}
			tempFeatureProfile.setDashPath(tIntervals, tempPhase);
		} else if (element.equals("interval")) {
			
		} 
	}
	
	boolean read(InputStream is) {
		InputStream inputStream = null;
		try {
			inputStream = new BufferedInputStream(is);
			start(inputStream);
		} catch (Exception e) {
			Log.e("Profile", "Read failed " + e);
			e.printStackTrace();
		}
		return true;
	}
	
	class ProfileFilter implements FilenameFilter {
		public boolean accept(File dir, String name) {
			return name.endsWith("-profile.xml");
		}
	}
	
	/**
	 * searches directories for profile files and creates new profiles from them
	 * @param ctx
	 */
	void getProfilesFromFile(Context ctx) {
		// assets directory
		AssetManager assetManager = ctx.getAssets();
		//
		try {
			String[] fileList = assetManager.list("");
			if (fileList != null) {
				for (String fn:fileList) {
					if (fn.endsWith("-profile.xml")) {
						Log.i("Profile","Creating profile from file in assets directory " + fn);
						InputStream is = assetManager.open(fn);
						Profile p = new Profile(is);
						availableProfiles.put(p.name,p);
					}
				}
			}
		} catch (Exception ex) { Log.i("Profile", ex.toString());}
		
		// from sdcard
		File sdcard = Environment.getExternalStorageDirectory();
		File indir = new File(sdcard, "Vespucci");
		if (indir != null) {
			File[] list = indir.listFiles(new ProfileFilter());
			if (list != null) {
				for (File f:list) {
					Log.i("Profile","Creating profile from " + f.getName());
					try {
						InputStream is = new FileInputStream(f);
						Profile p = new Profile(is);
						// overwrites profile with same name
						availableProfiles.put(p.name,p);
					} catch (Exception ex) { Log.i("Profile", ex.toString());}
				}
			}
			
		}
	}
	
	public static String getDefaultProfileName() {
		return "Default"; //TODO move to resources
	}
}


