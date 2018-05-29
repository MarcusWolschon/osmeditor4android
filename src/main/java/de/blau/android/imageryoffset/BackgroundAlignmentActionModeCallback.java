package de.blau.android.imageryoffset;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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
import de.blau.android.imageryoffset.ImageryOffset.DeprecationNote;
import de.blau.android.osm.Server;
import de.blau.android.osm.ViewBox;
import de.blau.android.prefs.Preferences;
import de.blau.android.resources.TileLayerServer;
import de.blau.android.util.GeoMath;
import de.blau.android.util.MenuUtil;
import de.blau.android.util.SavingHelper;
import de.blau.android.util.Snack;
import de.blau.android.util.ThemeUtils;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class BackgroundAlignmentActionModeCallback implements Callback {

    private static final String DEBUG_TAG = "BackgroundAlign...";

    private static final int MENUITEM_QUERYDB    = 1;
    private static final int MENUITEM_QUERYLOCAL = 2;
    private static final int MENUITEM_APPLY2ALL  = 3;
    private static final int MENUITEM_RESET      = 4;
    private static final int MENUITEM_ZERO       = 5;
    private static final int MENUITEM_SAVE2DB    = 6;
    private static final int MENUITEM_SAVELOCAL  = 7;
    private static final int MENUITEM_HELP       = 8;

    private Mode              oldMode;
    private final Preferences prefs;
    private final Uri         offsetServerUri;

    private Offset[] oldOffsets;

    private TileLayerServer osmts;
    private final Map       map;
    private final Main      main;

    private ArrayList<ImageryOffset> offsetList;

    private ActionMenuView cabBottomBar;

    public BackgroundAlignmentActionModeCallback(Main main, Mode oldMode) {
        this.oldMode = oldMode;
        this.main = main; // currently we are only called from here
        map = main.getMap();
        osmts = map.getBackgroundLayer().getTileLayerConfiguration();
        oldOffsets = osmts.getOffsets().clone();
        prefs = new Preferences(main);
        String offsetServer = prefs.getOffsetServer();
        offsetServerUri = Uri.parse(offsetServer);
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        main.hideLock();
        main.hideLayersControl();
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
        if (cabBottomBar != null) {
            menu = cabBottomBar.getMenu();
            final ActionMode actionMode = mode;
            android.support.v7.widget.ActionMenuView.OnMenuItemClickListener listener = new android.support.v7.widget.ActionMenuView.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    return onActionItemClicked(actionMode, item);
                }
            };
            cabBottomBar.setOnMenuItemClickListener(listener);
            MenuUtil.setupBottomBar(main, cabBottomBar, main.isFullScreen(), prefs.lightThemeEnabled());
        }
        menu.clear();
        MenuItem mi = menu.add(Menu.NONE, MENUITEM_QUERYDB, Menu.NONE, R.string.menu_tools_background_align_retrieve_from_db)
                .setEnabled(main.isConnectedOrConnecting()).setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_download));
        MenuItemCompat.setShowAsAction(mi, MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);
        // menu.add(Menu.NONE, MENUITEM_QUERYLOCAL, Menu.NONE,
        // R.string.menu_tools_background_align_retrieve_from_device);
        mi = menu.add(Menu.NONE, MENUITEM_RESET, Menu.NONE, R.string.menu_tools_background_align_reset)
                .setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_undo));
        MenuItemCompat.setShowAsAction(mi, MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);
        menu.add(Menu.NONE, MENUITEM_ZERO, Menu.NONE, R.string.menu_tools_background_align_zero);
        menu.add(Menu.NONE, MENUITEM_APPLY2ALL, Menu.NONE, R.string.menu_tools_background_align_apply2all);
        menu.add(Menu.NONE, MENUITEM_SAVE2DB, Menu.NONE, R.string.menu_tools_background_align_save_db).setEnabled(main.isConnectedOrConnecting());
        // menu.add(Menu.NONE, MENUITEM_SAVELOCAL, Menu.NONE, R.string.menu_tools_background_align_save_device);
        menu.add(Menu.NONE, MENUITEM_HELP, Menu.NONE, R.string.menu_help);
        // Toolbar toolbar = (Toolbar) Application.mainActivity.findViewById(R.id.mainToolbar);
        // toolbar.setVisibility(View.GONE);;
        return true;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        switch (item.getItemId()) {
        case MENUITEM_ZERO:
            osmts.setOffset(map.getZoomLevel(), 0.0d, 0.0d);
            map.invalidate();
            break;
        case MENUITEM_RESET:
            osmts.setOffsets(oldOffsets.clone());
            map.invalidate();
            break;
        case MENUITEM_APPLY2ALL:
            Offset o = osmts.getOffset(map.getZoomLevel());
            if (o != null)
                osmts.setOffset(o.getDeltaLon(), o.getDeltaLat());
            else
                osmts.setOffset(0.0d, 0.0d);
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
        default:
            return false;
        }
        return true;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        if (cabBottomBar != null) {
            cabBottomBar.setVisibility(View.GONE);
        }
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                Log.i(DEBUG_TAG, "Saving offsets");
                List<ImageryOffset> offsets = ImageryOffsetUtils.offsets2ImageryOffset(osmts, map.getViewBox(), null);
                ImageryOffsetDatabase db = new ImageryOffsetDatabase(main);
                SQLiteDatabase writableDb = db.getWritableDatabase();
                ImageryOffsetDatabase.deleteOffset(writableDb, osmts.getImageryOffsetId());
                for (ImageryOffset im : offsets) {
                    ImageryOffsetDatabase.addOffset(writableDb, im);
                }
                return null;
            }
        }.execute();

        main.showBottomBar();
        main.setMode(main, oldMode);
        main.showLock();
        main.showLayersControl();
    }

    /**
     * Download offsets
     * 
     * @author simon
     *
     */
    private class OffsetLoader extends AsyncTask<Double, Void, ArrayList<ImageryOffset>> {

        String                       error = null;
        final PostAsyncActionHandler handler;

        OffsetLoader(final PostAsyncActionHandler postLoadHandler) {
            handler = postLoadHandler;
        }

        ArrayList<ImageryOffset> getOffsetList(double lat, double lon, int radius) {
            Uri.Builder uriBuilder = offsetServerUri.buildUpon().appendPath("get").appendQueryParameter("lat", String.valueOf(lat)).appendQueryParameter("lon",
                    String.valueOf(lon));
            if (radius > 0) {
                uriBuilder.appendQueryParameter("radius", String.valueOf(radius));
            }
            uriBuilder.appendQueryParameter("imagery", osmts.getImageryOffsetId()).appendQueryParameter("format", "json");

            String urlString = uriBuilder.build().toString();
            try {
                Log.d(DEBUG_TAG, "urlString " + urlString);
                InputStream inputStream = null;

                Request request = new Request.Builder().url(urlString).build();
                OkHttpClient client = App.getHttpClient().newBuilder().connectTimeout(Server.TIMEOUT, TimeUnit.MILLISECONDS)
                        .readTimeout(Server.TIMEOUT, TimeUnit.MILLISECONDS).build();
                Call offsetListCall = client.newCall(request);
                Response offestListCallResponse = offsetListCall.execute();
                if (offestListCallResponse.isSuccessful()) {
                    ResponseBody responseBody = offestListCallResponse.body();
                    inputStream = responseBody.byteStream();
                } else {
                    Server.throwOsmServerException(offestListCallResponse);
                }

                JsonReader reader = null;
                try {
                    reader = new JsonReader(new InputStreamReader(inputStream));
                    ArrayList<ImageryOffset> result = new ArrayList<>();
                    try {
                        JsonToken token = reader.peek();
                        if (token.equals(JsonToken.BEGIN_ARRAY)) {
                            reader.beginArray();
                            while (reader.hasNext()) {
                                ImageryOffset imOffset = readOffset(reader);
                                if (imOffset != null && imOffset.deprecated == null) // TODO handle deprecated
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
                    } catch (IOException | IllegalStateException e) {
                        error = e.getMessage();
                    }
                    if (error != null) {
                        Log.d(DEBUG_TAG, "search error " + error);
                    }
                    return result;
                } finally {
                    SavingHelper.close(reader);
                }
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
            if (result == null || result.isEmpty()) {
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
     * 
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
                String urlString = offset.toSaveUrl(offsetServerUri);
                Log.d(DEBUG_TAG, "urlString " + urlString);
                RequestBody reqbody = RequestBody.create(null, "");
                Request request = new Request.Builder().url(urlString).post(reqbody).build();
                OkHttpClient client = App.getHttpClient().newBuilder().connectTimeout(Server.TIMEOUT, TimeUnit.MILLISECONDS)
                        .readTimeout(Server.TIMEOUT, TimeUnit.MILLISECONDS).build();
                Call offsetListCall = client.newCall(request);
                Response offestListCallResponse = offsetListCall.execute();
                if (!offestListCallResponse.isSuccessful()) {
                    Server.throwOsmServerException(offestListCallResponse);
                }
                return offestListCallResponse.code();
            } catch (MalformedURLException e) {
                error = e.getMessage();
                return -2;
            } catch (Exception /* IOException */ e) {
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
     * Get offset from imagery offset database server.
     */
    private void getOffsetFromDB() {
        // first try for our view box
        final ViewBox bbox = map.getViewBox();
        final double centerLat = bbox.getCenterLat();
        final double centerLon = (bbox.getLeft() + bbox.getWidth() / 2) / 1E7d;
        final Comparator<ImageryOffset> cmp = new Comparator<ImageryOffset>() {
            @Override
            public int compare(ImageryOffset offset1, ImageryOffset offset2) {
                double d1 = GeoMath.haversineDistance(centerLon, centerLat, offset1.getLon(), offset1.getLat());
                double d2 = GeoMath.haversineDistance(centerLon, centerLat, offset2.getLon(), offset2.getLat());
                return Double.valueOf(d1).compareTo(d2);
            }
        };
        PostAsyncActionHandler handler = new PostAsyncActionHandler() {
            @Override
            public void onSuccess() {
                if (offsetList != null && !offsetList.isEmpty()) {
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

        double hm = GeoMath.haversineDistance(centerLon, bbox.getBottom() / 1E7d, centerLon, bbox.getTop() / 1E7d);
        double wm = GeoMath.haversineDistance(bbox.getLeft() / 1E7d, centerLat, bbox.getRight() / 1E7d, centerLat);
        int radius = (int) Math.max(1, Math.round(Math.min(hm, wm) / 2000d)); // convert to km and make it at least 1
                                                                              // and /2 for radius
        loader.execute(centerLat, centerLon, (double) radius);
    }

    /**
     * Save current offset to imagery offset database server
     */
    private void saveOffsetsToDB() {

        String author = null;
        String error = null;

        // try to find current display name
        final Server server = prefs.getServer();
        if (server != null) {
            if (!server.needOAuthHandshake()) {
                try {
                    AsyncTask<Void, Void, Server.UserDetails> loader = new AsyncTask<Void, Void, Server.UserDetails>() {

                        @Override
                        protected Server.UserDetails doInBackground(Void... params) {
                            return server.getUserDetails();
                        }
                    };
                    loader.execute();
                    Server.UserDetails user = loader.get(10, TimeUnit.SECONDS);

                    if (user != null) {
                        author = user.getDisplayName();
                    } else {
                        author = server.getDisplayName(); // maybe it has been configured
                    }
                } catch (InterruptedException | ExecutionException e) {
                    error = e.getMessage();
                } catch (TimeoutException e) {
                    error = main.getString(R.string.toast_timeout);
                }
                displayError(error);
            } else {
                author = server.getDisplayName(); // maybe it has been configured
            }
        }

        List<ImageryOffset> offsetsToSaveList = ImageryOffsetUtils.offsets2ImageryOffset(osmts, map.getViewBox(), author);

        if (!offsetsToSaveList.isEmpty()) {
            AppCompatDialog d = createSaveOffsetDialog(0, offsetsToSaveList);
            d.show();
        }
    }

    private void displayError(@Nullable String error) {
        if (error != null) { // try to avoid code dup
            AlertDialog.Builder builder = new AlertDialog.Builder(main);
            builder.setMessage(error).setTitle(R.string.imagery_offset_title);
            builder.setPositiveButton(R.string.okay, null);
            AlertDialog dialog = builder.create();
            dialog.show();
        }
    }

    /**
     * Parse an ImageryOffset from Json input
     * 
     * @param reader the JsonReader
     * @return an ImagerOffset or null if parsing failed
     * @throws IOException
     */
    @Nullable
    private ImageryOffset readOffset(@NonNull JsonReader reader) throws IOException {
        String type = null;
        ImageryOffset result = new ImageryOffset();

        reader.beginObject();
        while (reader.hasNext()) {
            String jsonName = reader.nextName();
            switch (jsonName) {
            case "type":
                type = reader.nextString();
                break;
            case "id":
                result.id = reader.nextLong();
                break;
            case "lat":
                result.setLat(reader.nextDouble());
                break;
            case "lon":
                result.setLon(reader.nextDouble());
                break;
            case "author":
                result.author = reader.nextString();
                break;
            case "date":
                result.date = reader.nextString();
                break;
            case "imagery":
                result.imageryId = reader.nextString();
                break;
            case "imlat":
                result.setImageryLat(reader.nextDouble());
                break;
            case "imlon":
                result.setImageryLon(reader.nextDouble());
                break;
            case "min-zoom":
                result.setMinZoom(reader.nextInt());
                break;
            case "max-zoom":
                result.setMaxZoom(reader.nextInt());
                break;
            case "description":
                result.description = reader.nextString();
                break;
            case "deprecated":
                result.deprecated = readDeprecated(reader);
                break;
            default:
                reader.skipValue();
                break;
            }
        }
        reader.endObject();
        if ("offset".equals(type)) {
            return result;
        }
        return null;
    }

    private DeprecationNote readDeprecated(JsonReader reader) throws IOException {
        DeprecationNote result = new DeprecationNote();

        reader.beginObject();
        while (reader.hasNext()) {
            String jsonName = reader.nextName();
            switch (jsonName) {
            case "author":
                result.author = reader.nextString();
                break;
            case "reason":
                result.reason = reader.nextString();
                break;
            case "date":
                result.date = reader.nextString();
                break;
            default:
                reader.skipValue();
                break;
            }
        }
        reader.endObject();
        return result;
    }

    /**
     * Create the imagery offset display/apply dialog
     * 
     * @param index index in to the list of ImageryOffset objects
     * @param saveOffsetList the list of ImageryOffset objects
     * @return a Dialog
     */
    @SuppressLint("InflateParams")
    private AppCompatDialog createSaveOffsetDialog(final int index, final List<ImageryOffset> saveOffsetList) {
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
        off.setText(main.getString(R.string.distance_meter,
                GeoMath.haversineDistance(offset.getLon(), offset.getLat(), offset.getImageryLon(), offset.getImageryLat())));

        if (offset.date != null) {
            TextView created = (TextView) layout.findViewById(R.id.imagery_offset_date);
            created.setText(offset.date);
        }
        TextView minmax = (TextView) layout.findViewById(R.id.imagery_offset_zoom);
        minmax.setText(main.getString(R.string.min_max_zoom, offset.getMinZoom(), offset.getMaxZoom()));
        dialog.setPositiveButton(R.string.menu_tools_background_align_save_db, createSaveButtonListener(description, author, index, saveOffsetList));
        if (index == (saveOffsetList.size() - 1))
            dialog.setNegativeButton(R.string.cancel, null);
        else
            dialog.setNegativeButton(R.string.next, new OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    AppCompatDialog d = createSaveOffsetDialog(index + 1, saveOffsetList);
                    d.show();
                }
            });
        return dialog.create();
    }

    /**
     * Create an onClick listener that saves the current offset to the offset DB and (if it exists) displays the next
     * offset to be saved
     * 
     * @param description description of the offset in question
     * @param author author
     * @param index index in the list
     * @param saveOffsetList list of offsets to save
     * @return the OnClickListnener
     */
    private OnClickListener createSaveButtonListener(final EditText description, final EditText author, final int index,
            final List<ImageryOffset> saveOffsetList) {

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
                Log.d("Background...", offset.toSaveUrl(offsetServerUri));
                OffsetSaver saver = new OffsetSaver();
                saver.execute(offset);
                try {
                    int result = saver.get();
                    if (result < 0) {
                        error = saver.getError();
                    }
                } catch (InterruptedException | ExecutionException e) {
                    error = e.getMessage();
                }
                if (error != null) {
                    displayError(error);
                    return; // don't continue is something went wrong
                }

                if (index < (saveOffsetList.size() - 1)) {
                    // save retyping if it stays the same
                    saveOffsetList.get(index + 1).description = offset.description;
                    saveOffsetList.get(index + 1).author = offset.author;
                    AppCompatDialog d = createSaveOffsetDialog(index + 1, saveOffsetList);
                    d.show();
                }
            }
        };
    }

    /**
     * Create a dialog for a single offset that asks if it should be applied
     * 
     * @param index position in the list of offsets
     * @return the Dialog
     */
    @SuppressLint("InflateParams")
    private AppCompatDialog createDisplayOffsetDialog(final int index) {
        // Create some useful objects
        final ViewBox bbox = map.getViewBox();
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
        off.setText(main.getString(R.string.distance_meter,
                GeoMath.haversineDistance(offset.getLon(), offset.getLat(), offset.getImageryLon(), offset.getImageryLat())));
        if (offset.date != null) {
            TextView created = (TextView) layout.findViewById(R.id.imagery_offset_date);
            created.setText(offset.date);
        }
        TextView minmax = (TextView) layout.findViewById(R.id.imagery_offset_zoom);
        minmax.setText(main.getString(R.string.min_max_zoom, offset.getMinZoom(), offset.getMaxZoom()));
        TextView distance = (TextView) layout.findViewById(R.id.imagery_offset_distance);
        distance.setText(main.getString(R.string.distance_km,
                GeoMath.haversineDistance((bbox.getLeft() + bbox.getWidth() / 2D) / 1E7d, bbox.getCenterLat(), offset.getLon(), offset.getLat()) / 1000));
        dialog.setPositiveButton(R.string.apply, new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                osmts.setOffset(map.getZoomLevel(), offset.getLon() - offset.getImageryLon(), offset.getLat() - offset.getImageryLat());
                map.invalidate();
            }
        });
        dialog.setNeutralButton(R.string.menu_tools_background_align_apply2all, new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                osmts.setOffset(offset.getMinZoom(), offset.getMaxZoom(), offset.getLon() - offset.getImageryLon(), offset.getLat() - offset.getImageryLat());
                map.invalidate();
            }
        });
        if (index == (offsetList.size() - 1))
            dialog.setNegativeButton(R.string.cancel, null);
        else
            dialog.setNegativeButton(R.string.next, new OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    AppCompatDialog d = createDisplayOffsetDialog(index + 1);
                    d.show();
                }
            });
        return dialog.create();
    }
}
