package de.blau.android.resources;

import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Join;
import android.graphics.Paint.Style;
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

	public final static int PRESELECTED = 12;

	private final static int PAINT_COUNT = 13;

	private final Paint[] paints;

	public static final float NODE_TOLERANCE_VALUE = 40f;

	public static final float WAY_TOLERANCE_VALUE = 40f;

	private static final int TOLERANCE_ALPHA = 40;

	public Paints(final Resources resources) {
		paints = new Paint[PAINT_COUNT];
		initPaints(resources);
	}

	/**
	 * @param resources
	 */
	private void initPaints(final Resources resources) {
		Paint paint;

		Paint standardPath = new Paint();
		standardPath.setStyle(Style.STROKE);
		standardPath.setStrokeCap(Cap.ROUND);
		standardPath.setStrokeJoin(Join.ROUND);

		paint = new Paint(standardPath);
		paint.setColor(Color.BLACK);
		paints[WAY] = paint;

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

		paint = new Paint(paints[WAY]);
		paint.setColor(Color.BLUE);
		paints[TRACK] = paint;

		paint = new Paint(paints[WAY]);
		paint.setColor(resources.getColor(R.color.ccc_ocher));
		paint.setAlpha(TOLERANCE_ALPHA);
		paint.setStrokeWidth(WAY_TOLERANCE_VALUE);
		paints[WAY_TOLERANCE] = paint;

		paint = new Paint();
		paint.setColor(resources.getColor(R.color.ccc_beige));
		paints[SELECTED_NODE] = paint;

		paint = new Paint(paints[TRACK]);
		paint.setStyle(Style.FILL);
		paints[GPS_POS] = paint;

		paint = new Paint(paints[GPS_POS]);
		paint.setStyle(Style.FILL_AND_STROKE);
		paint.setAlpha(TOLERANCE_ALPHA);
		paints[GPS_ACCURACY] = paint;

		paint = new Paint(standardPath);
		paint.setColor(resources.getColor(R.color.tertiary));
		paints[SELECTED_WAY] = paint;

		paint = new Paint(paints[SELECTED_WAY]);
		paint.setColor(resources.getColor(R.color.preselected));
		paints[PRESELECTED] = paint;

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
		paints[WAY].setStrokeWidth(newStrokeWidth);
		paints[BUILDING].setStrokeWidth(newStrokeWidth);
		paints[TRACK].setStrokeWidth(newStrokeWidth);
		paints[NODE].setStrokeWidth(newStrokeWidth * 1.5f);
		paints[SELECTED_NODE].setStrokeWidth(newStrokeWidth * 2f);
		paints[SELECTED_WAY].setStrokeWidth(newStrokeWidth * 2f);
		paints[PRESELECTED].setStrokeWidth(newStrokeWidth * 2f);
		paints[GPS_POS].setStrokeWidth(newStrokeWidth * 2f);
	}

	public Paint get(final int key) {
		if (key >= 0 && key < PAINT_COUNT) {
			return paints[key];
		}
		return null;
	}

}
