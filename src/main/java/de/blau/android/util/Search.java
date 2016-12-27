package de.blau.android.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;

import android.annotation.SuppressLint;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AlertDialog.Builder;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import de.blau.android.Application;
import de.blau.android.R;
import de.blau.android.dialogs.Progress;
import de.blau.android.dialogs.ProgressDialog;
import de.blau.android.osm.BoundingBox;
import de.blau.android.prefs.AdvancedPrefDatabase.Geocoder;
import de.blau.android.presets.Preset;
import de.blau.android.presets.Preset.PresetItem;
import de.blau.android.util.mapbox.geojson.Feature;
import de.blau.android.util.mapbox.geojson.FeatureCollection;
import de.blau.android.util.mapbox.geojson.Geometry;
import de.blau.android.util.mapbox.geojson.Point;
import de.blau.android.util.mapbox.models.Position;
import okhttp3.Call;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Search with nominatim, photon and maybe others
 * @author simon
 *
 */
public class Search {
	
	private AppCompatActivity activity;

	private SearchItemFoundCallback callback;

	public class SearchResult {
		private double lat;
		private double lon;
		String display_name;

		@Override
		public String toString() {
			return "lat: " + getLat() + " lon: " + getLon() + " " + display_name;
		}

		/**
		 * @return the lat
		 */
		public double getLat() {
			return lat;
		}

		/**
		 * @param lat the lat to set
		 */
		public void setLat(double lat) {
			this.lat = lat;
		}

		/**
		 * @return the lon
		 */
		public double getLon() {
			return lon;
		}

		/**
		 * @param lon the lon to set
		 */
		public void setLon(double lon) {
			this.lon = lon;
		}
	}

	/**
	 * Constructor
	 * @param appCompatActivity
	 * @param callback will be called when search result is selected
	 */
	public Search(AppCompatActivity appCompatActivity, SearchItemFoundCallback callback) {
		this.activity = appCompatActivity;
		this.callback = callback;
	}

	/**
	 * Query and then display a list of results to pick from
	 * @param q
	 */
	public void find(Geocoder geocoder, String q, BoundingBox bbox) {
		Query querier = null;
		boolean multiline = false;
		switch (geocoder.type) {
		case PHOTON:
			querier = new QueryPhoton(geocoder.url, bbox);
			multiline = true;
			break;
		case NOMINATIM:
			querier = new QueryNominatim(geocoder.url, bbox);
			multiline = false;
			break;
		}
		querier.execute(q);
		try {
			ArrayList<SearchResult> result = querier.get(20, TimeUnit.SECONDS);
			if (result != null && result.size() > 0) {
				AppCompatDialog sr = createSearchResultsDialog(result, multiline ? R.layout.search_results_item_multi_line : R.layout.search_results_item);
				sr.show();
			} else {
				Toast.makeText(activity, R.string.toast_nothing_found, Toast.LENGTH_LONG).show();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		} catch (TimeoutException e) {
			Toast.makeText(activity, R.string.toast_timeout, Toast.LENGTH_LONG).show();
			e.printStackTrace();
		}
	}

	private class Query extends AsyncTask<String, Void, ArrayList<SearchResult>> {
		AlertDialog progress = null;
		
		final BoundingBox bbox;
		final String url;

		public Query() {
			this(null, null);
		}

		public Query(String url, BoundingBox bbox) {
			this.url = url;
			this.bbox = bbox;
		}
		
		@Override
		protected void onPreExecute() {
			progress = ProgressDialog.get(activity, Progress.PROGRESS_LOADING);
			progress.show();
		}
		
		@Override
		protected ArrayList<SearchResult> doInBackground(String... params) {
			return null;
		}
		
		@Override
		protected void onPostExecute(ArrayList<SearchResult> res) {
			try {
				progress.dismiss();
			} catch (Exception ex) {
				Log.e("Search", "dismiss dialog failed with " + ex);
			}
		}
	}

	private class QueryNominatim extends Query {
		
		public QueryNominatim() {
			super(null, null);
		}

		public QueryNominatim(String url, BoundingBox bbox) {
			super(url, bbox);
		}
		
		@Override
		protected ArrayList<SearchResult> doInBackground(String... params) {

			String query = params[0];
			Uri.Builder builder = Uri.parse(url)
					.buildUpon()
					.appendPath("search")
					.appendQueryParameter("q", query);
			if (bbox != null) {
				String viewBoxCoordinates = bbox.getLeft()/1E7D
						+ "," + bbox.getBottom()/1E7D
						+ "," + bbox.getRight()/1E7D
						+ "," + bbox.getTop()/1E7D;
				builder.appendQueryParameter("viewboxlbrt", viewBoxCoordinates);
			}
			Uri uriBuilder = builder.appendQueryParameter("format", "jsonv2").build();

			String urlString = uriBuilder.toString();
			Log.d("Search", "urlString: " + urlString);
			InputStream inputStream = null;
			JsonReader reader = null;
			ResponseBody responseBody = null;
			try {
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
					Request request = new Request.Builder()
							.url(urlString)
							.build();
					Call searchCall = Application.getHttpClient().newCall(request);
					Response searchCallResponse = searchCall.execute();
					if (searchCallResponse.isSuccessful()) {
						responseBody = searchCallResponse.body();
						inputStream = responseBody.byteStream();
					}
				} else { //FIXME 2.2/API 8 support
					URL url = new URL(urlString);
					HttpURLConnection conn = (HttpURLConnection) url.openConnection();
					conn.setRequestProperty("User-Agent", Application.userAgent);
					inputStream = conn.getInputStream();
				}

				if (inputStream != null) {
					reader = new JsonReader(new InputStreamReader(inputStream));
					ArrayList<SearchResult> result = new ArrayList<SearchResult>();
					reader.beginArray();
					while (reader.hasNext()) {
						SearchResult searchResult = readNominatimResult(reader);
						if (searchResult != null) { //TODO handle deprecated
							result.add(searchResult);
							Log.d("Search", "received: " + searchResult.toString());
						}
					}
					reader.endArray();
					return result;
				}
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {
					if (responseBody != null) {
						responseBody.close();
					}
					if (reader != null) {
						reader.close();
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			return null;
		}
	}

	public SearchResult readNominatimResult(JsonReader reader) {
		SearchResult result = new SearchResult();
		try {
			reader.beginObject();
			while (reader.hasNext()) {
				String jsonName = reader.nextName();
				if (jsonName.equals("lat")) {
			        result.setLat(reader.nextDouble());
			    } else if (jsonName.equals("lon")) {
			        result.setLon(reader.nextDouble());
			    } else if (jsonName.equals("display_name")) {
			    	result.display_name = reader.nextString();
			    }else {
			    	reader.skipValue();
			    }
			}
			reader.endObject();
			return result;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	private class QueryPhoton extends Query {
		
		public QueryPhoton() {
			super(null, null);
		}

		public QueryPhoton(String url, BoundingBox bbox) {
			super(url, bbox);
		}
		
		@Override
		protected ArrayList<SearchResult> doInBackground(String... params) {

			String query = params[0];
			Uri.Builder builder = Uri.parse(url)
					.buildUpon()
					.appendPath("api")
					.appendQueryParameter("q", query);
			if (bbox != null) {
				double lat = bbox.getCenterLat();
				double lon = (bbox.getLeft() + (bbox.getRight()-bbox.getLeft())/2)/1E7D;
				builder.appendQueryParameter("lat", Double.toString(lat));
				builder.appendQueryParameter("lon", Double.toString(lon));
			}
			builder.appendQueryParameter("limit", Integer.toString(10));
			Uri uriBuilder = builder.build();

			String urlString = uriBuilder.toString();
			Log.d("Search", "urlString: " + urlString);
			InputStream inputStream = null;
			JsonReader reader = null;
			ResponseBody responseBody = null;
			try {
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
					Request request = new Request.Builder()
							.url(urlString)
							.build();
					Call searchCall = Application.getHttpClient().newCall(request);
					Response searchCallResponse = searchCall.execute();
					if (searchCallResponse.isSuccessful()) {
						responseBody = searchCallResponse.body();
						inputStream = responseBody.byteStream();
					}
				} else { //FIXME 2.2/API 8 support
					URL url = new URL(urlString);
					HttpURLConnection conn = (HttpURLConnection) url.openConnection();
					conn.setRequestProperty("User-Agent", Application.userAgent);
					inputStream = conn.getInputStream();
				}

				if (inputStream != null) {
			        BufferedReader rd = new BufferedReader(new InputStreamReader(inputStream, Charset.forName("UTF-8")));
			        StringBuilder sb = new StringBuilder();
			        int cp;
			        while ((cp = rd.read()) != -1) {
			          sb.append((char) cp);
			        }
			        inputStream.close();
			        
					ArrayList<SearchResult> result = new ArrayList<SearchResult>();
					FeatureCollection fc = FeatureCollection.fromJson(sb.toString());
					for (Feature f:fc.getFeatures()) {
						SearchResult searchResult = readPhotonResult(f);
						if (searchResult != null) { 
							result.add(searchResult);
							Log.d("Search", "received: " + searchResult.toString());
						}
					}
					return result;
				}
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {
					if (responseBody != null) {
						responseBody.close();
					}
					if (reader != null) {
						reader.close();
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			return null;
		}
	}
	
	public SearchResult readPhotonResult(Feature f) {
		SearchResult result = new SearchResult();
		try {
			JsonObject properties = f.getProperties();
			Geometry g = f.getGeometry();
			if (g instanceof Point) {
				Point p = (Point)g;
				Position pos = p.getCoordinates();
				result.setLat(pos.getLatitude());
			    result.setLon(pos.getLongitude());
			    StringBuilder sb = new StringBuilder();
			    JsonElement name = properties.get("name");
			    if (name != null) {
			    	sb.append(name.getAsString());
			    	JsonElement osmKey = properties.get("osm_key");
			    	JsonElement osmValue = properties.get("osm_value");
				    if (osmKey != null && osmValue != null) {
				    	String key = osmKey.getAsString();
				    	String value = osmValue.getAsString();
				    	Map<String,String> tag = new HashMap<String,String>();
				    	tag.put(key,value);
					    PresetItem preset = Preset.findBestMatch(Application.getCurrentPresets(activity), tag, false);
					    if (preset != null) {
					    	sb.append(" [" + preset.getTranslatedName() +"]");
					    } else {
					    	sb.append(" [" + key + "=" + value +"]");
					    }
			    	}
			    	StringBuilder sb2 = new StringBuilder();
			    	JsonElement street = properties.get("street");
			    	if (street != null) {
			    		sb2.append(street.getAsString());
				    	JsonElement housenumber = properties.get("housenumber");
				    	if (housenumber != null) {
				    		sb2.append( " " + housenumber.getAsString());
				    	}
			    	}
			    	JsonElement postcode = properties.get("postcode");
			    	if (postcode != null) {
			    		if (sb2.length() > 0) {
			    			sb2.append(", ");
			    		}
			    		sb2.append(postcode.getAsString());
			    	}
			    	JsonElement state = properties.get("state");
			    	if (state != null) {
			    		if (sb2.length() > 0) {
			    			sb2.append(", ");
			    		}
			    		sb2.append(state.getAsString());
			    	}
			    	JsonElement country = properties.get("country");
			    	if (country != null) {
			    		if (sb2.length() > 0) {
			    			sb2.append(", ");
			    		}
			    		sb2.append(country.getAsString());
			    	}
			    	if (sb2.length() > 0) {
			    		sb.append("\n");
			    		sb.append(sb2);
			    	}
			    }
			    result.display_name = sb.toString();
				return result;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	
	@SuppressLint("InflateParams")
	private AppCompatDialog createSearchResultsDialog(final ArrayList<SearchResult> searchResults, int itemLayout) {
		// 
		Builder builder = new AlertDialog.Builder(activity);
		builder.setTitle(R.string.search_results_title);
		final LayoutInflater inflater = ThemeUtils.getLayoutInflater(activity);
		ListView lv = (ListView) inflater.inflate(R.layout.search_results, null);
		builder.setView(lv);

		ArrayList<String> ar = new ArrayList<String>();
		for (SearchResult sr:searchResults) {
			ar.add(sr.display_name);
		}
		lv.setAdapter(new ArrayAdapter<String>(activity, itemLayout, ar));
		lv.setSelection(0);
		builder.setNegativeButton(R.string.cancel, null);
		final AppCompatDialog dialog = builder.create();
		lv.setOnItemClickListener( new AdapterView.OnItemClickListener() {
		    public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
		    	// Log.d("Search","Result at pos " + position + " clicked");
		    	callback.onItemFound(searchResults.get(position));
		    	dialog.dismiss();
		    }
		});
		return dialog;
	}
}
