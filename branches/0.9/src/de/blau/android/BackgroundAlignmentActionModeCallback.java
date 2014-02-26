package de.blau.android;



import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.AsyncTask;
import android.util.Log;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.ActionMode.Callback;

import de.blau.android.Logic.Mode;
import de.blau.android.osm.BoundingBox;
import de.blau.android.util.GeoMath;
import de.blau.android.util.jsonreader.JsonReader;
import de.blau.android.views.util.OpenStreetMapTileServer;
import de.blau.android.util.Offset;

public class BackgroundAlignmentActionModeCallback implements Callback {
	
	private static final int MENUITEM_QUERYDB = 1;
	private static final int MENUITEM_QUERYLOCAL = 2;
	private static final int MENUITEM_APPLY2ALL = 3;
	private static final int MENUITEM_RESET = 4;
	private static final int MENUITEM_ZERO = 5;
	private static final int MENUITEM_SAVE2DB = 6;
	private static final int MENUITEM_SAVELOCAL = 7;
	
	Mode oldMode;
	Offset[] oldOffsets;
	
	OpenStreetMapTileServer osmts;
	Map map;
	
	public BackgroundAlignmentActionModeCallback(Mode oldMode) {
		this.oldMode = oldMode;
		map = Application.mainActivity.getMap();
		osmts = map.getOpenStreetMapTilesOverlay().getRendererInfo();
		oldOffsets = osmts.getOffsets().clone();
	}

	@Override
	public boolean onCreateActionMode(ActionMode mode, Menu menu) {
		mode.setTitle(R.string.menu_tools_background_align);
		return true;
	}

	@Override
	public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
		menu.clear();
		menu.add(Menu.NONE, MENUITEM_QUERYDB, Menu.NONE, "From DB");
		// menu.add(Menu.NONE, MENUITEM_QUERYLOCAL, Menu.NONE, "From Device");
		menu.add(Menu.NONE, MENUITEM_RESET, Menu.NONE, R.string.menu_tools_background_align_reset);
		menu.add(Menu.NONE, MENUITEM_ZERO, Menu.NONE, "Zero");
		menu.add(Menu.NONE, MENUITEM_APPLY2ALL, Menu.NONE, "Apply to all Zooms");
		// menu.add(Menu.NONE, MENUITEM_SAVE2DB, Menu.NONE, "Save to DB");
		// menu.add(Menu.NONE, MENUITEM_SAVELOCAL, Menu.NONE, "Save locally");
		return true;
	}

	@Override
	public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
		switch (item.getItemId()) {
		case MENUITEM_ZERO: 
			osmts.setOffset(0.0d,0.0d);
			map.invalidate();
			break;
		case MENUITEM_RESET: 
			osmts.setOffsets(oldOffsets);
			map.invalidate();
			break;
		case MENUITEM_APPLY2ALL: 
			Offset o = osmts.getOffset(map.getZoomLevel());
			osmts.setOffset(o.lon,o.lat);
			break;
		case MENUITEM_QUERYDB: 
			getOffsetFromDB();
			break;
		case MENUITEM_QUERYLOCAL:
			break;
		case MENUITEM_SAVE2DB:
			break;
		case MENUITEM_SAVELOCAL:
			break;
		default: return false;
		}
		return true;
	}

	private void getOffsetFromDB() {
		AsyncTask<Void, Void, ArrayList<ImageryOffset>> loader = new AsyncTask<Void, Void, ArrayList<ImageryOffset>>() {
			
			@Override
			protected void onPreExecute() {
			
			}
			
			@Override
			protected ArrayList<ImageryOffset> doInBackground(Void... v) {
		    	
				BoundingBox bbox = Application.mainActivity.getMap().getViewBox();
				double centerLon = (bbox.getLeft() + ((long)bbox.getRight() - (long)bbox.getLeft())/2L) / 1E7d;
				try {
					String urlString = "http://offsets.textual.ru/get?lat=" + bbox.getCenterLat() + "&lon=" + centerLon +"&imagery=" + osmts.getImageryOffsetId() + "&format=json";
					Log.d("BackgroundAlignmentActionModeCallback","urlString " + urlString);
					URL url = new URL(urlString);
					URLConnection conn = url.openConnection();
					conn.setRequestProperty("User-Agent", Application.userAgent);
					JsonReader reader = new JsonReader(new InputStreamReader(conn.getInputStream()));
					ArrayList<ImageryOffset> result = new ArrayList<ImageryOffset>();
					try {
						
						try {
							reader.beginArray();
							while (reader.hasNext()) {
								ImageryOffset imOffset = readOffset(reader);
								if (imOffset != null)
									result.add(imOffset);
							}
							reader.endArray();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						return result;
					}
					finally {
					       reader.close();
					}			
				} catch (MalformedURLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				return null;
			}
			
			@Override
			protected void onPostExecute(ArrayList<ImageryOffset> res) {
				
			}
		};
		loader.execute();
		try {
			ArrayList<ImageryOffset> offsetList = loader.get(60, TimeUnit.SECONDS);
			if (offsetList != null) {
				for (ImageryOffset imOffset:offsetList) {
					Dialog d = createOffsetDialog(imOffset, offsetList.indexOf(imOffset) == (offsetList.size()-1));
					d.show();
				}
			} else
				Log.d("BackgroundAlignmentActionModeCallback","offset list is empty");
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TimeoutException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	protected ImageryOffset readOffset(JsonReader reader) {
		String type = null;
		ImageryOffset result = new ImageryOffset();
		try {
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
			    } else {
			    	reader.skipValue();
			    }
			}
			reader.endObject();
			if (type.equals("offset"))
					return result;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	private class ImageryOffset {
	    long id;
	    double lat = 0;
	    double lon = 0;
	    String author;
	    String description;
	    Date date;
	    String imageryId;
	    int minZoom = 0;
	    int maxZoom = 18;
	    double imageryLat = 0;
	    double imageryLon = 0;
	}
	
	private Dialog createOffsetDialog(final ImageryOffset offset, boolean atEnd) {
		Builder dialog = new AlertDialog.Builder(Application.mainActivity);
		dialog.setTitle("Imagery Offset");
		dialog.setMessage(offset.description + "\n\n" + "Offset " + String.format("%.2f",GeoMath.haversineDistance(offset.lon,offset.lat,offset.imageryLon,offset.imageryLat))+" meters");
		dialog.setPositiveButton("Apply", new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				osmts.setOffset(map.getZoomLevel(),offset.lon-offset.imageryLon, offset.lat-offset.imageryLat);
				map.invalidate();
			}
		});
		dialog.setNeutralButton("Apply (all zooms)",new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				osmts.setOffset(offset.minZoom,offset.maxZoom,offset.lon-offset.imageryLon, offset.lat-offset.imageryLat);
				map.invalidate();
			}
		});
		if (atEnd)
			dialog.setNegativeButton("Cancel", null);
		else
			dialog.setNegativeButton("Next", null);
		return dialog.create();
	}
	@Override
	public void onDestroyActionMode(ActionMode mode) {
		Main.logic.setMode(oldMode);

	}

}
