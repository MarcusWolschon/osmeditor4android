package de.blau.android.dialogs;

import java.util.Random;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.appcompat.app.AppCompatDialog;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import de.blau.android.App;
import de.blau.android.R;
import de.blau.android.layer.tiles.MapTilesLayer;
import de.blau.android.layer.tiles.util.MapTileProvider.BitmapDecoder;
import de.blau.android.listener.DoNothingListener;
import de.blau.android.osm.BoundingBox;
import de.blau.android.resources.TileLayerSource;
import de.blau.android.resources.TileLayerSource.TileType;
import de.blau.android.services.util.MapTile;
import de.blau.android.services.util.MapTileTester;
import de.blau.android.util.ExecutorTask;
import de.blau.android.util.ImmersiveDialogFragment;
import de.blau.android.util.ThemeUtils;

/**
 * Try to retrieve a tile for a layer
 * 
 * @author simon
 *
 */
public class TileSourceDiagnostics extends ImmersiveDialogFragment {

    private static final String DEBUG_TAG = TileSourceDiagnostics.class.getSimpleName().substring(0,
            Math.min(23, TileSourceDiagnostics.class.getSimpleName().length()));

    private static final String TAG = "fragment_tile_source_diag";

    private static final String TILE_KEY = "tile";

    private MapTile tile;

    /**
     * Show an info dialog for the results of trying to retrieve a random tile in the provided BoundingBox and at the
     * zoomLevel.
     * 
     * Note that the zoom level is bounded by the may and min zoom from souce.
     * 
     * @param activity the calling Activity
     * @param source the TileLayerSource
     * @param zoomLevel zoom level
     * @param box the BoundingBox in which the tile should be
     */
    public static void showDialog(@NonNull FragmentActivity activity, @NonNull TileLayerSource source, int zoomLevel, @NonNull BoundingBox box) {
        int zoom = Math.max(source.getMinZoom(), Math.min(source.getMaxZoom(), zoomLevel));
        int n = 1 << zoom;
        int tileLeft = MapTilesLayer.tileLeft(box.getLeft(), 0, n);
        int tileRight = MapTilesLayer.tileRight(box.getRight(), 0, n);
        int tileTop = MapTilesLayer.tileTop(box.getTop(), 0, n);
        int tileBottom = MapTilesLayer.tileBottom(box.getBottom(), 0, n);
        Random random = App.getRandom();
        MapTile tile = new MapTile(source.getId(), zoom, random.nextInt((tileRight - tileLeft) + 1) + tileLeft,
                random.nextInt((tileBottom - tileTop) + 1) + tileTop);
        showDialog(activity, tile);
    }

    /**
     * Show an info dialog for the results of trying to retrieve a tile
     * 
     * @param activity the calling Activity
     * @param tile the tile to retrieve
     */
    public static void showDialog(@NonNull FragmentActivity activity, @NonNull MapTile tile) {
        dismissDialog(activity);
        try {
            FragmentManager fm = activity.getSupportFragmentManager();
            TileSourceDiagnostics fragment = newInstance(tile);
            fragment.show(fm, TAG);
        } catch (IllegalStateException isex) {
            Log.e(DEBUG_TAG, "showDialog", isex);
        }
    }

    /**
     * Dismiss the dialog
     * 
     * @param activity the calling Activity
     */
    private static void dismissDialog(@NonNull FragmentActivity activity) {
        de.blau.android.dialogs.Util.dismissDialog(activity, TAG);
    }

    /**
     * Create a new instance of the FeatureInfo dialog
     * 
     * @param feature Feature to display the info on
     * @param titleRes resource id for the title
     * @return an instance of ElementInfo
     */
    @NonNull
    private static TileSourceDiagnostics newInstance(@NonNull MapTile tile) {
        TileSourceDiagnostics f = new TileSourceDiagnostics();

        Bundle args = new Bundle();
        args.putSerializable(TILE_KEY, tile);

        f.setArguments(args);
        f.setShowsDialog(true);

        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            tile = de.blau.android.util.Util.getSerializeable(savedInstanceState, TILE_KEY, MapTile.class);
        } else {
            tile = de.blau.android.util.Util.getSerializeable(getArguments(), TILE_KEY, MapTile.class);
        }
    }

    @Override
    public AppCompatDialog onCreateDialog(Bundle savedInstanceState) {
        Log.d(DEBUG_TAG, "onCreateDialog " + tile);
        final Builder builder = new AlertDialog.Builder(getActivity());
        DoNothingListener doNothingListener = new DoNothingListener();
        builder.setPositiveButton(R.string.done, doNothingListener);

        builder.setTitle(R.string.layer_diag_title);
        LayoutInflater inflater = ThemeUtils.getLayoutInflater(getContext());
        final ScrollView sv = (ScrollView) inflater.inflate(R.layout.tile_diag_view, null, false);
        final TextView status = (TextView) sv.findViewById(R.id.diag_status_text);
        final TextView text = (TextView) sv.findViewById(R.id.diag_text);
        text.setTextIsSelectable(true);
        final ImageView tileView = (ImageView) sv.findViewById(R.id.diag_image);
        final TextView tileText = (TextView) sv.findViewById(R.id.diag_tile_text);
        final MapTileTester tester = new MapTileTester(getContext(), tile);
        new ExecutorTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void param) {
                boolean result = tester.run();
                text.postDelayed(() -> {
                    status.setText(result ? R.string.tile_success : R.string.tile_fail);
                    text.setText(tester.getOutput());
                    byte[] image = tester.getTile();
                    if (result && tester.getTileType() == TileType.BITMAP && image != null && image.length > 0) {
                        BitmapDecoder decoder = new BitmapDecoder(false);
                        Bitmap bitmap = decoder.decode(image, false);
                        if (bitmap != null) {
                            tileView.setVisibility(View.VISIBLE);
                            tileView.setImageBitmap(bitmap);
                        } else {
                            tileText.setVisibility(View.VISIBLE);
                            tileText.setText(R.string.tile_decoding_failed);
                        }
                    }
                }, 50);
                return null;
            }
        }.execute();
        builder.setView(sv);
        return builder.create();
    }
}
