package de.blau.android.imageryoffset;


import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AlertDialog.Builder;
import android.support.v7.app.AppCompatDialog;
import android.support.v7.view.ActionMode;
import android.support.v7.view.ActionMode.Callback;
import android.support.v7.widget.ActionMenuView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewStub;
import android.widget.EditText;
import android.widget.TextView;
import de.blau.android.App;
import de.blau.android.HelpViewer;
import de.blau.android.Main;
import de.blau.android.Map;
import de.blau.android.Mode;
import de.blau.android.PostAsyncActionHandler;
import de.blau.android.R;
import de.blau.android.dialogs.Progress;
import de.blau.android.osm.BoundingBox;
import de.blau.android.osm.Server;
import de.blau.android.prefs.Preferences;
import de.blau.android.resources.TileLayerServer;
import de.blau.android.util.DateFormatter;
import de.blau.android.util.GeoMath;
import de.blau.android.util.MenuUtil;
import de.blau.android.util.NetworkStatus;
import de.blau.android.util.Offset;
import de.blau.android.util.Snack;
import de.blau.android.util.ThemeUtils;

public class BackgroundAlignmentActionModeCallback implements Callback {
	
	private final static String DEBUG_TAG = "BackgroundAlign...";
	
	private static final int MENUITEM_QUERYDB = 1;
	private static final int MENUITEM_QUERYLOCAL = 2;
	private static final int MENUITEM_APPLY2ALL = 3;
	private static final int MENUITEM_RESET = 4;
	private static final int MENUITEM_ZERO = 5;
	private static final int MENUITEM_SAVE2DB = 6;
	private static final int MENUITEM_SAVELOCAL = 7;
	private static final int MENUITEM_HELP = 8;
	
	private Mode oldMode;
	private final Preferences prefs;
	private final Uri offsetServerUri;
	
	private Offset[] oldOffsets;
	
	private TileLayerServer osmts;
	private final Map map;
	private final Main main;
	
	private ArrayList<ImageryOffset> offsetList;
	
	private ActionMenuView cabBottomBar;
	
	public BackgroundAlignmentActionModeCallback(Main main, Mode oldMode) {
		this.oldMode = oldMode;
		this.main = main; // currently we are only called from here
		map = main.getMap();
		osmts = map.getOpenStreetMapTilesOverlay().getRendererInfo();
		oldOffsets = osmts.getOffsets().clone();
		prefs = new Preferences(main);
		String offsetServer = prefs.getOffsetServer();
		offsetServerUri = Uri.parse(offsetServer);
	}

	@Override
	public boolean onCreateActionMode(ActionMode mode, Menu menu) {
		FloatingActionButton lock = main.getLock();
		if (lock != null) {
			lock.hide();
		}
		mode.setTitle(R.string.menu_tools_background_align);
		if (main.getBottomBar() != null) {
			main.hideBottomBar();
			View v = main.findViewById(R.id.cab_stub);
			if (v instanceof ViewStub) { // only need to inflate once
				ViewStub stub = (ViewStub) v;
				stub.setLayoutResource(R.layout.toolbar);
				stub.setInflatedId(R.id.cab_stub);
				cabBottomBar = (ActionMenuView) stub.inflate();
			} else if (v instanceof ActionMenuView) {
				cabBottomBar = (ActionMenuView) v;
				cabBottomBar.setVisibility(View.VISIBLE);
				cabBottomBar.getMenu().clear();
			}
		}
		return true;
	}

	@Override
	public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
		if (cabBottomBar!=null) {
			menu = cabBottomBar.getMenu();
			final ActionMode actionMode = mode;
			android.support.v7.widget.ActionMenuView.OnMenuItemClickListener listener = new android.support.v7.widget.ActionMenuView.OnMenuItemClickListener() {
				@Override
				public boolean onMenuItemClick(MenuItem item) {
					return onActionItemClicked(actionMode,item);
				}	
			};
			cabBottomBar.setOnMenuItemClickListener(listener);
			MenuUtil.setupBottomBar(main, cabBottomBar, main.isFullScreen(), prefs.lightThemeEnabled());
		}
		menu.clear();
		MenuItem mi = menu.add(Menu.NONE, MENUITEM_QUERYDB, Menu.NONE, R.string.menu_tools_background_align_retrieve_from_db).setEnabled(NetworkStatus.isConnected(main))
			.setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_download));
		MenuItemCompat.setShowAsAction(mi,MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);
		// menu.add(Menu.NONE, MENUITEM_QUERYLOCAL, Menu.NONE, R.string.menu_tools_background_align_retrieve_from_device);
		mi = menu.add(Menu.NONE, MENUITEM_RESET, Menu.NONE, R.string.menu_tools_background_align_reset)
			.setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_undo));
		MenuItemCompat.setShowAsAction(mi,MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);
		menu.add(Menu.NONE, MENUITEM_ZERO, Menu.NONE, R.string.menu_tools_background_align_zero);
		menu.add(Menu.NONE, MENUITEM_APPLY2ALL, Menu.NONE, R.string.menu_tools_background_align_apply2all);
		menu.add(Menu.NONE, MENUITEM_SAVE2DB, Menu.NONE, R.string.menu_tools_background_align_save_db).setEnabled(NetworkStatus.isConnected(main));
		// menu.add(Menu.NONE, MENUITEM_SAVELOCAL, Menu.NONE, R.string.menu_tools_background_align_save_device);
		menu.add(Menu.NONE, MENUITEM_HELP, Menu.NONE, R.string.menu_help);
//		Toolbar toolbar = (Toolbar) Application.mainActivity.findViewById(R.id.mainToolbar);
//		toolbar.setVisibility(View.GONE);;
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
			osmts.setOffsets(oldOffsets.clone());
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
			HelpViewer.start(main, R.string.help_aligningbackgroundiamgery);
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
	private class OffsetLoader extends AsyncTask<Double, Void, ArrayList<ImageryOffset>> {
		
		String error = null;
		final PostAsyncActionHandler handler;
		
		OffsetLoader(final PostAsyncActionHandler postLoadHandler) {
			handler = postLoadHandler;
		}
		
		ArrayList<ImageryOffset> getOffsetList(double lat, double lon, int radius) {
			Uri.Builder uriBuilder = offsetServerUri.buildUpon()
					.appendPath("get")
					.appendQueryParameter("lat", String.valueOf(lat))
					.appendQueryParameter("lon", String.valueOf(lon));
			if (radius > 0) {
				uriBuilder.appendQueryParameter("radius", String.valueOf(radius));
			}
			uriBuilder.appendQueryParameter("imagery", osmts.getImageryOffsetId())
				.appendQueryParameter("format", "json");
		
			String urlString = uriBuilder.build().toString();
			try {
				Log.d(DEBUG_TAG, "urlString " + urlString);
				URL url = new URL(urlString);
				HttpURLConnection conn = (HttpURLConnection) url.openConnection();
				conn.setRequestProperty("User-Agent", App.userAgent);
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
									Log.d(DEBUG_TAG, "search error " + error);
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
					if(error != null) {
						Log.d(DEBUG_TAG, "search error " + error);
					}
					return result;
				}
				finally {
					try {
						reader.close();
					} catch (IOException ioex) {
						Log.d(DEBUG_TAG,"Ignoring " + ioex);
					}
				}			
			} catch (MalformedURLException e) {
				error = e.getMessage();
			} catch (IOException e) {
				error = e.getMessage();
			}		
			Log.d(DEBUG_TAG, "search error " + error);
			return null;
		}

		@Override
		protected void onPreExecute() {
			Progress.showDialog(main, Progress.PROGRESS_SEARCHING);
		}
		
		@Override
		protected ArrayList<ImageryOffset> doInBackground(Double... params) {
	    	
			if (params.length != 3) {
				Log.e(DEBUG_TAG, "wrong number of params in OffsetLoader " + params.length);
				return null;
			}
			double centerLat = params[0];
			double centerLon = params[1];
			int radius = (int) (params[2] == null ? 0 : params[2]);
			ArrayList<ImageryOffset> result = getOffsetList(centerLat, centerLon, radius);
			if (result == null || result.size() == 0) {
				// retry with max radius
				Log.d(DEBUG_TAG, "retrying search with max radius");
				result = getOffsetList(centerLat, centerLon, 0);
			}
			return result;
		}
		
		@Override
		protected void onPostExecute(ArrayList<ImageryOffset> res) {
			Progress.dismissDialog(main, Progress.PROGRESS_SEARCHING);
			offsetList = res;
			if (handler != null) {
				handler.onSuccess();
			}
		}
		
		String getError() {
			return error;
		}
	}
	
	/**
	 * Save offsets 
	 * @author simon
	 *
	 */
	private class OffsetSaver extends AsyncTask<ImageryOffset, Void, Integer> {
		String error = null;

		@Override
		protected void onPreExecute() {
			Progress.showDialog(main, Progress.PROGRESS_SAVING);
		}
		
		@Override
		protected Integer doInBackground(ImageryOffset... params) {
	    	
			try {
				ImageryOffset offset = params[0];
				String urlString = offset.toSaveUrl();
				Log.d(DEBUG_TAG,"urlString " + urlString);
				URL url = new URL(urlString);
				HttpURLConnection conn = (HttpURLConnection) url.openConnection();
				conn.setRequestMethod("POST");
				conn.setRequestProperty("User-Agent", App.userAgent);
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
			Progress.dismissDialog(main, Progress.PROGRESS_SAVING);
			if (res == 200) {
				Snack.barInfo(main, R.string.toast_save_done);
			} else {
				Snack.barError(main, R.string.toast_save_failed);
			}
		}
		
		String getError() {
			return error;
		}
	}

	/**
	 * Get offset from server.
	 */
	private void getOffsetFromDB() {

		// first try for our view box
		final BoundingBox bbox = map.getViewBox();
		final double centerLat = bbox.getCenterLat();
		final double centerLon = (bbox.getLeft() + bbox.getWidth()/2)/1E7d;
		final Comparator<ImageryOffset> cmp = new Comparator<ImageryOffset>() {
			@Override
			public int compare(ImageryOffset  offset1, ImageryOffset  offset2)
			{
				double d1 = GeoMath.haversineDistance(centerLon, centerLat, offset1.lon, offset1.lat);
				double d2 = GeoMath.haversineDistance(centerLon, centerLat, offset2.lon, offset2.lat);
				return  Double.valueOf(d1).compareTo(Double.valueOf(d2));
			}
		};
		PostAsyncActionHandler handler = new PostAsyncActionHandler() {
			@Override
			public void onSuccess() {
				if (offsetList != null && offsetList.size() > 0) {
					Collections.sort(offsetList, cmp);
					AppCompatDialog d = createDisplayOffsetDialog(0);
					d.show();
				} else {
					displayError(main.getString(R.string.imagery_offset_not_found));
				}
			}
			@Override
			public void onError() {	
			}
		};
		OffsetLoader loader = new OffsetLoader(handler); 

		double hm = GeoMath.haversineDistance(centerLon, bbox.getBottom()/1E7d, centerLon, bbox.getTop()/1E7d);
		double wm = GeoMath.haversineDistance(bbox.getLeft()/1E7d, centerLat, bbox.getRight()/1E7d, centerLat);
		int radius = (int)Math.max(1, Math.round(Math.min(hm,wm)/2000d)); // convert to km and make it at least 1 and /2 for radius
		loader.execute(centerLat,centerLon,Double.valueOf(radius));
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
					error = main.getString(R.string.toast_timeout);
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
				if (lastOffset != null && im != null) {
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
				im.date = DateFormatter.getFormattedString(
						ImageryOffset.DATE_PATTERN_IMAGERY_OFFSET_CREATED_AT, c.getTime());
				im.author = author;
				offsetList.add(im);
			}
			lastOffset = o;
		}
		
		if (offsetList.size() > 0) {
			AppCompatDialog d = createSaveOffsetDialog(0, offsetList);
			d.show();
		} 
	}

	private void displayError(String error) {
		if (error != null) { // try to avoid code dup
			AlertDialog.Builder builder = new AlertDialog.Builder(main);
			builder.setMessage(error).setTitle(R.string.imagery_offset_title);
			builder.setPositiveButton(R.string.okay, null);
			AlertDialog dialog = builder.create();
			dialog.show();
		}
	}

	private ImageryOffset readOffset(JsonReader reader) throws IOException {
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
		if ("offset".equals(type))
			return result;

		return null;
	}
	
	private DeprecationNote readDeprecated(JsonReader reader) throws IOException {
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
	    @SuppressWarnings("unused")
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
	    
		/**
		 * Date pattern used to describe when the imagery offset was created.
		 */
		static final String DATE_PATTERN_IMAGERY_OFFSET_CREATED_AT = "yyyy-MM-dd";

		public String toSaveUrl() {
			Uri uriBuilder = offsetServerUri.buildUpon()
					.appendPath("store")
					.appendQueryParameter("lat", String.format(Locale.US, "%.7f", lat))
					.appendQueryParameter("lon", String.format(Locale.US, "%.7f", lon))
					.appendQueryParameter("author", author)
					.appendQueryParameter("description", description)
					.appendQueryParameter("imagery", imageryId)
					.appendQueryParameter("imlat", String.format(Locale.US, "%.7f", imageryLat))
					.appendQueryParameter("imlon", String.format(Locale.US, "%.7f", imageryLon))
					.build();
			return uriBuilder.toString();
		}
	}
	
	/**
	 * Object to hold the output from the imagery DB see https://wiki.openstreetmap.org/wiki/Imagery_Offset_Database/API
	 * Currently we don't actually display the contents anywhere
	 * @author simon
	 *
	 */
	private class DeprecationNote {
		@SuppressWarnings("unused")
		String author;
		@SuppressWarnings("unused")
		String date;
		@SuppressWarnings("unused")
		String reason;
	}
	
	/**
	 * Create the imagery offset display/apply dialog ... given that it has so much logic, done here instead of DialogFactory
	 * @param index
	 * @return
	 */
	@SuppressLint("InflateParams")
	private AppCompatDialog createSaveOffsetDialog(final int index, final ArrayList<ImageryOffset> saveOffsetList) {
		// Create some useful objects
		// final BoundingBox bbox = map.getViewBox();
		final LayoutInflater inflater = ThemeUtils.getLayoutInflater(main);
		Builder dialog = new AlertDialog.Builder(main);
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
		off.setText(String.format(Locale.US,"%.2f",GeoMath.haversineDistance(offset.lon,offset.lat,offset.imageryLon,offset.imageryLat))+" meters");
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
					AppCompatDialog d = createSaveOffsetDialog(index+1,saveOffsetList);
					d.show();
				}
			});
		return dialog.create();
	}
	
	/**
	 * Create an onClick listener that saves the current offset to the offset DB and (if it exists) displays the next offset to be saved
	 * @param description desciption of the offset in question
	 * @param author author
	 * @param index index in the list
	 * @return the OnClickListnener
	 */
	private OnClickListener createSaveButtonListener(final EditText description, final EditText author, final int index, final ArrayList<ImageryOffset> saveOffsetList) {
		
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
					AppCompatDialog d = createSaveOffsetDialog(index+1,saveOffsetList);
					d.show();
				}
			}
		};
	}

	@SuppressLint("InflateParams")
	private AppCompatDialog createDisplayOffsetDialog(final int index) {
		// Create some useful objects
		final BoundingBox bbox = map.getViewBox();
		final LayoutInflater inflater = ThemeUtils.getLayoutInflater(main);
		
		Builder dialog = new AlertDialog.Builder(main);
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
		off.setText(String.format(Locale.US,"%.2f",GeoMath.haversineDistance(offset.lon,offset.lat,offset.imageryLon,offset.imageryLat))+" meters");
		if (offset.date != null) {
			TextView created = (TextView) layout.findViewById(R.id.imagery_offset_date);
			created.setText(offset.date);
		}
		TextView minmax = (TextView) layout.findViewById(R.id.imagery_offset_zoom);
		minmax.setText(offset.minZoom + "-" + offset.maxZoom);
		TextView distance = (TextView) layout.findViewById(R.id.imagery_offset_distance);
		distance.setText(String.format(Locale.US,"%.3f",GeoMath.haversineDistance((bbox.getLeft() + bbox.getWidth()/2)/1E7d, bbox.getCenterLat(), offset.lon, offset.lat)/1000) +" km");
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
					AppCompatDialog d = createDisplayOffsetDialog(index+1);
					d.show();
				}
			});
		return dialog.create();
	}
	
	@Override
	public void onDestroyActionMode(ActionMode mode) {
		if (cabBottomBar != null) {
			cabBottomBar.setVisibility(View.GONE);
		}
		main.showBottomBar();
		main.setMode(main, oldMode);
		main.showLock();
	}

}
