package de.blau.android.photos;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.media.ExifInterface;
import android.net.ParseException;
import android.net.Uri;
import android.os.Environment;
import android.text.Html;
import android.util.Log;


/**
 * a photo somewhere on the device or possibly on the network
 * exif accessing code from http://www.blog.nathanhaze.com/how-to-get-exif-tags-gps-coordinates-time-date-from-a-picture/
 */
public class Photo {
		
	/**  */
	String ref;
	/** Latitude *1E7. */
	int lat;
	/** Longitude *1E7. */
	int lon;
	
	
	/**
	 * Create a Bug from an OSB GPX XML wpt element.
	 * @param parser Parser up to a wpt element.
	 * @throws IOException If there was a problem parsing the XML.
	 * @throws NumberFormatException If there was a problem parsing the XML.
	 */
	public Photo(File f) throws IOException, NumberFormatException {
		// 
		ExifInterface exif = new ExifInterface(f.toString()); // create the ExifInterface file

		/** get the attribute. rest of the attributes are the same. i will add convertToDegree on the bottom (not required) **/
		String lonStr = exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE);
		if (lonStr == null) {
			throw new IOException("No EXIF tag");
		}
		float lonf = convertToDegree(lonStr);

		if(!exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF).equals("E"))	{ // deal with the negative degrees
			lonf = -lonf;	
		}
		float latf = convertToDegree(exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE));
		if(!exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF).equals("N"))	{
			latf = -latf;
		}
       
		lat = (int)(latf * 1E7d);
		lon = (int)(lonf * 1E7d);
		
	}
	
	public Photo(int lat, int lon, String ref) {
		this.lat = lat;
		this.lon = lon;
		this.ref = ref;
	}
	
	private Float convertToDegree(String stringDMS){
	   	Float result = null;
	   	String[] DMS = stringDMS.split(",", 3);
	
	   	String[] stringD = DMS[0].split("/", 2);
	   	Double D0 = new Double(stringD[0]);
	   	Double D1 = new Double(stringD[1]);
	   	Double FloatD = D0/D1;
	
	   	String[] stringM = DMS[1].split("/", 2);
	   	Double M0 = new Double(stringM[0]);
	   	Double M1 = new Double(stringM[1]);
	   	Double FloatM = M0/M1;
	
	   	String[] stringS = DMS[2].split("/", 2);
	   	Double S0 = new Double(stringS[0]);
	   	Double S1 = new Double(stringS[1]);
	   	Double FloatS = S0/S1;

   	    result = new Float(FloatD + (FloatM/60) + (FloatS/3600));

   	 return result;

   	};
	
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
		Log.d("Photo", "Uri " +  Uri.fromFile(new File(ref)));
		return Uri.fromFile(new File(ref));
		//return Uri.parse("content:/" + ref);
	}
	
}
