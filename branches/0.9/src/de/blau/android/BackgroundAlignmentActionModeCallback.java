package de.blau.android;



import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

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
	
	ArrayList<ImageryOffset> offsetList;
	
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
		menu.add(Menu.NONE, MENUITEM_QUERYDB, Menu.NONE, R.string.menu_tools_background_align_retrieve_from_db);
		// menu.add(Menu.NONE, MENUITEM_QUERYLOCAL, Menu.NONE, R.string.menu_tools_background_align_retrieve_from_device);
		menu.add(Menu.NONE, MENUITEM_RESET, Menu.NONE, R.string.menu_tools_background_align_reset);
		menu.add(Menu.NONE, MENUITEM_ZERO, Menu.NONE, R.string.menu_tools_background_align_zero);
		menu.add(Menu.NONE, MENUITEM_APPLY2ALL, Menu.NONE, R.string.menu_tools_background_align_apply2all);
		// menu.add(Menu.NONE, MENUITEM_SAVE2DB, Menu.NONE, R.string.menu_tools_background_align_save_db);
		// menu.add(Menu.NONE, MENUITEM_SAVELOCAL, Menu.NONE, R.string.menu_tools_background_align_save_device);
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
	
	/**
	 * Download offsets 
	 * @author simon
	 *
	 */
	private class OffsetLoader extends AsyncTask<Integer, Void, ArrayList<ImageryOffset>> {
		
		@Override
		protected void onPreExecute() {
		
		}
		
		@Override
		protected ArrayList<ImageryOffset> doInBackground(Integer... params) {
	    	
			BoundingBox bbox = Application.mainActivity.getMap().getViewBox();
			double centerLon = (bbox.getLeft() + ((long)bbox.getRight() - (long)bbox.getLeft())/2L) / 1E7d;
			try {
				Integer radius = params[0];
				String urlString = "http://offsets.textual.ru/get?lat=" + bbox.getCenterLat() + "&lon=" + centerLon 
						+ (radius != null && radius > 0 ? "&radius=" + radius : "") + "&imagery=" + osmts.getImageryOffsetId() + "&format=json";
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
							if (imOffset != null && imOffset.deprecated == null) //TODO handle deprecated 
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

	private void getOffsetFromDB() {
		OffsetLoader loader = new OffsetLoader(); 
		
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
			offsetList = loader.get(60, TimeUnit.SECONDS);
			if (offsetList != null && offsetList.size() > 0) {
				Collections.sort(offsetList, cmp);
				Dialog d = createOffsetDialog(0);
				d.show();
			} else {
				loader.cancel(true);
				loader = new OffsetLoader();
				loader.execute(Integer.valueOf(0));
				offsetList = loader.get(60, TimeUnit.SECONDS);
				if (offsetList != null && offsetList.size() > 0) {
					Collections.sort(offsetList, cmp);
					Dialog d = createOffsetDialog(0);
					d.show();
				} else {
					AlertDialog.Builder builder = new AlertDialog.Builder(Application.mainActivity);
					builder.setMessage(R.string.imagery_offset_not_found).setTitle(R.string.imagery_offset_title);
					AlertDialog dialog = builder.create();
					dialog.show();
				}
			}
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
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	protected DeprecationNote readDeprecated(JsonReader reader) {
		DeprecationNote result = new DeprecationNote();
		try {
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
	    String date;
	    String imageryId;
	    int minZoom = 0;
	    int maxZoom = 18;
	    double imageryLat = 0;
	    double imageryLon = 0;
	    DeprecationNote deprecated = null;
	}
	
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
	private Dialog createOffsetDialog(final int index) {
		// Create some useful objects
		final BoundingBox bbox = map.getViewBox();
		final Context context = Application.mainActivity.getApplicationContext();
		final LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
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
					Dialog d = createOffsetDialog(index+1);
					d.show();
				}
			});
		return dialog.create();
	}
	@Override
	public void onDestroyActionMode(ActionMode mode) {
		Main.logic.setMode(oldMode);

	}

}
