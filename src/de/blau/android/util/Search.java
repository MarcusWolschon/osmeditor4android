package de.blau.android.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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
import de.blau.android.osm.BoundingBox;
import de.blau.android.util.jsonreader.JsonReader;
import okhttp3.Call;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Search with nominatim
 * @author simon
 *
 */
public class Search {

	public static final String NOMINATIM_SERVER = "http://nominatim.openstreetmap.org/"; //TODO set in prefs
	
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
	 * Query nominatim and then display a list of results to pick from
	 * @param q
	 */
	public void find(String q, BoundingBox bbox) {
		QueryNominatim querier = new QueryNominatim(bbox);
		querier.execute(q);
		try {
			ArrayList<SearchResult> result = querier.get(20, TimeUnit.SECONDS);
			if (result != null && result.size() > 0) {
				AppCompatDialog sr = createSearchResultsDialog(result);
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


	private class QueryNominatim extends AsyncTask<String, Void, ArrayList<SearchResult>> {
		final BoundingBox bbox;

		public QueryNominatim() {
			this(null);
		}

		public QueryNominatim(BoundingBox bbox) {
			this.bbox = bbox;
		}

		@Override
		protected void onPreExecute() {
			Progress.showDialog(activity, Progress.PROGRESS_SEARCHING);
		}

		@Override
		protected ArrayList<SearchResult> doInBackground(String... params) {

			String query = params[0];
			Uri.Builder builder = Uri.parse(NOMINATIM_SERVER)
					.buildUpon()
					.appendPath("search")
					.appendQueryParameter("q", query);
			if (bbox != null) {
				String viewBoxCoordinates = bbox.getLeft()
						+ "," + bbox.getBottom()
						+ "," + bbox.getRight()
						+ "," + bbox.getTop();
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
						SearchResult searchResult = readResult(reader);
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

		@Override
		protected void onPostExecute(ArrayList<SearchResult> res) {
			Progress.dismissDialog(activity, Progress.PROGRESS_SEARCHING);
		}
	}

	public SearchResult readResult(JsonReader reader) {
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
	
	@SuppressLint("InflateParams")
	private AppCompatDialog createSearchResultsDialog(final ArrayList<SearchResult> searchResults) {
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
		lv.setAdapter(new ArrayAdapter<String>(activity, R.layout.search_results_item, ar));
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
