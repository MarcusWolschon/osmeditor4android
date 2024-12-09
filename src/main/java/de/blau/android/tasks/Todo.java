package de.blau.android.tasks;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;

import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.Point;
import com.mapbox.turf.TurfMeasurement;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.blau.android.R;
import de.blau.android.javascript.Utils;
import de.blau.android.osm.BoundingBox;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Relation;
import de.blau.android.osm.ViewBox;
import de.blau.android.osm.Way;
import de.blau.android.util.GeoJSONConstants;
import de.blau.android.util.Geometry;
import de.blau.android.util.StringWithDescription;
import de.blau.android.util.collections.LongPrimitiveList;

/**
 * A todo, loosely base on the OSMOSE format // NOSONAR
 * 
 * @author Simon Poole
 */
public final class Todo extends Bug implements Serializable {
    private static final long serialVersionUID = 4L;

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, Todo.class.getSimpleName().length());
    private static final String DEBUG_TAG = Todo.class.getSimpleName().substring(0, TAG_LEN);

    static final String FILTER_KEY = "TODO";

    protected static final String TODOS          = "todos";
    private static final String   TODO_STATE     = "state";
    private static final String   TODO_ID        = "id";
    private static final String   TODO_LON       = "lon";
    private static final String   TODO_LAT       = "lat";
    private static final String   TODO_COMMENT   = "comment";
    private static final String   TODO_LIST_NAME = "name";
    public static final String    DEFAULT_LIST   = "default";

    private String list = DEFAULT_LIST;

    private static BitmapWithOffset cachedIconTodoClosed;
    private static BitmapWithOffset cachedIconTodoChangedClosed;
    private static BitmapWithOffset cachedIconTodoOpen;
    private static BitmapWithOffset cachedIconTogoChanged;

    /**
     * Setup the icon caches
     * 
     * @param context android Context
     * @param hwAccelerated true if the Canvas is hw accelerated
     */
    public static void setupIconCache(@NonNull Context context, boolean hwAccelerated) {
        cachedIconTodoOpen = getIcon(context, R.drawable.todo_open, hwAccelerated);
        cachedIconTogoChanged = getIcon(context, R.drawable.todo_skipped, hwAccelerated);
        cachedIconTodoChangedClosed = getIcon(context, R.drawable.todo_closed, hwAccelerated);
        cachedIconTodoClosed = getIcon(context, R.drawable.todo_closed, hwAccelerated);
    }

    /**
     * Parse an InputStream containing todos in JSON format
     * 
     * @param is the InputStream
     * @return a List of CustomBugs
     * @throws IOException for JSON reading issues
     * @throws NumberFormatException if a number conversion fails
     */
    public static List<Todo> parseTodos(@NonNull InputStream is) throws IOException, NumberFormatException {
        List<Todo> result = new ArrayList<>();
        try (JsonReader reader = new JsonReader(new InputStreamReader(is))) {
            // key object
            String listName = "";
            String key = null;
            reader.beginObject();
            while (reader.hasNext()) {
                key = reader.nextName(); //
                if (TODO_LIST_NAME.equals(key)) {
                    listName = reader.nextString();
                } else if (TODOS.equals(key)) {
                    reader.beginArray();
                    while (reader.hasNext()) {
                        result.add(parseTodo(reader, listName));
                    }
                    reader.endArray();
                } else {
                    reader.skipValue();
                }
            }
            reader.endObject();
        } catch (IOException | IllegalStateException | NumberFormatException ex) {
            Log.d(DEBUG_TAG, "Parse error, ignoring " + ex);
        }
        return result;
    }

    /**
     * Parse a single todo object
     * 
     * @param reader the current JsonReader
     * @param listName optional list name
     * @return a new Todo
     * @throws IOException on errors reading the JSON
     */
    private static Todo parseTodo(@NonNull JsonReader reader, @Nullable String listName) throws IOException {
        Todo todo = new Todo();
        todo.list = listName;
        reader.beginObject();
        while (reader.hasNext()) {
            String jsonName = reader.nextName();
            switch (jsonName) {
            case TODO_LAT:
                todo.lat = (int) (reader.nextDouble() * 1E7D);
                break;
            case TODO_LON:
                todo.lon = (int) (reader.nextDouble() * 1E7D);
                break;
            case TODO_ID:
                todo.id = reader.nextString();
                break;
            case TODO_STATE:
                todo.setState(State.valueOf(reader.nextString()));
                break;
            case TODO_COMMENT:
                todo.setTitle(reader.nextString());
                break;
            case OSM_IDS:
                parseIds(reader, todo);
                break;
            default:
                reader.skipValue();
            }
        }
        reader.endObject();
        return todo;
    }

    /**
     * Default constructor
     */
    private Todo() {
        open();
    }

    /**
     * Construct a todo with just a list name and id //NOSONAR
     * 
     * @param listName the name of the todo list to add this to // NOSONAR
     * 
     */
    private Todo(@Nullable String listName) {
        this();
        list = listName;
        id = java.util.UUID.randomUUID().toString();
    }

    /**
     * Construct a new Todo from an OsmElement //NOSONAR
     * 
     * If a centroid cannot be determined the item will end up in null island
     * 
     * @param listName the name of the todo list to add this to // NOSONAR
     * @param element the OsmElement
     */
    public Todo(@Nullable String listName, @NonNull OsmElement element) {
        this(listName);
        LongPrimitiveList elementList = new LongPrimitiveList();
        elementList.add(element.getOsmId());
        switch (element.getName()) {
        case Node.NAME:
            setNodes(elementList);
            lat = ((Node) element).getLat();
            lon = ((Node) element).getLon();
            break;
        case Way.NAME:
            setWays(elementList);
            double[] center = Geometry.centroidLonLat((Way) element);
            if (center.length == 2) {
                lon = (int) Math.round(center[0] * 1E7D);
                lat = (int) Math.round(center[1] * 1E7D);
            }
            break;
        case Relation.NAME:
            setRelations(elementList);
            BoundingBox box = element.getBounds();
            if (box != null) {
                ViewBox viewBox = new ViewBox(element.getBounds());
                center = viewBox.getCenter();
                lon = (int) Math.round(center[0] * 1E7D);
                lat = (int) Math.round(center[1] * 1E7D);
            }
            break;
        default:
            throw new IllegalArgumentException("Unknown element " + element);
        }
    }

    /**
     * Construct a new Todo from an GeoJson Feature //NOSONAR
     * 
     * If a centroid cannot be determined the item will end up in null island
     * 
     * @param context an Android context
     * @param listName the name of the todo list to add this to // NOSONAR
     * @param feature the GeoJson Feature
     * @param script optional conversion script if null a default conversion will be performed
     */
    public Todo(@NonNull Context context, @Nullable String listName, @NonNull Feature feature, @Nullable String script) {
        this(listName);
        if (script != null) {
            Utils.evalString(context, script, script, feature, this);
            return;
        }
        StringBuilder content = new StringBuilder();
        final JsonObject properties = feature.properties();
        if (properties != null) {
            for (Entry<String, com.google.gson.JsonElement> property : properties.entrySet()) {
                content.append(property.getKey() + " " + property.getValue().getAsString() + "<BR>");
            }
        }
        setTitle(content.toString());
        final com.mapbox.geojson.Geometry geometry = TurfMeasurement.center(feature).geometry();
        if (geometry == null) {
            throw new IllegalArgumentException("Cannot determine center for  " + feature.toString());
        }
        if (GeoJSONConstants.POINT.equals(geometry.type())) {
            lat = (int) (((Point) geometry).latitude() * 1E7D);
            lon = (int) (((Point) geometry).longitude() * 1E7D);
            return;
        }
        throw new IllegalArgumentException("Unknown / unsupported geometry type for center  " + geometry.toString());
    }

    @Override
    public String getDescription() {
        return "Todo: " + list;
    }

    @Override
    public String getDescription(@NonNull Context context) {
        return context.getString(R.string.todo_description, getListName(context).getValue());
    }

    @Override
    public String getLongDescription(@NonNull Context context, boolean withElements) {
        String title = getTitle();
        if (title != null && !"".equals(title)) {
            return context.getString(R.string.todo_list_and_comment, getListName(context).getValue(), title);
        } else {
            return context.getString(R.string.todo_list, getListName(context).getValue());
        }
    }

    /**
     * Get the list name for this issue
     * 
     * @return a name
     */
    @NonNull
    public String getListName() {
        return list != null ? list : "";
    }

    /**
     * Get the list name for this issue
     * 
     * @param context an Android Context
     * @return a name
     */
    @NonNull
    public StringWithDescription getListName(@NonNull Context context) {
        return list != null && !"".equals(list) ? new StringWithDescription(list)
                : new StringWithDescription(DEFAULT_LIST, context.getString(R.string.default_));
    }

    @Override
    public String bugFilterKey() {
        return FILTER_KEY;
    }

    @Override
    public boolean canBeUploaded() {
        // this is not true, but allows the "upload" button to be enabled
        return true;
    }

    /**
     * Generate a descriptive JSON header
     * 
     * @param writer the JsonWriter
     * @param name the name of the todo list // NOSONAR
     * @throws IOException if writing Json fails
     */
    public static void headerToJSON(@NonNull JsonWriter writer, @NonNull String name) throws IOException {
        writer.name(TODO_LIST_NAME);
        writer.value(name);
    }

    /**
     * Generate a JSON representation for this element
     * 
     * @param writer the JsonWriter
     * @throws IOException if writing Json fails
     */
    public void toJSON(@NonNull JsonWriter writer) throws IOException {
        writer.beginObject();
        writer.name(TODO_LAT);
        writer.value(lat / 1E7D);
        writer.name(TODO_LON);
        writer.value(lon / 1E7D);
        writer.name(TODO_ID);
        writer.value(id);
        writer.name(TODO_STATE);
        writer.value(getState().toString());
        final String title = getTitle();
        if (title != null) {
            writer.name(TODO_COMMENT);
            writer.value(title);
        }
        writer.name(OSM_IDS);
        writer.beginObject();
        writeIdArray(writer, nodes, NODES_ARRAY);
        writeIdArray(writer, ways, WAYS_ARRAY);
        writeIdArray(writer, relations, RELATIONS_ARRAY);
        writer.endObject();
        writer.endObject();
    }

    /**
     * Write a Json array of ids
     * 
     * @param writer the JsonWriter
     * @param array the array of ids to write out
     * @param arrayName the name of the array
     * @throws IOException if writing Json fails
     */
    private void writeIdArray(@NonNull JsonWriter writer, @Nullable LongPrimitiveList array, @NonNull String arrayName) throws IOException {
        if (array != null) {
            writer.name(arrayName);
            writer.beginArray();
            for (long e : array.values()) {
                writer.value(e);
            }
            writer.endArray();
        }
    }

    /**
     * Get the geographically nearest Todo from a List // NOSONAR
     * 
     * @param todos List of Todos
     * @return the nearest Todo // NOSONAR
     */
    @NonNull
    public Todo getNearest(@NonNull List<Todo> todos) {
        todos.remove(this); // this would always be the nearest
        int count = todos.size();
        if (count >= 1) {
            final double curLat = getLat() / 1E7D;
            final double curLon = getLon() / 1E7D;
            sortByDistance(todos, curLon, curLat, true);
            return todos.get(0);
        }
        return this; // the only thing we can return
    }

    @Override
    public boolean hasBeenChanged() {
        return !isClosed() && getState() == State.SKIPPED;
    }

    @Override
    public void drawBitmapOpen(Canvas c, float x, float y, boolean selected, Paint paint) {
        drawIcon(cachedIconTodoOpen, c, x, y, selected, paint);
    }

    @Override
    public void drawBitmapChanged(Canvas c, float x, float y, boolean selected, Paint paint) {
        drawIcon(cachedIconTogoChanged, c, x, y, selected, paint);
    }

    @Override
    public void drawBitmapChangedClosed(Canvas c, float x, float y, boolean selected, Paint paint) {
        drawIcon(cachedIconTodoChangedClosed, c, x, y, selected, paint);
    }

    @Override
    public void drawBitmapClosed(Canvas c, float x, float y, boolean selected, Paint paint) {
        drawIcon(cachedIconTodoClosed, c, x, y, selected, paint);
    }

    /**
     * Set a new List to hold OSM element ids for Nodes
     * 
     * @param nodes the List
     */
    public void setNodes(@Nullable LongPrimitiveList nodes) {
        this.nodes = nodes;
    }

    /**
     * Set a new List to hold OSM element ids for Ways
     * 
     * @param nodes the List
     */
    public void setWays(@Nullable LongPrimitiveList ways) {
        this.ways = ways;
    }

    /**
     * Set a new List to hold OSM element ids for Relations
     * 
     * @param nodes the List
     */
    public void setRelations(@Nullable LongPrimitiveList relations) {
        this.relations = relations;
    }

    /**
     * Set the latitude in WGS84*1E7 degrees for the Todo // NOSONAR
     * 
     * @param lat the latitude in WGS84*1E7 degrees
     */
    public void setLat(int lat) {
        this.lat = lat;
    }

    /**
     * Set the longitude in WGS84*1E7 degrees for the Todo // NOSONAR
     * 
     * @param lat the longitude in WGS84*1E7 degrees
     */
    public void setLon(int lon) {
        this.lon = lon;
    }

    @Override
    public boolean equals(Object obj) { // NOSONAR
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Todo)) {
            return false;
        }
        Todo other = (Todo) obj;
        return Objects.equals(id, other.id);
    }
}