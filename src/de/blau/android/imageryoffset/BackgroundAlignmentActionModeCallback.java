package de.blau.android.imageryoffset;



import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.os.AsyncTask;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.ActionMode.Callback;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import de.blau.android.Application;
import de.blau.android.DialogFactory;
import de.blau.android.HelpViewer;
import de.blau.android.Logic.Mode;
import de.blau.android.Map;
import de.blau.android.R;
import de.blau.android.osm.BoundingBox;
import de.blau.android.osm.Server;
import de.blau.android.prefs.Preferences;
import de.blau.android.util.GeoMath;
import de.blau.android.util.Offset;
import de.blau.android.util.ThemeUtils;
import de.blau.android.util.jsonreader.JsonReader;
import de.blau.android.util.jsonreader.JsonToken;
import de.blau.android.views.util.OpenStreetMapTileServer;

public class BackgroundAlignmentActionModeCallback implements Callback {
	
	private static final int MENUITEM_QUERYDB = 1;
	private static final int MENUITEM_QUERYLOCAL = 2;
	private static final int MENUITEM_APPLY2ALL = 3;
	private static final int MENUITEM_RESET = 4;
	private static final int MENUITEM_ZERO = 5;
	private static final int MENUITEM_SAVE2DB = 6;
	private static final int MENUITEM_SAVELOCAL = 7;
	private static final int MENUITEM_HELP = 8;
	
	Mode oldMode;
	private final Preferences prefs;
	private final String offsetServer;
	
	Offset[] oldOffsets;
	
	OpenStreetMapTileServer osmts;
	Map map;
	
	ArrayList<ImageryOffset> offsetList;
	
	public BackgroundAlignmentActionModeCallback(Mode oldMode) {
		this.oldMode = oldMode;
		map = Application.mainActivity.getMap();
		osmts = map.getOpenStreetMapTilesOverlay().getRendererInfo();
		oldOffsets = osmts.getOffsets().clone();
		prefs = new Preferences(Application.mainActivity);
		offsetServer = prefs.getOffsetServer();
	}

	@Override
	public boolean onCreateActionMode(ActionMode mode, Menu menu) {
		mode.setTitle(R.string.menu_tools_background_align);
		return true;
	}

	@Override
	public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
		menu.clear();
		menu.add(Menu.NONE, MENUITEM_QUERYDB, Menu.NONE, R.string.menu_tools_background_align_retrieve_from_db);
		// menu.add(Menu.NONE, MENUITEM_QUERYLOCAL, Menu.NONE, R.string.menu_tools_background_align_retrieve_from_device);
		menu.add(Menu.NONE, MENUITEM_RESET, Menu.NONE, R.string.menu_tools_background_align_reset);
		menu.add(Menu.NONE, MENUITEM_ZERO, Menu.NONE, R.string.menu_tools_background_align_zero);
		menu.add(Menu.NONE, MENUITEM_APPLY2ALL, Menu.NONE, R.string.menu_tools_background_align_apply2all);
		menu.add(Menu.NONE, MENUITEM_SAVE2DB, Menu.NONE, R.string.menu_tools_background_align_save_db);
		// menu.add(Menu.NONE, MENUITEM_SAVELOCAL, Menu.NONE, R.string.menu_tools_background_align_save_device);
		menu.add(Menu.NONE, MENUITEM_HELP, Menu.NONE, R.string.menu_help);
		return true;
	}

	@Override
	public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
		switch (item.getItemId()) {
		case MENUITEM_ZERO: 
			osmts.setOffset(map.getZoomLevel(),0.0d,0.0d);
			map.invalidate();
			break;
		case MENUITEM_RESET: 
			osmts.setOffsets(oldOffsets);
			map.invalidate();
			break;
		case MENUITEM_APPLY2ALL: 
			Offset o = osmts.getOffset(map.getZoomLevel());
			if (o != null)
				osmts.setOffset(o.lon,o.lat);
			else
				osmts.setOffset(0.0d,0.0d);
			break;
		case MENUITEM_QUERYDB: 
			getOffsetFromDB();
			break;
		case MENUITEM_QUERYLOCAL:
			break;
		case MENUITEM_SAVE2DB:
			saveOffsetsToDB();
			break;
		case MENUITEM_SAVELOCAL:
			break;
		case MENUITEM_HELP:
			Intent startHelpViewer = new Intent(Application.mainActivity, HelpViewer.class);
			startHelpViewer.putExtra(HelpViewer.TOPIC, mode.getTitle().toString());
			Application.mainActivity.startActivity(startHelpViewer);
			return true;
		default: return false;
		}
		return true;
	}
	
	/**
	 * Download offsets 
	 * @author simon
	 *
	 */
	private class OffsetLoader extends AsyncTask<Integer, Void, ArrayList<ImageryOffset>> {
		
		String error = null;

		@Override
		protected void onPreExecute() {
			Application.mainActivity.showDialog(DialogFactory.PROGRESS_SEARCHING);
		}
		
		@Override
		protected ArrayList<ImageryOffset> doInBackground(Integer... params) {
	    	
			BoundingBox bbox = Application.mainActivity.getMap().getViewBox();
			double centerLon = (bbox.getLeft() + ((long)bbox.getRight() - (long)bbox.getLeft())/2L) / 1E7d;
			try {
				Integer radius = params[0];
				String urlString = offsetServer + "get?lat=" + bbox.getCenterLat() + "&lon=" + centerLon 
						+ (radius != null && radius > 0 ? "&radius=" + radius : "") + "&imagery=" + osmts.getImageryOffsetId() + "&format=json";
				Log.d("BackgroundAlignmentActionModeCallback","urlString " + urlString);
				URL url = new URL(urlString);
				HttpURLConnection conn = (HttpURLConnection) url.openConnection();
				conn.setRequestProperty("User-Agent", Application.userAgent);
				JsonReader reader = new JsonReader(new InputStreamReader(conn.getInputStream()));
				ArrayList<ImageryOffset> result = new ArrayList<ImageryOffset>();
				try {
					
					try {
						JsonToken token = reader.peek();
						if (token.equals(JsonToken.BEGIN_ARRAY)) {
							reader.beginArray();
							while (reader.hasNext()) {
								ImageryOffset imOffset = readOffset(reader);
								if (imOffset != null && imOffset.deprecated == null) //TODO handle deprecated 
									result.add(imOffset);
							}
							reader.endArray();
						} else if (token.equals(JsonToken.BEGIN_OBJECT)) {
							reader.beginObject();
							while (reader.hasNext()) {
								String jsonName = reader.nextName();
								if (jsonName.equals("error")) {
									error = reader.nextString();
								} else {
									reader.skipValue();
								}
							}
							return null;
						} // can't happen ?
					} catch (IOException e) {
						error = e.getMessage();
					} catch (IllegalStateException e) {
						error = e.getMessage();
					}
					return result;
				}
				finally {
				       reader.close();
				}			
			} catch (MalformedURLException e) {
				error = e.getMessage();
			} catch (IOException e) {
				error = e.getMessage();
			}			
			return null;
		}
		
		@Override
		protected void onPostExecute(ArrayList<ImageryOffset> res) {
			try {
				Application.mainActivity.dismissDialog(DialogFactory.PROGRESS_SEARCHING);
			} catch (IllegalArgumentException e) {
				 // Avoid crash if dialog is already dismissed
				Log.d("BackgroundAlignmentActionModeCallback", "", e);
			}
		}
		
		String getError() {
			return error;
		}
	};
	
	/**
	 * Save offsets 
	 * @author simon
	 *
	 */
	private class OffsetSaver extends AsyncTask<ImageryOffset, Void, Integer> {
		String error = null;

		@Override
		protected void onPreExecute() {
			Application.mainActivity.showDialog(DialogFactory.PROGRESS_SAVING);
		}
		
		@Override
		protected Integer doInBackground(ImageryOffset... params) {
	    	
			try {
				ImageryOffset offset = params[0];
				String urlString = offset.toSaveUrl();
				Log.d("BackgroundAlignmentActionModeCallback","urlString " + urlString);
				URL url = new URL(urlString);
				HttpURLConnection conn = (HttpURLConnection) url.openConnection();
				conn.setRequestMethod("POST");
				conn.setRequestProperty("User-Agent", Application.userAgent);
				InputStream is = conn.getInputStream();
				return conn.getResponseCode();
			} catch (MalformedURLException e) {
				error = e.getMessage();
				return -2;
			} catch (Exception /*IOException*/ e) {
				error = e.getMessage();
				return -1;
			} 
		}
		
		@Override
		protected void onPostExecute(Integer res) {
			try {
				Application.mainActivity.dismissDialog(DialogFactory.PROGRESS_SAVING);
			} catch (IllegalArgumentException e) {
				 // Avoid crash if dialog is already dismissed
				Log.d("BackgroundAlignmentActionModeCallback", "", e);
			}
			if (res == 200)
				Toast.makeText(Application.mainActivity.getApplicationContext(), R.string.toast_save_done, Toast.LENGTH_SHORT).show();
			else
				Toast.makeText(Application.mainActivity.getApplicationContext(), R.string.toast_save_failed, Toast.LENGTH_SHORT).show();
		}
		
		String getError() {
			return error;
		}
	};

	private void getOffsetFromDB() {
		OffsetLoader loader = new OffsetLoader(); 
		String error = null;
		
		try {
			// first try for our view box
			final BoundingBox bbox = map.getViewBox();
			final double centerLat = bbox.getCenterLat();
			final double centerLon = (bbox.getLeft() + bbox.getWidth()/2)/1E7d;
			Comparator<ImageryOffset> cmp = new Comparator<ImageryOffset>() {
		        @Override
		        public int compare(ImageryOffset  offset1, ImageryOffset  offset2)
		        {
		        	double d1 = GeoMath.haversineDistance(centerLon, centerLat, offset1.lon, offset1.lat);
		        	double d2 = GeoMath.haversineDistance(centerLon, centerLat, offset2.lon, offset2.lat);
		            return  Double.valueOf(d1).compareTo(Double.valueOf(d2));
		        }
		    };
			double hm = GeoMath.haversineDistance(centerLon, bbox.getBottom()/1E7d, centerLon, bbox.getTop()/1E7d);
			double wm = GeoMath.haversineDistance(bbox.getLeft()/1E7d, centerLat, bbox.getRight()/1E7d, centerLat);
			int radius = (int)Math.min(1, Math.round(Math.min(hm,wm)/2000d)); // convert to km and make it at least 1 and /2 for radius
			loader.execute(Integer.valueOf(radius));
			offsetList = loader.get(10, TimeUnit.SECONDS);
			if (offsetList != null && offsetList.size() > 0) {
				Collections.sort(offsetList, cmp);
				Dialog d = createDisplayOffsetDialog(0);
				d.show();
			} else {
				loader.cancel(true);
				loader = new OffsetLoader();
				loader.execute(Integer.valueOf(0));
				offsetList = loader.get(10, TimeUnit.SECONDS);
				if (offsetList != null && offsetList.size() > 0) {
					Collections.sort(offsetList, cmp);
					Dialog d = createDisplayOffsetDialog(0);
					d.show();
				} else {
					AlertDialog.Builder builder = new AlertDialog.Builder(Application.mainActivity);
					builder.setMessage(R.string.imagery_offset_not_found).setTitle(R.string.imagery_offset_title);
					builder.setPositiveButton(R.string.okay, null);
					AlertDialog dialog = builder.create();
					dialog.show();
				}
			}
			return;
		} catch (InterruptedException e) {
			error = e.getMessage();
		} catch (ExecutionException e) {
			error = e.getMessage();
		} catch (TimeoutException e) {
			error = Application.mainActivity.getString(R.string.toast_timeout);
		}
		displayError(error);
	}
	
	private void saveOffsetsToDB() {
		
		Offset[] offsets = osmts.getOffsets(); // current offset
		ArrayList<ImageryOffset> offsetList = new ArrayList<ImageryOffset>();
		final BoundingBox bbox = map.getViewBox();
		Offset lastOffset = null;
		ImageryOffset im = null;
		String author = null;
		String error = null;
		
		// try to find current display name
		final Server server = prefs.getServer();
		if (server != null) {
			if (!server.needOAuthHandshake()) {
				try {
					AsyncTask<Void,Void,Server.UserDetails> loader = new AsyncTask<Void,Void,Server.UserDetails>() {
						
						@Override
						protected Server.UserDetails doInBackground(Void... params) {
							return server.getUserDetails();
						}
					};
					loader.execute();			
					Server.UserDetails user = loader.get(10, TimeUnit.SECONDS);
						
					if (user != null) {
						author = user.display_name;
					} else {
						author = server.getDisplayName(); // maybe it has been configured
					}
				} catch (InterruptedException e) {
					error = e.getMessage();
				} catch (ExecutionException e) {
					error = e.getMessage();
				} catch (TimeoutException e) {
					error = Application.mainActivity.getString(R.string.toast_timeout);
				}
				displayError(error);
				error=null;
			} else {
				author = server.getDisplayName(); // maybe it has been configured
			}	
		}
		
		for (int z = 0; z < offsets.length; z++)  { // iterate through the list and generate a new offset when necessary
			Offset o = offsets[z];
			if (o != null && (o.lon != 0 || o.lat !=0)) { // non-null zoom
				if (lastOffset != null && (o.lon != 0 || o.lat !=0)) {
					if (lastOffset.lon == o.lon && lastOffset.lat == o.lat) {
						im.maxZoom++;
						lastOffset = o;
						continue;
					}
				}
				im = new ImageryOffset();
				im.lon = (bbox.getLeft() + bbox.getWidth()/2)/1E7d;
				im.lat = bbox.getCenterLat();
				im.imageryLon = im.lon - o.lon;
				im.imageryLat = im.lat - o.lat;
				im.minZoom = z + osmts.getMinZoomLevel();
				im.maxZoom = im.minZoom;
				Calendar c = Calendar.getInstance();
				SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd",Locale.US);
				im.date = simpleDateFormat.format(c.getTime());
				im.author = author;
				offsetList.add(im);
			}
			lastOffset = o;
		}
		
		if (offsetList != null && offsetList.size() > 0) {		
			Dialog d = createSaveOffsetDialog(0, offsetList);
			d.show();
		} 
	}

	private void displayError(String error) {
		if (error != null) { // try to avoid code dup
			AlertDialog.Builder builder = new AlertDialog.Builder(Application.mainActivity);
			builder.setMessage(error).setTitle(R.string.imagery_offset_title);
			builder.setPositiveButton(R.string.okay, null);
			AlertDialog dialog = builder.create();
			dialog.show();
		}
	}

	protected ImageryOffset readOffset(JsonReader reader) throws IOException {
		String type = null;
		ImageryOffset result = new ImageryOffset();

		reader.beginObject();
		while (reader.hasNext()) {
			String jsonName = reader.nextName();
			if (jsonName.equals("type")) {
				type = reader.nextString();
			} else if (jsonName.equals("id")) {
				result.id = reader.nextLong();
			} else if (jsonName.equals("lat")) {
				result.lat = reader.nextDouble();
			} else if (jsonName.equals("lon")) {
				result.lon = reader.nextDouble();
			} else if (jsonName.equals("author")) {
				result.author = reader.nextString();
			} else if (jsonName.equals("date")) {
				result.date = reader.nextString();
			} else if (jsonName.equals("imagery")) {
				result.imageryId = reader.nextString();
			} else if (jsonName.equals("imlat")) {
				result.imageryLat = reader.nextDouble();
			} else if (jsonName.equals("imlon")) {
				result.imageryLon = reader.nextDouble();
			} else if (jsonName.equals("min-zoom")) {
				result.minZoom = reader.nextInt();
			} else if (jsonName.equals("max-zoom")) {
				result.maxZoom = reader.nextInt();
			} else if (jsonName.equals("description")) {
				result.description = reader.nextString();
			} else if (jsonName.equals("deprecated")) {
				result.deprecated = readDeprecated(reader);
			}else {
				reader.skipValue();
			}
		}
		reader.endObject();
		if (type.equals("offset"))
			return result;

		return null;
	}
	
	protected DeprecationNote readDeprecated(JsonReader reader) throws IOException {
		DeprecationNote result = new DeprecationNote();

		reader.beginObject();
		while (reader.hasNext()) {
			String jsonName = reader.nextName();
			if (jsonName.equals("author")) {
				result.author = reader.nextString();
			} else if (jsonName.equals("reason")) {
				result.reason = reader.nextString();
			}else if (jsonName.equals("date")) {
				result.date = reader.nextString();
			} else{
				reader.skipValue();
			}
		}
		reader.endObject();
		return result;
	}
	
	/**
	 * Object to hold the output from the imagery DB see https://wiki.openstreetmap.org/wiki/Imagery_Offset_Database/API
	 * @author simon
	 *
	 */
	private class ImageryOffset {
	    long id;
	    double lat = 0;
	    double lon = 0;
	    String author;
	    String description;
	    String date;
	    String imageryId;
	    int minZoom = 0;
	    int maxZoom = 18;
	    double imageryLat = 0;
	    double imageryLon = 0;
	    DeprecationNote deprecated = null;
	    
		public String toSaveUrl() {
			try {
				return offsetServer+"store?lat="+ URLEncoder.encode(String.format("%.7f",lat),"UTF-8")+"&lon="+URLEncoder.encode(String.format("%.7f",lon),"UTF-8")
						+"&author="+URLEncoder.encode(author,"UTF-8")
						+"&description="+URLEncoder.encode(description,"UTF-8")
						+"&imagery="+URLEncoder.encode(imageryId,"UTF-8")
						+"&imlat="+URLEncoder.encode(String.format("%.7f",imageryLat),"UTF-8")+"&imlon="+URLEncoder.encode(String.format("%.7f",imageryLon),"UTF-8");
			} catch (UnsupportedEncodingException e) {
				return null;
			}
		}
	}
	
	/**
	 * Object to hold the output from the imagery DB see https://wiki.openstreetmap.org/wiki/Imagery_Offset_Database/API
	 * Currently we don't actually display the contents anywhere
	 * @author simon
	 *
	 */
	private class DeprecationNote {
		String author;
		String date;
		String reason;
	}
	
	/**
	 * Create the imagery offset display/apply dialog ... given that it has so much logic, done here instead of DialogFactory
	 * @param index
	 * @return
	 */
	private Dialog createSaveOffsetDialog(final int index, final ArrayList<ImageryOffset> saveOffsetList) {
		// Create some useful objects
		final BoundingBox bbox = map.getViewBox();
		final LayoutInflater inflater = ThemeUtils.getLayoutInflater(Application.mainActivity);
		Builder dialog = new AlertDialog.Builder(Application.mainActivity);
		dialog.setTitle(R.string.imagery_offset_title);
		final ImageryOffset offset = saveOffsetList.get(index);
		View layout = inflater.inflate(R.layout.save_imagery_offset, null);
		dialog.setView(layout);
		EditText description = (EditText) layout.findViewById(R.id.imagery_offset_description);
		EditText author = (EditText) layout.findViewById(R.id.imagery_offset_author);
		if (offset.author != null) {
			author.setText(offset.author);
		}
		TextView off = (TextView) layout.findViewById(R.id.imagery_offset_offset);
		off.setText(String.format("%.2f",GeoMath.haversineDistance(offset.lon,offset.lat,offset.imageryLon,offset.imageryLat))+" meters");
		if (offset.date != null) {
			TextView created = (TextView) layout.findViewById(R.id.imagery_offset_date);
			created.setText(offset.date);
		}
		TextView minmax = (TextView) layout.findViewById(R.id.imagery_offset_zoom);
		minmax.setText(offset.minZoom + "-" + offset.maxZoom);
		dialog.setPositiveButton(R.string.menu_tools_background_align_save_db, createSaveButtonListener(description, author, index, saveOffsetList));
		if (index == (saveOffsetList.size() - 1))
			dialog.setNegativeButton(R.string.cancel, null);
		else
			dialog.setNegativeButton(R.string.next, new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					Dialog d = createSaveOffsetDialog(index+1,saveOffsetList);
					d.show();
				}
			});
		return dialog.create();
	}
	
	/**
	 * Create an onClick listener that saves the current offset to the offset DB and (if it exists) displays the next offset to be saved
	 * @param description
	 * @param author
	 * @param offset
	 * @param index
	 * @return the OnClickListnener
	 */
	OnClickListener createSaveButtonListener(final EditText description, final EditText author, final int index, final ArrayList <ImageryOffset> saveOffsetList) {
		
		return new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				String error = null;
				ImageryOffset offset = saveOffsetList.get(index);
				if (offset == null)
					return;
				offset.description = description.getText().toString();
				offset.author = author.getText().toString();
				offset.imageryId = osmts.getImageryOffsetId();
				Log.d("Background...",offset.toSaveUrl());
				OffsetSaver saver = new OffsetSaver();
				saver.execute(offset);
				try {
					int result = saver.get();
					if (result < 0) {
						error = saver.getError();
					}
				} catch (InterruptedException e) {
					error = e.getMessage();
				} catch (ExecutionException e) {
					error = e.getMessage();
				}
				if (error != null) {
					displayError(error);
					return; // don't continue is something went wrong 
				}
				
				if (index < (saveOffsetList.size()-1)) {
					// save retyping if it stays the same
					saveOffsetList.get(index+1).description = offset.description;
					saveOffsetList.get(index+1).author = offset.author;
					Dialog d = createSaveOffsetDialog(index+1,saveOffsetList);
					d.show();
				}
			}
		};
	}

	private Dialog createDisplayOffsetDialog(final int index) {
		// Create some useful objects
		final BoundingBox bbox = map.getViewBox();
		final LayoutInflater inflater = ThemeUtils.getLayoutInflater(Application.mainActivity);
		
		Builder dialog = new AlertDialog.Builder(Application.mainActivity);
		dialog.setTitle(R.string.imagery_offset_title);
		final ImageryOffset offset = offsetList.get(index);
		View layout = inflater.inflate(R.layout.imagery_offset, null);
		dialog.setView(layout);
		if (offset.description != null) {
			TextView description = (TextView) layout.findViewById(R.id.imagery_offset_description);
			description.setText(offset.description);
		}
		if (offset.author != null) {
			TextView author = (TextView) layout.findViewById(R.id.imagery_offset_author);
			author.setText(offset.author);
		}
		TextView off = (TextView) layout.findViewById(R.id.imagery_offset_offset);
		off.setText(String.format("%.2f",GeoMath.haversineDistance(offset.lon,offset.lat,offset.imageryLon,offset.imageryLat))+" meters");
		if (offset.date != null) {
			TextView created = (TextView) layout.findViewById(R.id.imagery_offset_date);
			created.setText(offset.date);
		}
		TextView minmax = (TextView) layout.findViewById(R.id.imagery_offset_zoom);
		minmax.setText(offset.minZoom + "-" + offset.maxZoom);
		TextView distance = (TextView) layout.findViewById(R.id.imagery_offset_distance);
		distance.setText(String.format("%.3f",GeoMath.haversineDistance((bbox.getLeft() + bbox.getWidth()/2)/1E7d, bbox.getCenterLat(), offset.lon, offset.lat)/1000) +" km");
		dialog.setPositiveButton(R.string.apply, new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				osmts.setOffset(map.getZoomLevel(),offset.lon-offset.imageryLon, offset.lat-offset.imageryLat);
				map.invalidate();
			}
		});
		dialog.setNeutralButton(R.string.menu_tools_background_align_apply2all,new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				osmts.setOffset(offset.minZoom,offset.maxZoom,offset.lon-offset.imageryLon, offset.lat-offset.imageryLat);
				map.invalidate();
			}
		});
		if (index == (offsetList.size() - 1))
			dialog.setNegativeButton(R.string.cancel, null);
		else
			dialog.setNegativeButton(R.string.next, new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					Dialog d = createDisplayOffsetDialog(index+1);
					d.show();
				}
			});
		return dialog.create();
	}
	
	@Override
	public void onDestroyActionMode(ActionMode mode) {
		Application.mainActivity.setMode(oldMode);
	}

}
