package de.blau.android.util;

import java.util.ArrayList;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;
import de.blau.android.Application;
import de.blau.android.Logic;
import de.blau.android.Main;
import de.blau.android.Map;
import de.blau.android.R;
import de.blau.android.exception.OsmException;
import de.blau.android.osb.Bug;
import de.blau.android.osm.BoundingBox;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Node;
import de.blau.android.osm.Way;
import de.blau.android.prefs.Preferences;

public class IssueAlert {
	
	
	public static void alert(OsmElement e) {
	
		Main main = Application.mainActivity;
		Preferences prefs = new Preferences(main);
		
		if (!prefs.generateAlerts()) { // don't generate alerts
			return;
		}
		
		LocationManager locationManager = (LocationManager) main.getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
		Location location = locationManager.getLastKnownLocation("gps");
		double eLon = 0D;
		double eLat = 0D;
		if (e.getName().equals("node")) {
			eLon = ((Node)e).getLon()/1E7D;
			eLat = ((Node)e).getLat()/1E7D;
		} else if (e.getName().equals("way")) {
			int result[] = Logic.centroid(main.getMap().getWidth(), main.getMap().getHeight(), main.getMap().getViewBox(),(Way)e);
			eLon = result[1]/1E7D;
			eLat = result[0]/1E7D;
		}
		String title = main.getString(R.string.alert_data_issue);
		String ticker = title;
		String message = "";
		if (location != null) {
			// if we know where we are we can provide better information
			long distance = 0;
			if (e.getName().equals("node")) {
				distance = Math.round(GeoMath.haversineDistance(location.getLongitude(), location.getLatitude(), eLon, eLat));
			} else if (e.getName().equals("way")) {
				ClosestPoint cp = getClosestDistance(location.getLongitude(), location.getLatitude(), (Way)e);
				distance = Math.round(cp.distance);
				eLon = cp.lon;
				eLat = cp.lat;
			}
			long bearing = GeoMath.bearing(location.getLongitude(), location.getLatitude(), eLon, eLat);
			String[] bearings = {"NE", "E", "SE", "S", "SW", "W", "NW", "N"};

			int index = (int)(bearing - 22.5);
			if (index < 0)
			    index += 360;
			index = index / 45;

			// message = "in " + distance + "m " /* + bearing + "° " */ + bearings[index] + "\n";
			message = main.getString(R.string.alert_distance_direction, distance, bearings[index]) + "\n";
			ticker = ticker + " " + message;
		}
		message = message + e.describeProblem();
		NotificationCompat.Builder mBuilder =
		        new NotificationCompat.Builder(main)
		        .setSmallIcon(R.drawable.osm_logo)
		        .setContentTitle(title)
		        .setContentText(message)
		        .setPriority(NotificationCompat.PRIORITY_HIGH)
		        .setTicker(ticker);
		// Creates an explicit intent for an Activity in your app
		// Intent resultIntent = new Intent(main, Main.class);
		Intent resultIntent = new Intent(Intent.ACTION_VIEW);
		// Uri geo = Uri.fromParts("geo", eLat+","+eLon,null);
	    // resultIntent.setData(geo);
		try {
			BoundingBox box = GeoMath.createBoundingBoxForCoordinates(eLat, eLon, prefs.getDownloadRadius());

			Uri rc = Uri.parse( "http://127.0.0.1:8111/load_and_zoom?left=" + box.getLeft()/1E7D + "&right=" + box.getRight()/1E7D + "&top=" + box.getTop()/1E7D + "&bottom=" + box.getBottom()/1E7D + "&select="+e.getName()+e.getOsmId());

			Log.d("IssueAlert", rc.toString());
			resultIntent.setData(rc);
			TaskStackBuilder stackBuilder = TaskStackBuilder.create(main);
			// Adds the back stack for the Intent (but not the Intent itself)
			stackBuilder.addParentStack(Main.class);
			// Adds the Intent that starts the Activity to the top of the stack
			stackBuilder.addNextIntent(resultIntent);
			PendingIntent resultPendingIntent =
					stackBuilder.getPendingIntent(
							0,
							PendingIntent.FLAG_UPDATE_CURRENT
							);
			mBuilder.setContentIntent(resultPendingIntent);

			NotificationManager mNotificationManager =
					(NotificationManager) main.getSystemService(Context.NOTIFICATION_SERVICE);
			// mId allows you to update the notification later on.
			mNotificationManager.notify((e.getName() + e.getOsmId()).hashCode(), mBuilder.build());
		} catch (OsmException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
	
	public static void alert(Bug b) {
		
		Main main = Application.mainActivity;
		
		Preferences prefs = new Preferences(main);
		
		if (!prefs.generateAlerts()) { // don't generate alerts
			return;
		}
		
		LocationManager locationManager = (LocationManager) main.getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
		Location location = locationManager.getLastKnownLocation("gps");
		double eLon = b.getLon()/1E7D;
		double eLat = b.getLat()/1E7D;
		
		String title = main.getString(R.string.alert_note);
		String ticker = title;
		String message = "";
		if (location != null) {
			// if we know where we are we can provide better information
			long distance = Math.round(GeoMath.haversineDistance(location.getLongitude(), location.getLatitude(), eLon, eLat));
			
			// filter
			if (distance > 100) {
				return;
			}
			
			long bearing = GeoMath.bearing(location.getLongitude(), location.getLatitude(), eLon, eLat);
			String[] bearings = {"NE", "E", "SE", "S", "SW", "W", "NW", "N"};

			int index = (int)(bearing - 22.5);
			if (index < 0)
			    index += 360;
			index = index / 45;

			// message = "in " + distance + "m " /* + bearing + "° " */ + bearings[index] + "\n";
			message = main.getString(R.string.alert_distance_direction, distance, bearings[index]) + "\n";
			ticker = ticker + " " + message;
		}
		message = message + b.getDescription();
		NotificationCompat.Builder mBuilder =
		        new NotificationCompat.Builder(main)
		        .setSmallIcon(R.drawable.osm_logo)
		        .setContentTitle(title)
		        .setContentText(message)
		        .setPriority(NotificationCompat.PRIORITY_HIGH)
		        .setTicker(ticker);
		// Creates an explicit intent for an Activity in your app
		// Intent resultIntent = new Intent(main, Main.class);
		Intent resultIntent = new Intent(Intent.ACTION_VIEW);
		Uri geo = Uri.fromParts("geo", eLat+","+eLon,null);
	    resultIntent.setData(geo);
		TaskStackBuilder stackBuilder = TaskStackBuilder.create(main);
		// Adds the back stack for the Intent (but not the Intent itself)
		stackBuilder.addParentStack(Main.class);
		// Adds the Intent that starts the Activity to the top of the stack
		stackBuilder.addNextIntent(resultIntent);
		PendingIntent resultPendingIntent =
		        stackBuilder.getPendingIntent(
		            0,
		            PendingIntent.FLAG_UPDATE_CURRENT
		        );
		mBuilder.setContentIntent(resultPendingIntent);

		NotificationManager mNotificationManager =
			    (NotificationManager) main.getSystemService(Context.NOTIFICATION_SERVICE);
			// mId allows you to update the notification later on.
			mNotificationManager.notify(("bug" + b.getId()).hashCode(), mBuilder.build());
	}
	
	static class ClosestPoint{
		double distance = Double.MAX_VALUE;
		double lat;
		double lon;
	}
	
	static ClosestPoint getClosestDistance(double lon, double lat,  Way w) {
		ClosestPoint closest = new IssueAlert.ClosestPoint();
		Main main = Application.mainActivity;
		Map map = main.getMap();

		// to avoid rounding errors we translate the bb to 0,0
		BoundingBox bb = main.getMap().getViewBox();
		double latOffset = GeoMath.latE7ToMercatorE7(bb.getBottom());
		double lonOffset = bb.getLeft();
		double ny = GeoMath.latToMercator(lat)-latOffset/1E7D;
		double nx = lon - lonOffset/1E7D;

		ArrayList<Node> nodes = new ArrayList<Node>(w.getNodes());
		for (int i = 0;i <= nodes.size()-2;i++) {
			double bx = (nodes.get(i).getLon()-lonOffset)/1E7D;
			double by = (GeoMath.latE7ToMercatorE7(nodes.get(i).getLat())-latOffset )/1E7D;
			double ax = (nodes.get(i+1).getLon()-lonOffset)/1E7D;
			double ay = (GeoMath.latE7ToMercatorE7(nodes.get(i+1).getLat())-latOffset)/1E7D;
			float[] newClosest = GeoMath.closestPoint((float)nx, (float)ny, (float)bx, (float)by, (float)ax, (float)ay);
			double newDistance = GeoMath.haversineDistance(nx, ny, newClosest[0], newClosest[1]);
			if (newDistance < closest.distance) {
				closest.distance = newDistance;
				closest.lon = GeoMath.xToLonE7(map.getWidth(), map.getViewBox(), newClosest[0]);
				closest.lat = GeoMath.yToLatE7(map.getHeight(), map.getWidth(), map.getViewBox(), newClosest[1]);
				
			}
		}
		return closest;
	}
}
