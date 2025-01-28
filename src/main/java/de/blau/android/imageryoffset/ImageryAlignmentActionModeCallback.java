package de.blau.android.imageryoffset;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

import android.annotation.SuppressLint;
import android.content.DialogInterface.OnClickListener;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.text.DynamicLayout;
import android.text.Layout;
import android.text.SpannableStringBuilder;
import android.text.TextPaint;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewStub;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.appcompat.app.AppCompatDialog;
import androidx.appcompat.view.ActionMode;
import androidx.appcompat.view.ActionMode.Callback;
import androidx.appcompat.widget.ActionMenuView;
import de.blau.android.App;
import de.blau.android.HelpViewer;
import de.blau.android.Logic;
import de.blau.android.Main;
import de.blau.android.Map;
import de.blau.android.Mode;
import de.blau.android.PostAsyncActionHandler;
import de.blau.android.R;
import de.blau.android.contract.Schemes;
import de.blau.android.dialogs.Progress;
import de.blau.android.easyedit.EasyEditActionModeCallback;
import de.blau.android.imageryoffset.ImageryOffset.DeprecationNote;
import de.blau.android.osm.Server;
import de.blau.android.osm.UserDetails;
import de.blau.android.osm.ViewBox;
import de.blau.android.prefs.Preferences;
import de.blau.android.resources.DataStyle;
import de.blau.android.resources.TileLayerSource;
import de.blau.android.util.Density;
import de.blau.android.util.ExecutorTask;
import de.blau.android.util.GeoMath;
import de.blau.android.util.MenuUtil;
import de.blau.android.util.SavingHelper;
import de.blau.android.util.ScreenMessage;
import de.blau.android.util.SerializableState;
import de.blau.android.util.ThemeUtils;
import de.blau.android.views.layers.MapTilesLayer;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * This is an ActionMode for aligning the background imagery
 * 
 * @author simon
 *
 */
public class ImageryAlignmentActionModeCallback implements Callback {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, ImageryAlignmentActionModeCallback.class.getSimpleName().length());
    private static final String DEBUG_TAG = ImageryAlignmentActionModeCallback.class.getSimpleName().substring(0, TAG_LEN);

    private static final int MENUITEM_QUERYDB   = 1;
    private static final int MENUITEM_APPLY2ALL = 2;
    private static final int MENUITEM_RESET     = 3;
    private static final int MENUITEM_ZERO      = 4;
    private static final int MENUITEM_SAVE2DB   = 5;
    private static final int MENUITEM_HELP      = 6;

    private static final String LAYER_ID_KEY = "layerId";
    private static final String OLD_MODE_KEY = "oldMode";
    private static final String OFFSETS_KEY  = "offsets";
    private static final String FILENAME     = ImageryAlignmentActionModeCallback.class.getSimpleName() + ".res";

    private static SavingHelper<SerializableState> savingHelper = new SavingHelper<>();

    private final Mode        oldMode;
    private final Preferences prefs;
    private final Uri         offsetServerUri;

    private final Offset[] oldOffsets;

    private final TileLayerSource osmts;
    private final Map             map;
    private final Main            main;

    private List<ImageryOffset> offsetList;

    private ActionMenuView cabBottomBar;

    private Drawable   savedButtonDrawable;
    private boolean    savedButtonEnabled;
    private ActionMode actionMode;

    private boolean isService;

    private final DynamicLayout          zoomAndOffsetLayout;
    private final SpannableStringBuilder zoomAndOffsetText;

    /**
     * Construct a new BackgroundAlignmentActionModeCallback
     * 
     * @param main the current instance of Main
     * @param oldMode the Mode before we were called
     * @param layerId the id for the "layer" we want to adjust
     */
    public ImageryAlignmentActionModeCallback(@NonNull Main main, @NonNull Mode oldMode, @NonNull String layerId) {
        this(main, oldMode, layerId, null);
    }

    /**
     * Construct a new callback from saved state
     * 
     * @param main the current instance of Main
     * @param state the saved state
     */
    public ImageryAlignmentActionModeCallback(@NonNull Main main, @NonNull SerializableState state) {
        this(main, (Mode) state.getSerializable(OLD_MODE_KEY), state.getString(LAYER_ID_KEY), state.getList(OFFSETS_KEY));
    }

    /**
     * Actually construct an instance
     * 
     * @param main the current instance of Main
     * @param oldMode the Mode before we were called
     * @param layerId the id for the "layer" we want to adjust
     * @param offsetList the current (original) list of Offsets
     */
    private ImageryAlignmentActionModeCallback(@NonNull Main main, @Nullable Mode oldMode, @Nullable String layerId, @Nullable List<Offset> offsetList) {
        this.main = main;
        this.oldMode = oldMode;

        prefs = App.getPreferences(main);
        String offsetServer = prefs.getOffsetServer();
        offsetServerUri = Uri.parse(offsetServer);
        map = main.getMap();
        if (layerId == null) {
            throw new IllegalStateException("Layer id is null");
        }
        MapTilesLayer<?> layer = (MapTilesLayer<?>) map.getLayer(layerId);
        if (layer == null) {
            throw new IllegalStateException("MapTilesLayer is null");
        }
        osmts = layer.getTileLayerConfiguration();
        Offset[] offsets = offsetList == null ? osmts.getOffsets() : offsetList.toArray(new Offset[0]);

        oldOffsets = copy(offsets);
        isService = osmts.getTileUrl().startsWith(Schemes.HTTP) || osmts.getTileUrl().startsWith(Schemes.HTTPS);

        zoomAndOffsetText = new SpannableStringBuilder();
        zoomAndOffsetLayout = new DynamicLayout(zoomAndOffsetText, zoomAndOffsetText,
                new TextPaint(map.getDataStyle().getInternal(DataStyle.LABELTEXT_NORMAL).getPaint()),
                map.getWidth() - 2 * (int) Density.dpToPx(main, de.blau.android.layer.grid.MapOverlay.DISTANCE2SIDE_DP),
                map.rtlLayout() ? Layout.Alignment.ALIGN_OPPOSITE : Layout.Alignment.ALIGN_NORMAL, 1.0f, 0f, true);
        setOffset(map.getZoomLevel(), 0, 0);
    }

    /**
     * Deep copy an Offset array
     * 
     * @param offsets the offsets to copy
     * @return a deep copy of the Offset array
     */
    @NonNull
    private Offset[] copy(@NonNull Offset[] offsets) {
        final int length = offsets.length;
        Offset[] copy = new Offset[length];
        for (int i = 0; i < length; i++) {
            if (offsets[i] != null) {
                copy[i] = new Offset(offsets[i]);
            }
        }
        return copy;
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        actionMode = mode;
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
        FloatingActionButton button = main.getSimpleActionsButton();
        button.setOnClickListener(v -> saveAndFinish());
        savedButtonDrawable = button.getDrawable();
        savedButtonEnabled = button.isEnabled();
        button.setImageResource(R.drawable.ic_done_white_36dp);
        main.enableSimpleActionsButton();
        if (!prefs.areSimpleActionsEnabled()) {
            main.showSimpleActionsButton();
        }
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        if (cabBottomBar != null) {
            menu = cabBottomBar.getMenu();
            cabBottomBar.setOnMenuItemClickListener(item -> onActionItemClicked(actionMode, item));
            MenuUtil.setupBottomBar(main, cabBottomBar, main.isFullScreen(), prefs.lightThemeEnabled());
        }
        menu.clear();
        menu.add(Menu.NONE, MENUITEM_QUERYDB, Menu.NONE, R.string.menu_tools_background_align_retrieve_from_db)
                .setEnabled(main.isConnectedOrConnecting() && isService).setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_download))
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        menu.add(Menu.NONE, MENUITEM_RESET, Menu.NONE, R.string.menu_tools_background_align_reset)
                .setIcon(ThemeUtils.getResIdFromAttribute(main, R.attr.menu_undo)).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        menu.add(Menu.NONE, MENUITEM_ZERO, Menu.NONE, R.string.menu_tools_background_align_zero);
        menu.add(Menu.NONE, MENUITEM_APPLY2ALL, Menu.NONE, R.string.menu_tools_background_align_apply2all);
        menu.add(Menu.NONE, MENUITEM_SAVE2DB, Menu.NONE, R.string.menu_tools_background_align_save_db).setEnabled(main.isConnectedOrConnecting() && isService);
        menu.add(Menu.NONE, MENUITEM_HELP, Menu.NONE, R.string.menu_help);
        View close = EasyEditActionModeCallback.getActionCloseView(mode);
        if (close != null) {
            close.setOnClickListener(v -> close());
        }
        return true;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        switch (item.getItemId()) {
        case MENUITEM_ZERO:
            final int zoomLevel = map.getZoomLevel();
            osmts.setOffset(zoomLevel, 0.0d, 0.0d);
            setOffset(zoomLevel, 0f, 0f);
            map.invalidate();
            break;
        case MENUITEM_RESET:
            reset();
            break;
        case MENUITEM_APPLY2ALL:
            Offset o = osmts.getOffset(map.getZoomLevel());
            if (o != null) {
                osmts.setOffset(o.getDeltaLon(), o.getDeltaLat());
            } else {
                osmts.setOffset(0.0d, 0.0d);
            }
            break;
        case MENUITEM_QUERYDB:
            getOffsetFromDB();
            break;
        case MENUITEM_SAVE2DB:
            saveOffsetsToDB();
            break;
        case MENUITEM_HELP:
            HelpViewer.start(main, R.string.help_aligningbackgroundiamgery);
            return true;
        default:
            return false;
        }
        return true;
    }

    /**
     * Reset to the original offsets
     */
    private void reset() {
        osmts.setOffsets(copy(oldOffsets));
        final int zoomLevel = map.getZoomLevel();
        Offset o = osmts.getOffset(zoomLevel);
        setOffset(zoomLevel, o != null ? (float) o.getDeltaLon() : 0f, o != null ? (float) o.getDeltaLat() : 0f);
        map.invalidate();
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        if (cabBottomBar != null) {
            cabBottomBar.setVisibility(View.GONE);
        }
        Logic logic = App.getLogic();
        main.showBottomBar();
        logic.setMode(main, oldMode);
        main.setImageryAlignmentActionModeCallback(null);
        main.showLock();
        main.showLayersControl();
        FloatingActionButton button = main.getSimpleActionsButton();
        button.setImageDrawable(savedButtonDrawable);
        main.setSimpleActionsButtonListener();
        if (!savedButtonEnabled) {
            main.disableSimpleActionsButton();
        }
        if (!prefs.areSimpleActionsEnabled()) {
            main.hideSimpleActionsButton();
        }
    }

    /**
     * Modify the behaviour of the "done"/"close" button
     */
    public void close() {
        Log.d(DEBUG_TAG, "close");
        if (!Arrays.equals(osmts.getOffsets(), oldOffsets)) {
            new AlertDialog.Builder(main).setTitle(R.string.abort_action_title).setPositiveButton(R.string.yes, (dialog, which) -> {
                reset();
                actionMode.finish();
            }).setNeutralButton(R.string.cancel, null).show();
            return;
        }
        actionMode.finish();
    }

    /**
     * Save the offsets to the imagery configuration and exit
     * 
     * @return true
     */
    private boolean saveAndFinish() {
        Logic logic = App.getLogic();
        new ExecutorTask<Void, Void, Void>(logic.getExecutorService(), logic.getHandler()) {
            @Override
            protected Void doInBackground(Void param) {
                Log.i(DEBUG_TAG, "Saving offsets");
                List<ImageryOffset> offsets = ImageryOffsetUtils.offsets2ImageryOffset(osmts, map.getViewBox(), null);
                try (ImageryOffsetDatabase db = new ImageryOffsetDatabase(main); SQLiteDatabase writableDb = db.getWritableDatabase()) {
                    ImageryOffsetDatabase.deleteOffset(writableDb, osmts.getImageryOffsetId());
                    for (ImageryOffset im : offsets) {
                        ImageryOffsetDatabase.addOffset(writableDb, im);
                    }
                    return null;
                }
            }
        }.execute();
        actionMode.finish();
        return true;
    }

    /**
     * Download offsets
     * 
     * @author simon
     *
     */
    private class OffsetLoader extends ExecutorTask<Double[], Void, List<ImageryOffset>> {

        private static final String GET              = "get";
        private static final String DEPRECATED_FIELD = "deprecated";
        private static final String MAX_ZOOM_FIELD   = "max-zoom";
        private static final String MIN_ZOOM_FIELD   = "min-zoom";
        private static final String ID_FIELD         = "id";
        private static final String TYPE_FIELD       = "type";
        private static final String DATE_FIELD       = "date";
        private static final String REASON_FIELD     = "reason";
        private static final String ERROR_FIELD      = "error";
        private static final String JSON_VALUE       = "json";
        private static final String FORMAT_PARAM     = "format";
        private static final String RADIUS_PARAM     = "radius";

        private String                       error = null;
        private final PostAsyncActionHandler handler;

        /**
         * Construct a new OffsetLoader
         * 
         * @param executorService ExecutorService to run this on
         * @param uiHandler Handler to use
         * @param postLoadHandler a handler to call after loading or null
         */
        OffsetLoader(@NonNull ExecutorService executorService, @NonNull Handler uiHandler, @Nullable final PostAsyncActionHandler postLoadHandler) {
            super(executorService, uiHandler);
            handler = postLoadHandler;
        }

        /**
         * Parse an ImageryOffset from Json input
         * 
         * @param reader the JsonReader
         * @return an ImageryOffset or null if parsing failed
         * @throws IOException if reading JSON failed
         */
        @Nullable
        private ImageryOffset readOffset(@NonNull JsonReader reader) throws IOException {
            String type = null;
            ImageryOffset result = new ImageryOffset();

            reader.beginObject();
            while (reader.hasNext()) {
                String jsonName = reader.nextName();
                switch (jsonName) {
                case TYPE_FIELD:
                    type = reader.nextString();
                    break;
                case ID_FIELD:
                    result.id = reader.nextLong();
                    break;
                case ImageryOffset.LAT_PARAM:
                    result.setLat(reader.nextDouble());
                    break;
                case ImageryOffset.LON_PARAM:
                    result.setLon(reader.nextDouble());
                    break;
                case ImageryOffset.AUTHOR_PARAM:
                    result.author = reader.nextString();
                    break;
                case DATE_FIELD:
                    result.date = reader.nextString();
                    break;
                case ImageryOffset.IMAGERY_PARAM:
                    result.imageryId = reader.nextString();
                    break;
                case ImageryOffset.IMLAT_PARAM:
                    result.setImageryLat(reader.nextDouble());
                    break;
                case ImageryOffset.IMLON_PARAM:
                    result.setImageryLon(reader.nextDouble());
                    break;
                case MIN_ZOOM_FIELD:
                    result.setMinZoom(reader.nextInt());
                    break;
                case MAX_ZOOM_FIELD:
                    result.setMaxZoom(reader.nextInt());
                    break;
                case ImageryOffset.DESCRIPTION_PARAM:
                    result.description = reader.nextString();
                    break;
                case DEPRECATED_FIELD:
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

        /**
         * Parse depreciation information from Json input
         * 
         * @param reader the JsonReader
         * @return an ImagerOffset or null if parsing failed
         * @throws IOException if reading JSON failed
         */
        @NonNull
        private DeprecationNote readDeprecated(@NonNull JsonReader reader) throws IOException {
            DeprecationNote result = new DeprecationNote();

            reader.beginObject();
            while (reader.hasNext()) {
                String jsonName = reader.nextName();
                switch (jsonName) {
                case ImageryOffset.AUTHOR_PARAM:
                    result.author = reader.nextString();
                    break;
                case REASON_FIELD:
                    result.reason = reader.nextString();
                    break;
                case DATE_FIELD:
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
         * Get the offsets around the specified coordinates
         * 
         * @param lat latitude in WGS84 coordinates
         * @param lon longitude in WGS84 coordinates
         * @param radius radius around the location in fm
         * @return a List containing the found ImageryOffsets or null
         */
        @NonNull
        List<ImageryOffset> getOffsetList(double lat, double lon, int radius) {
            Uri.Builder uriBuilder = offsetServerUri.buildUpon().appendPath(GET).appendQueryParameter(ImageryOffset.LAT_PARAM, String.valueOf(lat))
                    .appendQueryParameter(ImageryOffset.LON_PARAM, String.valueOf(lon));
            if (radius > 0) {
                uriBuilder.appendQueryParameter(RADIUS_PARAM, String.valueOf(radius));
            }
            uriBuilder.appendQueryParameter(ImageryOffset.IMAGERY_PARAM, osmts.getImageryOffsetId()).appendQueryParameter(FORMAT_PARAM, JSON_VALUE);

            String urlString = uriBuilder.build().toString();
            List<ImageryOffset> result = new ArrayList<>();
            try {
                Log.d(DEBUG_TAG, "urlString " + urlString);

                Request request = new Request.Builder().url(urlString).build();
                OkHttpClient client = App.getHttpClient().newBuilder().connectTimeout(Server.TIMEOUT, TimeUnit.MILLISECONDS)
                        .readTimeout(Server.TIMEOUT, TimeUnit.MILLISECONDS).build();
                Call offsetListCall = client.newCall(request);
                Response offestListCallResponse = offsetListCall.execute();
                if (offestListCallResponse.isSuccessful()) {
                    try (ResponseBody responseBody = offestListCallResponse.body();
                            JsonReader reader = new JsonReader(new InputStreamReader(responseBody.byteStream()))) {
                        JsonToken token = reader.peek();
                        if (token.equals(JsonToken.BEGIN_ARRAY)) {
                            reader.beginArray();
                            while (reader.hasNext()) {
                                ImageryOffset imOffset = readOffset(reader);
                                if (imOffset != null && imOffset.deprecated == null) { // deprecated offsets are ignored
                                    result.add(imOffset);
                                }
                            }
                            reader.endArray();
                        } else if (token.equals(JsonToken.BEGIN_OBJECT)) {
                            reader.beginObject();
                            while (reader.hasNext()) {
                                String jsonName = reader.nextName();
                                if (ERROR_FIELD.equals(jsonName)) {
                                    error = reader.nextString();
                                } else {
                                    reader.skipValue();
                                }
                            }
                        } // can't happen ?
                    }
                } else {
                    Server.throwOsmServerException(offestListCallResponse);
                }
            } catch (IOException | IllegalStateException e) {
                error = e.getMessage();
            }
            if (error != null) {
                Log.d(DEBUG_TAG, "search error " + error);
            }
            return result;
        }

        @Override
        protected void onPreExecute() {
            Progress.showDialog(main, Progress.PROGRESS_SEARCHING);
        }

        @Override
        protected List<ImageryOffset> doInBackground(Double[] params) {
            if (params.length != 3) {
                throw new IllegalArgumentException("wrong number of params in OffsetLoader " + params.length);
            }
            double centerLat = params[0];
            double centerLon = params[1];
            int radius = (int) (params[2] == null ? 0 : params[2]);
            List<ImageryOffset> result = getOffsetList(centerLat, centerLon, radius);
            if (result.isEmpty()) {
                // retry with max radius
                Log.d(DEBUG_TAG, "retrying search with max radius");
                result = getOffsetList(centerLat, centerLon, 0);
            }
            return result;
        }

        @Override
        protected void onPostExecute(List<ImageryOffset> res) {
            Progress.dismissDialog(main, Progress.PROGRESS_SEARCHING);
            offsetList = res;
            if (error == null) {
                if (handler != null) {
                    handler.onSuccess();
                }
            } else {
                ScreenMessage.toastTopError(main, main.getString(R.string.toast_imagery_offset_download_failed, error));
            }
        }

        /**
         * Get a String describing any error that occurred
         * 
         * @return the error String or null if none
         */
        @Nullable
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
    private class OffsetSaver extends ExecutorTask<ImageryOffset, Void, Integer> {
        private String error = null;

        /**
         * Create a new OffsetSaver
         * 
         * @param executorService ExecutorService to run this on
         * @param handler an Handler
         */
        OffsetSaver(@NonNull ExecutorService executorService, @NonNull Handler handler) {
            super(executorService, handler);
        }

        @Override
        protected void onPreExecute() {
            Progress.showDialog(main, Progress.PROGRESS_SAVING);
        }

        @Override
        protected Integer doInBackground(ImageryOffset offset) {

            try {
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
                ScreenMessage.barInfo(main, R.string.toast_save_done);
            } else {
                ScreenMessage.barError(main, R.string.toast_save_failed);
            }
        }

        /**
         * Get a String describing any error that occurred
         * 
         * @return the error String or null if none
         */
        @Nullable
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
        final double centerLon = (bbox.getLeft() + bbox.getWidth() / 2D) / 1E7d;
        final Comparator<ImageryOffset> cmp = (offset1, offset2) -> {
            double d1 = GeoMath.haversineDistance(centerLon, centerLat, offset1.getLon(), offset1.getLat());
            double d2 = GeoMath.haversineDistance(centerLon, centerLat, offset2.getLon(), offset2.getLat());
            return Double.compare(d1, d2);
        };
        Logic logic = App.getLogic();
        OffsetLoader loader = new OffsetLoader(logic.getExecutorService(), logic.getHandler(), () -> {
            if (!offsetList.isEmpty()) {
                Collections.sort(offsetList, cmp);
                AppCompatDialog d = createDisplayOffsetDialog(0);
                d.show();
            } else {
                displayError(main.getString(R.string.imagery_offset_not_found));
            }
        });

        double hm = GeoMath.haversineDistance(centerLon, bbox.getBottom() / 1E7d, centerLon, bbox.getTop() / 1E7d);
        double wm = GeoMath.haversineDistance(bbox.getLeft() / 1E7d, centerLat, bbox.getRight() / 1E7d, centerLat);
        int radius = (int) Math.max(1, Math.round(Math.min(hm, wm) / 2000d)); // convert to km and make it at least 1
                                                                              // and /2 for radius
        loader.execute(new Double[] { centerLat, centerLon, (double) radius });
    }

    /**
     * Save current offset to imagery offset database server
     */
    private void saveOffsetsToDB() {

        String author = null;
        String error = null;

        // try to find current display name
        final Server server = prefs.getServer();
        if (!server.needOAuthHandshake()) {
            Logic logic = App.getLogic();
            ExecutorTask<Void, Void, UserDetails> loader = new ExecutorTask<Void, Void, UserDetails>(logic.getExecutorService(), logic.getHandler()) {

                @Override
                protected UserDetails doInBackground(Void param) {
                    return server.getUserDetails();
                }
            };
            try {
                loader.execute();
                UserDetails user = loader.get(10, TimeUnit.SECONDS);

                if (user != null) {
                    author = user.getDisplayName();
                } else {
                    author = server.getDisplayName(); // maybe it has been configured
                }
            } catch (InterruptedException | ExecutionException e) { // NOSONAR cancel does interrupt the thread in
                                                                    // question
                loader.cancel();
                error = e.getMessage();
            } catch (TimeoutException e) {
                error = main.getString(R.string.toast_timeout);
            }
            displayError(error);
        } else {
            author = server.getDisplayName(); // maybe it has been configured
        }

        List<ImageryOffset> offsetsToSaveList = ImageryOffsetUtils.offsets2ImageryOffset(osmts, map.getViewBox(), author);

        if (!offsetsToSaveList.isEmpty()) {
            AppCompatDialog d = createSaveOffsetDialog(0, offsetsToSaveList);
            d.show();
        }
    }

    /**
     * Show an AlertDialog for the error if any
     * 
     * @param error the error String or null
     */
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
     * Create the imagery offset display/apply dialog
     * 
     * @param index index in to the list of ImageryOffset objects
     * @param saveOffsetList the list of ImageryOffset objects
     * @return a Dialog
     */
    @SuppressLint("InflateParams")
    @NonNull
    private AppCompatDialog createSaveOffsetDialog(final int index, @NonNull final List<ImageryOffset> saveOffsetList) {
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
        if (index == (saveOffsetList.size() - 1)) {
            dialog.setNegativeButton(R.string.cancel, null);
        } else {
            dialog.setNegativeButton(R.string.next, (dia, which) -> {
                AppCompatDialog d = createSaveOffsetDialog(index + 1, saveOffsetList);
                d.show();
            });
        }
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
    @NonNull
    private OnClickListener createSaveButtonListener(@NonNull final EditText description, @NonNull final EditText author, final int index,
            @NonNull final List<ImageryOffset> saveOffsetList) {
        return (dialog, which) -> {
            String error = null;
            ImageryOffset offset = saveOffsetList.get(index);
            if (offset == null) {
                return;
            }
            offset.description = description.getText().toString();
            offset.author = author.getText().toString();
            offset.imageryId = osmts.getImageryOffsetId();
            Log.d("Background...", offset.toSaveUrl(offsetServerUri));
            Logic logic = App.getLogic();
            OffsetSaver saver = new OffsetSaver(logic.getExecutorService(), logic.getHandler());
            saver.execute(offset);
            try {
                int result = saver.get();
                if (result < 0) {
                    error = saver.getError();
                }
            } catch (InterruptedException | ExecutionException e) { // NOSONAR cancel does interrupt the thread in
                                                                    // question
                saver.cancel();
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
        dialog.setPositiveButton(R.string.apply, (d, which) -> {
            osmts.setOffset(map.getZoomLevel(), offset.getLon() - offset.getImageryLon(), offset.getLat() - offset.getImageryLat());
            map.invalidate();
        });
        dialog.setNeutralButton(R.string.menu_tools_background_align_apply2all, (d, which) -> {
            osmts.setOffset(offset.getMinZoom(), offset.getMaxZoom(), offset.getLon() - offset.getImageryLon(), offset.getLat() - offset.getImageryLat());
            map.invalidate();
        });
        if (index == (offsetList.size() - 1)) {
            dialog.setNegativeButton(R.string.cancel, null);
        } else {
            dialog.setNegativeButton(R.string.next, (dia, which) -> {
                AppCompatDialog d = createDisplayOffsetDialog(index + 1);
                d.show();
            });
        }
        return dialog.create();
    }

    /**
     * Converts screen-coords to gps-coords and offsets background layer.
     * 
     * @param zoomLevel the zoom level
     * @param screenTransX Movement on the screen.
     * @param screenTransY Movement on the screen.
     */
    public void setOffset(final int zoomLevel, final float screenTransX, final float screenTransY) {
        int height = map.getHeight();
        Logic logic = App.getLogic();
        int lon = logic.xToLonE7(screenTransX);
        int lat = logic.yToLatE7(height - screenTransY);
        ViewBox viewBox = logic.getViewBox();
        int relativeLon = lon - viewBox.getLeft();
        int relativeLat = lat - viewBox.getBottom();

        double lonOffset = 0d;
        double latOffset = 0d;
        Offset o = osmts.getOffset(zoomLevel);
        final boolean hasOffset = o != null;
        if (hasOffset) {
            lonOffset = o.getDeltaLon();
            latOffset = o.getDeltaLat();
        }
        lonOffset = lonOffset - relativeLon / 1E7d;
        latOffset = latOffset - relativeLat / 1E7d;
        osmts.setOffset(zoomLevel, lonOffset, latOffset);
        double[] center = viewBox.getCenter();
        zoomAndOffsetText.replace(0, zoomAndOffsetText.length(),
                main.getString(R.string.zoom_and_offsets, zoomLevel, String.format(Locale.US, "%.7f", lonOffset), String.format(Locale.US, "%.7f", latOffset),
                        String.format(Locale.US, "%.2f", GeoMath.haversineDistance(center[0], center[1], center[0] + lonOffset, center[1])),
                        String.format(Locale.US, "%.2f", GeoMath.haversineDistance(center[0], center[1], center[0], center[1] + latOffset))));

    }

    /**
     * @return the zoomAndOffsetLayout
     */
    @NonNull
    public DynamicLayout getZoomAndOffsetLayout() {
        return zoomAndOffsetLayout;
    }

    /**
     * Save any state that is needed to restart
     */
    public void saveState() {
        SerializableState state = new SerializableState();
        state.putSerializable(OLD_MODE_KEY, oldMode);
        state.putString(LAYER_ID_KEY, osmts.getId());
        state.putList(OFFSETS_KEY, Arrays.asList(oldOffsets));
        savingHelper.save(main, FILENAME, state, false, true);
    }

    /**
     * Restart from saved state
     * 
     * @param main current instance of main
     */
    public static void restart(@NonNull Main main) {
        new ExecutorTask<Void, Void, SerializableState>() {
            @Override
            protected SerializableState doInBackground(Void param) {
                return savingHelper.load(main, FILENAME, false, true, true);
            }

            @Override
            protected void onPostExecute(SerializableState state) {
                if (state != null) {
                    ImageryAlignmentActionModeCallback callback = new ImageryAlignmentActionModeCallback(main, state);
                    main.startSupportActionMode(callback);
                    main.setImageryAlignmentActionModeCallback(callback);
                    return;
                }
                Log.e(DEBUG_TAG, "restart, saved state is null");
                App.getLogic().setMode(main, Mode.MODE_EASYEDIT);
            }
        }.execute();
    }
}
