package de.blau.android.photos;

import java.io.File;
import java.io.IOException;

import android.media.ExifInterface;
import android.net.Uri;
import android.util.Log;
import de.blau.android.osm.BoundingBox;
import de.blau.android.util.ExtendedExifInterface;
import de.blau.android.util.rtree.BoundedObject;


/**
 * a photo somewhere on the device or possibly on the network
 * exif accessing code from http://www.blog.nathanhaze.com/how-to-get-exif-tags-gps-coordinates-time-date-from-a-picture/
 */
public class Photo implements BoundedObject {
		
	private static final String DEBUG_TAG = "Photo";
	/**  */
	private final String ref;
	/** Latitude *1E7. */
	private final int lat;
	/** Longitude *1E7. */
	private final int lon;
	/**
	 * compass direction
	 */
	private int direction = 0;
	private String directionRef = null; // if null direction not present
	
	/**
	 * Create a Bug from an OSB GPX XML wpt element.
	 * @param parser Parser up to a wpt element.
	 * @throws IOException If there was a problem parsing the XML.
	 * @throws NumberFormatException If there was a problem parsing the XML.
	 */
	public Photo(File d, File f) throws IOException, NumberFormatException {
		// 
		ExtendedExifInterface exif = new ExtendedExifInterface(f.toString()); // create the ExifInterface file

		/** get the attribute. rest of the attributes are the same. i will add convertToDegree on the bottom (not required) **/
		String lonStr = exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE);
		if (lonStr == null) {
			throw new IOException("No EXIF tag");
		}
		float lonf = convertToDegree(lonStr);

		String lonRef = exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF);
		if(lonRef != null && !lonRef.equals("E"))	{ // deal with the negative degrees
			lonf = -lonf;	
		}
		
		float latf = convertToDegree(exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE));
		
		String latRef = exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF);
		if(latRef != null && !latRef.equals("N"))	{
			latf = -latf;
		}
		lat = (int)(latf * 1E7d);
		lon = (int)(lonf * 1E7d);
		Log.d(DEBUG_TAG,"lat: " + lat + " lon: " + lon);
		ref = d.getAbsolutePath() + "/" + f.getName();
		String dir = exif.getAttribute(ExtendedExifInterface.TAG_GPS_IMG_DIRECTION);
		if (dir != null) {
			direction =(int) Double.parseDouble(dir);
			directionRef = exif.getAttribute(ExtendedExifInterface.TAG_GPS_IMG_DIRECTION_REF);
			Log.d(DEBUG_TAG,"dir " + dir + " direction " + direction + " ref " + directionRef);
		}
	}
	
	public Photo(int lat, int lon, String ref) {
		this.lat = lat;
		this.lon = lon;
		this.ref = ref;		
	}
	
	public Photo(int lat, int lon, int direction, String ref) {
		this.lat = lat;
		this.lon = lon;
		this.direction = direction;
		this.directionRef = "M"; // magnetic north
		// Log.d("Photo","constructor direction " + direction + " ref " + directionRef);
		this.ref = ref;
	}
	
	private Float convertToDegree(String stringDMS) throws NumberFormatException {
		try {
			Float result = null;
			String[] DMS = stringDMS.split(",", 3);

			String[] stringD = DMS[0].split("/", 2);
			Double D0 = Double.valueOf(stringD[0]);
			Double D1 = Double.valueOf(stringD[1]);
			Double FloatD = D0/D1;

			String[] stringM = DMS[1].split("/", 2);
			Double M0 = Double.valueOf(stringM[0]);
			Double M1 = Double.valueOf(stringM[1]);
			Double FloatM = M0/M1;

			String[] stringS = DMS[2].split("/", 2);
			Double S0 = Double.valueOf(stringS[0]);
			Double S1 = Double.valueOf(stringS[1]);
			Double FloatS = S0/S1;

			result = new Float(FloatD + (FloatM/60) + (FloatS/3600));

			return result;
		} catch (Exception ex) {
			Log.e(DEBUG_TAG,"couldn't parse >" + stringDMS + "< exception " + ex);
			throw new NumberFormatException("couldn't parse: " + stringDMS);
		}
   	}
	
	/**
	 * Get the latitude of the bug.
	 * @return The latitude *1E7.
	 */
	public int getLat() {
		return lat;
	}
	
	/**
	 * Get the longitude of the bug.
	 * @return The longitude *1E7.
	 */
	public int getLon() {
		return lon;
	}
	
	/**
	 *  should probable encode ref as URI
	 */
	public Uri getRef() {
		Log.d(DEBUG_TAG, "Uri " +  Uri.fromFile(new File(ref)));
		return Uri.fromFile(new File(ref));
		//return Uri.parse("content:/" + ref);
	}
	
	/**
	 * did we have a direction attribute
	 */
	public boolean hasDirection() {
		return directionRef != null;
	}
	
	/**
	 * get the direction value
	 * @return
	 */
	public int getDirection() {
		return direction;
	}
	
	/**
	 * BoundedObject interface
	 */
	@SuppressWarnings("UnnecessaryLocalVariable")
	public BoundingBox getBounds() {
		return new BoundingBox(lon,lat);
	}
}
