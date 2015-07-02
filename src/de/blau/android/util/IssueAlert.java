package de.blau.android.util;

import java.util.ArrayList;

import android.app.Activity;
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
	
	
	public static void alert(Context context, OsmElement e) {
	
		Preferences prefs = new Preferences(context);
		
		if (!prefs.generateAlerts()) { // don't generate alerts
			return;
		}
		
		LocationManager locationManager = (LocationManager) context.getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
		Location location = locationManager.getLastKnownLocation("gps");
		double eLon = 0D;
		double eLat = 0D;
		if (e.getName().equals("node")) {
			eLon = ((Node)e).getLon()/1E7D;
			eLat = ((Node)e).getLat()/1E7D;
		} else if (e.getName().equals("way")) {
			double[] result = Logic.centroidLonLat((Way)e);
			eLon = result[0];
			eLat = result[1];
		} else {
			return;
		}
		String title = context.getString(R.string.alert_data_issue);
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
			message = context.getString(R.string.alert_distance_direction, distance, bearings[index]) + "\n";
			ticker = ticker + " " + message;
		}
		message = message + e.describeProblem();
		NotificationCompat.Builder mBuilder =
		        new NotificationCompat.Builder(context)
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
			TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
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
					(NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
			// mId allows you to update the notification later on.
			mNotificationManager.notify((e.getName() + e.getOsmId()).hashCode(), mBuilder.build());
		} catch (OsmException e1) {
			Log.d("IssueAlert","Illegal BB created from lat " + eLat+ " lon " + eLon + " r " + prefs.getDownloadRadius());
		}
	}
	
	public static void alert(Context context, Bug b) {
		
		
		Preferences prefs = new Preferences(context);
		
		if (!prefs.generateAlerts()) { // don't generate alerts
			return;
		}
		
		LocationManager locationManager = (LocationManager) context.getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
		Location location = locationManager.getLastKnownLocation("gps");
		double eLon = b.getLon()/1E7D;
		double eLat = b.getLat()/1E7D;
		
		String title = context.getString(R.string.alert_note);
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
			message = context.getString(R.string.alert_distance_direction, distance, bearings[index]) + "\n";
			ticker = ticker + " " + message;
		}
		message = message + b.getDescription();
		NotificationCompat.Builder mBuilder =
		        new NotificationCompat.Builder(context)
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
		TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
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
			    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
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

		double ny = GeoMath.latToMercator(lat);
		double nx = lon;

		ArrayList<Node> nodes = new ArrayList<Node>(w.getNodes());
		for (int i = 0;i <= nodes.size()-2;i++) {
			double bx = nodes.get(i).getLon()/1E7D;
			double by = GeoMath.latE7ToMercator(nodes.get(i).getLat());
			double ax = nodes.get(i+1).getLon()/1E7D;
			double ay = GeoMath.latE7ToMercator(nodes.get(i+1).getLat());
			float[] newClosest = GeoMath.closestPoint((float)nx, (float)ny, (float)bx, (float)by, (float)ax, (float)ay);
			double newDistance = GeoMath.haversineDistance(nx, ny, newClosest[0], newClosest[1]);
			if (newDistance < closest.distance) {
				closest.distance = newDistance;
				closest.lon = newClosest[0];
				closest.lat = GeoMath.mercatorToLat(newClosest[1]);
			}
		}
		return closest;
	}
}
