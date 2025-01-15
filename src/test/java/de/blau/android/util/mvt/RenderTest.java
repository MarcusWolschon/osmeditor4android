package de.blau.android.util.mvt;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.LargeTest;
import de.blau.android.util.mvt.style.Style;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33)
@LargeTest
public class RenderTest {

    /**
     * render openinframap tile
     */
    @Test
    public void renderOpenInfraMapTest() {
        renderTest("/openinframap_tile.pbf", null);
    }

    /**
     * Render tilemaker tile
     */
    @Test
    public void renderTilemakerTest() {
        renderTest("/tilemaker_tile.pbf", "/osm-liberty.json");
    }

    /**
     * Render mapillary tile
     */
    @Test
    public void renderMapillaryTest() {
        renderTest("/mapillary.pbf", "/mapillary-style.json");
    }
    
    /**
     * Render mapillary tile
     */
    @Test
    public void renderMapillaryNoFilterTest() {
        renderTest("/mapillary.pbf", "/mapillary-style-no-filter.json");
    }

    /**
     * 
     */
    private void renderTest(@NonNull String tile, String styleName) {
        try {
            VectorTileDecoder.FeatureIterable decodedTile = new VectorTileDecoder().decode(DecodeTest.readTile(tile));
            Map<String, List<VectorTileDecoder.Feature>> features = decodedTile.asMap();
            Bitmap bitmap = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(bitmap);
            Paint p = new Paint();
            VectorTileRenderer r = new VectorTileRenderer();
            if (styleName != null) {
                Style style = new Style();
                final Context ctx = ApplicationProvider.getApplicationContext();
                style.loadStyle(ctx, getClass().getResourceAsStream(styleName));
                r.setStyle(style);
            }
            Rect rect = new Rect(0, 0, 512, 512);
            long start = System.currentTimeMillis();
            r.preRender(c, 14);
            r.render(c, features, 14, null, rect, p);
            r.postRender(c, 14);
            System.out.println("Rendering 1 took " + (System.currentTimeMillis() - start) + "ms");
            c = new Canvas(bitmap);
            start = System.currentTimeMillis();
            r.preRender(c, 14);
            r.render(c, features, 14, null, rect, p);
            r.postRender(c, 14);
            System.out.println("Rendering 2 took " + (System.currentTimeMillis() - start) + "ms");
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

}