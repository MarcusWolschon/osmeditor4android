package de.blau.android.tasks;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import android.content.Context;
import android.graphics.Canvas;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.blau.android.R;
import de.blau.android.osm.BoundingBox;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Relation;
import de.blau.android.osm.ViewBox;
import de.blau.android.osm.Way;
import de.blau.android.util.GeoMath;
import de.blau.android.util.Geometry;
import de.blau.android.util.collections.LongPrimitiveList;

/**
 * A todo, loosely base on the OSMOSE format // NOSONAR
 * 
 * @author Simon Poole
 */
public final class Todo extends Bug implements Serializable {
    private static final long serialVersionUID = 3L;

    private static final String DEBUG_TAG = Todo.class.getSimpleName();

    protected static final String TODOS          = "todos";
    private static final String   TODO_STATE     = "state";
    private static final String   TODO_ID        = "id";
    private static final String   TODO_LON       = "lon";
    private static final String   TODO_LAT       = "lat";
    private static final String   TODO_LIST_NAME = "name";

    private String list;

    /**
     * Parse an InputStream containing todos in JSON format
     * 
     * @param is the InputStream
     * @return a List of CustomBugs
     * @throws IOException for JSON reading issues
     * @throws NumberFormatException if a number conversion fails
     */
    public static List<Todo> parseTodos(InputStream is) throws IOException, NumberFormatException {
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
                            case OSM_IDS:
                                parseIds(reader, todo);
                                break;
                            default:
                                reader.skipValue();
                            }
                        }
                        reader.endObject();
                        result.add(todo);
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
     * Default constructor
     */
    private Todo() {
        open();
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
        this();
        list = listName;
        id = java.util.UUID.randomUUID().toString();
        LongPrimitiveList elementList = new LongPrimitiveList();
        elementList.add(element.getOsmId());
        switch (element.getName()) {
        case Node.NAME:
            nodes = elementList;
            lat = ((Node) element).getLat();
            lon = ((Node) element).getLon();
            break;
        case Way.NAME:
            ways = elementList;
            double[] center = Geometry.centroidLonLat((Way) element);
            if (center != null) {
                lon = (int) Math.round(center[0] * 1E7D);
                lat = (int) Math.round(center[1] * 1E7D);
            }
            break;
        case Relation.NAME:
            relations = elementList;
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

    @Override
    public String getDescription() {
        return "Todo: " + list;
    }

    @Override
    public String getDescription(@NonNull Context context) {
        return context.getString(R.string.todo_description, getListName(context));
    }

    @Override
    public String getLongDescription(@NonNull Context context, boolean withElements) {
        String title = getTitle();
        if (title != null && !"".equals(title)) {
            return context.getString(R.string.todo_list_and_comment, getListName(context), title);
        } else {
            return context.getString(R.string.todo_list, getListName(context));
        }
    }

    /**
     * Get the list name for this issue
     * 
     * @param context an Android Context
     * @return a name
     */
    @NonNull
    public String getListName(@NonNull Context context) {
        return list != null && !"".equals(list) ? list : context.getString(R.string.default_);
    }

    @Override
    public String bugFilterKey() {
        return "TODO";
    }

    @Override
    public boolean canBeUploaded() {
        return false;
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
     * Generate a JSON representation this element
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
        final double curLat = getLat() / 1E7D;
        final double curLon = getLon() / 1E7D;
        Collections.sort(todos, (Todo t1, Todo t2) -> Double.compare(GeoMath.haversineDistance(curLon, curLat, t1.getLon() / 1E7D, t1.getLat() / 1E7D),
                GeoMath.haversineDistance(curLon, curLat, t2.getLon() / 1E7D, t2.getLat() / 1E7D)));
        return todos.get(0);
    }

    @Override
    public void drawBitmapOpen(Context context, Canvas c, float x, float y, boolean selected) {
        drawIcon(context, cachedIconOpen, c, R.drawable.todo_open, x, y, selected);
    }

    @Override
    public void drawBitmapChanged(Context context, Canvas c, float x, float y, boolean selected) {
        drawIcon(context, cachedIconChanged, c, R.drawable.todo_open, x, y, selected);
    }

    @Override
    public void drawBitmapChangedClosed(Context context, Canvas c, float x, float y, boolean selected) {
        drawIcon(context, cachedIconChangedClosed, c, R.drawable.todo_closed, x, y, selected);
    }

    @Override
    public void drawBitmapClosed(Context context, Canvas c, float x, float y, boolean selected) {
        drawIcon(context, cachedIconClosed, c, R.drawable.todo_closed, x, y, selected);
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
        if (id == null) {
            if (other.id != null) {
                return false;
            }
        } else if (!id.equals(other.id)) {
            return false;
        }
        return true;
    }
}
