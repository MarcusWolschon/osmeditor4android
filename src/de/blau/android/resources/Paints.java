package de.blau.android.resources;

import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Join;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.Typeface;
import de.blau.android.R;

public class Paints {

	public final static int WAY = 0;

	public final static int NODE = 1;

	public final static int TRACK = 2;

	public final static int SELECTED_NODE = 3;

	public final static int SELECTED_WAY = 4;

	public final static int NODE_TOLERANCE = 5;

	public final static int INFOTEXT = 6;

	public final static int VIEWBOX = 7;

	public final static int BUILDING = 8;

	public final static int WAY_TOLERANCE = 9;

	public final static int GPS_POS = 10;

	public final static int GPS_ACCURACY = 11;

	public final static int SELECTED_NODE_THIN = 12;

	public final static int NODE_THIN = 13;

	public final static int RAILWAY = 14;

	public final static int FOOTWAY = 15;

	public final static int INTERPOLATION = 16;

	public final static int WATERWAY = 17;

	public final static int BOUNDARY = 18;
	
	public final static int PROBLEM_WAY = 19;

	public final static int PROBLEM_NODE = 20;

	public final static int PROBLEM_NODE_THIN = 21;
	
	public final static int WAY_DIRECTION = 22;
	public final static int ONEWAY_DIRECTION = 23;
	
	private final static int PAINT_COUNT = 24;
		
	private final Paint[] paints;

	public static final float NODE_TOLERANCE_VALUE = 40f;
	
	public static final float NODE_OVERLAP_TOLERANCE_VALUE = 10f;

	public static final float WAY_TOLERANCE_VALUE = 40f;

	private static final int TOLERANCE_ALPHA = 40;
	
	public static final Path ORIENTATION_PATH = new Path();
	
	/**
	 * Arrow indicating the direction of one-way streets. Set/updated in updateStrokes 
	 */
	public static final Path WAY_DIRECTION_PATH = new Path();
	
	public Paints(final Resources resources) {
		paints = new Paint[PAINT_COUNT];
		initPaints(resources);
		ORIENTATION_PATH.moveTo(0,-20);
		ORIENTATION_PATH.lineTo(15, 20);
		ORIENTATION_PATH.lineTo(0, 10);
		ORIENTATION_PATH.lineTo(-15, 20);
		ORIENTATION_PATH.lineTo(0, -20);
	}

	/**
	 * @param resources
	 */
	private void initPaints(final Resources resources) {
		Paint paint;

		Paint standardPath = new Paint();
		standardPath.setStyle(Style.STROKE);
		// As nodes cover the line ends/joins, the line ending styles are irrelevant for most paints
		// However, at least on the software renderer, the default styles (Cap = BUTT, Join = MITER)
		// have slightly better performance than the round styles.

		paint = new Paint(standardPath);
		paint.setColor(Color.BLACK);
		paints[WAY] = paint;
		
		paint = new Paint(standardPath);
		paint.setColor(resources.getColor(R.color.problem));
		paints[PROBLEM_WAY] = paint;
		
		paint = new Paint(standardPath);
		paint.setColor(Color.BLUE);
		paints[WATERWAY] = paint;
		
		paint = new Paint(standardPath);
		paint.setColor(Color.GRAY);
		paints[FOOTWAY] = paint;
		
		paint = new Paint(standardPath);
		paint.setColor(Color.WHITE);
		paints[RAILWAY] = paint;

		paint = new Paint(standardPath);
		paint.setColor(resources.getColor(R.color.building));
		paints[BUILDING] = paint;

		paint = new Paint();
		paint.setColor(resources.getColor(R.color.grey));
		paint.setStyle(Style.FILL);
		paint.setAlpha(125);
		paints[VIEWBOX] = paint;

		paint = new Paint();
		paint.setColor(resources.getColor(R.color.ccc_red));
		paints[NODE] = paint;

		paint = new Paint();
		paint.setColor(resources.getColor(R.color.ccc_red));
		paints[NODE_THIN] = paint;

		paint = new Paint();
		paint.setColor(resources.getColor(R.color.problem));
		paints[PROBLEM_NODE] = paint;

		paint = new Paint();
		paint.setColor(resources.getColor(R.color.problem));
		paints[PROBLEM_NODE_THIN] = paint;

		paint = new Paint(paints[WAY]);
		paint.setColor(Color.BLUE);
		paint.setStrokeCap(Cap.ROUND);
		paint.setStrokeJoin(Join.ROUND);
		paints[TRACK] = paint;

		paint = new Paint(paints[WAY]);
		paint.setColor(resources.getColor(R.color.ccc_ocher));
		paint.setAlpha(TOLERANCE_ALPHA);
		paint.setStrokeWidth(WAY_TOLERANCE_VALUE);
		paints[WAY_TOLERANCE] = paint;

		paint = new Paint();
		paint.setColor(resources.getColor(R.color.ccc_beige));
		paints[SELECTED_NODE] = paint;

		paint = new Paint();
		paint.setColor(resources.getColor(R.color.ccc_beige));
		paints[SELECTED_NODE_THIN] = paint;

		paint = new Paint(paints[TRACK]);
		paint.setStyle(Style.FILL);
		paints[GPS_POS] = paint;

		paint = new Paint(paints[GPS_POS]);
		paint.setStyle(Style.FILL_AND_STROKE);
		paint.setAlpha(TOLERANCE_ALPHA);
		paints[GPS_ACCURACY] = paint;

		paint = new Paint(standardPath);
		paint.setColor(resources.getColor(R.color.tertiary));
		paint.setStrokeCap(Cap.ROUND);
		paint.setStrokeJoin(Join.ROUND);
		paints[SELECTED_WAY] = paint;

		paint = new Paint(standardPath);
		paint.setColor(Color.BLACK);
		DashPathEffect dashPath = new DashPathEffect(new float[]{5,5}, 1);
		paint.setPathEffect(dashPath);
		paints[INTERPOLATION] = paint;

		paint = new Paint(standardPath);
		paint.setColor(Color.BLACK);
		dashPath = new DashPathEffect(new float[]{20,5}, 1);
		paint.setPathEffect(dashPath);
		paints[BOUNDARY] = paint;
	
		paint = new Paint();
		paint.setColor(resources.getColor(R.color.ccc_ocher));
		paint.setStyle(Style.FILL);
		paint.setAlpha(TOLERANCE_ALPHA);
		paint.setStrokeWidth(NODE_TOLERANCE_VALUE);
		paints[NODE_TOLERANCE] = paint;

		paint = new Paint();
		paint.setColor(Color.BLACK);
		paint.setTypeface(Typeface.SANS_SERIF);
		paint.setTextSize(12);
		paints[INFOTEXT] = paint;
		
		paint = new Paint();
		paint.setColor(resources.getColor(R.color.ccc_red));
		paint.setStyle(Style.STROKE);
		paint.setStrokeCap(Cap.SQUARE);
		paint.setStrokeJoin(Join.MITER);
		paints[WAY_DIRECTION] = paint;
		
		paint = new Paint(paints[WAY_DIRECTION]);
		paint.setColor(resources.getColor(R.color.ccc_blue));
		paints[ONEWAY_DIRECTION] = paint;
	}

	public void setAntiAliasing(final boolean aa) {
		for (Paint paint : paints) {
			paint.setAntiAlias(aa);
		}
	}

	/**
	 * Sets the stroke width of all Elements corresponding to the width of the viewbox (=zoomfactor).
	 */
	public void updateStrokes(final float newStrokeWidth) {
		paints[RAILWAY].setStrokeWidth(newStrokeWidth * 0.7f);
		paints[WAY].setStrokeWidth(newStrokeWidth);
		paints[PROBLEM_WAY].setStrokeWidth(newStrokeWidth * 1.5f);
		paints[WATERWAY].setStrokeWidth(newStrokeWidth);

		paints[BOUNDARY].setStrokeWidth(newStrokeWidth * 0.6f);
		DashPathEffect dashPath = new DashPathEffect(new float[]{2 * newStrokeWidth * 2.4f, 2 * newStrokeWidth * 0.6f}, 1);
		paints[BOUNDARY].setPathEffect(dashPath);

		paints[INTERPOLATION].setStrokeWidth(newStrokeWidth * 0.6f);
		dashPath = new DashPathEffect(new float[]{2 * newStrokeWidth * 0.6f, 2 * newStrokeWidth * 0.6f}, 1);
		paints[INTERPOLATION].setPathEffect(dashPath);

		paints[FOOTWAY].setStrokeWidth(newStrokeWidth * 0.6f);
		paints[BUILDING].setStrokeWidth(newStrokeWidth);
		paints[TRACK].setStrokeWidth(newStrokeWidth);
		paints[NODE].setStrokeWidth(newStrokeWidth * 1.5f);
		paints[NODE_THIN].setStyle(Style.STROKE);
		paints[PROBLEM_NODE].setStrokeWidth(newStrokeWidth * 1.5f);
		paints[PROBLEM_NODE_THIN].setStyle(Style.STROKE);
		paints[SELECTED_NODE_THIN].setStyle(Style.STROKE);
		paints[SELECTED_NODE].setStrokeWidth(newStrokeWidth * 2f);
		paints[SELECTED_WAY].setStrokeWidth(newStrokeWidth * 2f);
		paints[GPS_POS].setStrokeWidth(newStrokeWidth * 2f);
		
		paints[WAY_DIRECTION].setStrokeWidth(newStrokeWidth * 0.8f);
		paints[ONEWAY_DIRECTION].setStrokeWidth(newStrokeWidth * 0.5f);
		
		WAY_DIRECTION_PATH.rewind();
		float wayDirectionPathOffset = newStrokeWidth * 2.0f;
		WAY_DIRECTION_PATH.moveTo(-wayDirectionPathOffset, -wayDirectionPathOffset);
		WAY_DIRECTION_PATH.lineTo(0,0);
		WAY_DIRECTION_PATH.lineTo(-wayDirectionPathOffset, +wayDirectionPathOffset);
	}

	public Paint get(final int key) {
		if (key >= 0 && key < PAINT_COUNT) {
			return paints[key];
		}
		return null;
	}

}
