package de.blau.android.util.mvt.style;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.LargeTest;
import ch.poole.android.sprites.Sprites;
import de.blau.android.resources.DataStyle;

@RunWith(RobolectricTestRunner.class)
@LargeTest
public class StyleTest {

    /**
     * Pre test setup
     */
    @Before
    public void setup() {
        // default values are currently take from the data style
        DataStyle.getStylesFromFiles(ApplicationProvider.getApplicationContext());
    }

    /**
     * Load a style and check if we got what we expected
     */
    @Test
    public void parseMapboxStyleTest() {
        Style style = new Style();
        final Context ctx = ApplicationProvider.getApplicationContext();
        float density = ctx.getResources().getDisplayMetrics().density;
        style.loadStyle(ctx, getClass().getResourceAsStream("/osm-liberty.json"));
        Sprites sprite = new Sprites(ctx, getClass().getResourceAsStream("/osm-liberty-sprite.json"),
                getClass().getResourceAsStream("/osm-liberty-sprite.png"));
        style.setSprites(sprite);
        final List<Layer> layers = style.getLayers();
        assertEquals(102, layers.size());
        Fill buildingLayer = (Fill) getLayer("building", layers);
        assertNotNull(buildingLayer);
        // "stops": [[13, "hsla(35, 6%, 79%, 0.32)"], [14, "hsl(35, 6%, 79%)"]]
        buildingLayer.onZoomChange(style, null, 13);
        assertNotNull(buildingLayer.outline);
        assertEquals(Color.parseColor("hsla(35, 6%, 79%, 0.32)"), buildingLayer.outline.getColor());
        buildingLayer.onZoomChange(style, null, 15);
        assertEquals(Color.parseColor("hsl(35, 6%, 79%)"), buildingLayer.outline.getColor());
        // "line-width": {"base": 1.4, "stops": [[14.5, 0], [15, 3], [20, 8]]}
        Line bridgeHatching = (Line) getLayer("bridge_transit_rail_hatching", layers);
        bridgeHatching.onZoomChange(style, null, 14);
        assertEquals(0, bridgeHatching.getStrokeWidth(), 0.01);
        bridgeHatching.onZoomChange(style, null, 16);
        double interpolated = Layer.interpolation(1.4, 15, 3, 20, 8, 16);
        assertEquals(interpolated * density, bridgeHatching.getStrokeWidth(), 0.01);
        bridgeHatching.onZoomChange(style, null, 20);
        assertEquals(8 * density, bridgeHatching.getStrokeWidth(), 0.01);
        // "fill-translate": { "stops": [[15,[0,0]],[16,[-2,-2]]],"base": 1}
        Fill buildingTop = (Fill) getLayer("building_top", layers);
        buildingTop.onZoomChange(style, null, 14);
        assertEquals(0, buildingTop.fillTranslate.literal[0], 0.01);
        assertEquals(0, buildingTop.fillTranslate.literal[1], 0.01);
        buildingTop.onZoomChange(style, null, 17);
        assertEquals(-2 * density, buildingTop.fillTranslate.literal[0], 0.01);
        assertEquals(-2 * density, buildingTop.fillTranslate.literal[1], 0.01);
        // "text-size": {"base": 1,"stops": [[13,12],[14,13]]}
        Symbol roadLabel = (Symbol) getLayer("road_label", layers);
        roadLabel.onZoomChange(style, null, 12);
        assertEquals(12 * density, roadLabel.getLabelPaint().getTextSize(), 0.01);
        roadLabel.onZoomChange(style, null, 14);
        assertEquals(13 * density, roadLabel.getLabelPaint().getTextSize(), 0.01);
        // "symbol-placement": { "base": 1, "stops": [[10,"point"],[11,"line"]]}
        Symbol roadShield = (Symbol) getLayer("road_shield", layers);
        roadShield.onZoomChange(style, null, 9);
        assertEquals(Symbol.SYMBOL_PLACEMENT_POINT, roadShield.getSymbolPlacement());
        roadShield.onZoomChange(style, null, 12);
        assertEquals(Symbol.SYMBOL_PLACEMENT_LINE, roadShield.getSymbolPlacement());
    }

    /**
     * Load a style with a circle layer and check if we got what we expected
     */
    @Test
    public void styleCircleTest() {
        Style style = new Style();
        final Context ctx = ApplicationProvider.getApplicationContext();
        float density = ctx.getResources().getDisplayMetrics().density;
        style.loadStyle(ctx, getClass().getResourceAsStream("/rob.json"));
        final List<Layer> layers = style.getLayers();
        Circle something = (Circle) getLayer("something", layers);
        assertNotNull(something);
        assertEquals(6 * density, something.circleRadius.literal, 0.01);
        assertEquals(2 * density, something.stroke.getStrokeWidth(), 0.01);
    }

    /**
     * Get a layer by id
     * 
     * @param id the id
     * @param layers the list of layers
     * @return the Layer or null
     */
    @Nullable
    private Layer getLayer(@NonNull String id, @NonNull List<Layer> layers) {
        for (Layer layer : layers) {
            if (id.equals(layer.getId())) {
                return layer;
            }
        }
        return null;
    }
}
