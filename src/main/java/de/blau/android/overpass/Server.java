package de.blau.android.overpass;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import de.blau.android.App;
import de.blau.android.AsyncResult;
import de.blau.android.ErrorCodes;
import de.blau.android.Logic;
import de.blau.android.Main;
import de.blau.android.exception.DataConflictException;
import de.blau.android.exception.OsmException;
import de.blau.android.exception.OsmServerException;
import de.blau.android.exception.StorageException;
import de.blau.android.geocode.QueryNominatim;
import de.blau.android.geocode.Search.SearchResult;
import de.blau.android.osm.BoundingBox;
import de.blau.android.osm.Node;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.OsmParser;
import de.blau.android.osm.Relation;
import de.blau.android.osm.Storage;
import de.blau.android.osm.StorageDelegator;
import de.blau.android.osm.ViewBox;
import de.blau.android.osm.Way;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public final class Server {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, Main.class.getSimpleName().length());
    private static final String DEBUG_TAG = Server.class.getSimpleName().substring(0, TAG_LEN);

    private static final long   TIMEOUT   = 2000;
    private static final String BODY_DATA = "data";

    private static final int BASE_STATE         = 0;
    private static final int CURLY_1_STATE      = 1;
    private static final int CURLY_2_STATE      = 2;
    private static final int CURLY_FINISH_STATE = 3;
    private static final int ARGUMENT_STATE     = 4;

    private static final String TURBO_GEOCODE_AREA = "geocodeArea";
    private static final String TURBO_CENTER       = "center";
    private static final String TURBO_BBOX         = "bbox";

    private static final char CLOSE_BRACKET = '}';
    private static final char OPEN_BRACKET  = '{';
    private static final char DOUBLE_COLON  = ':';

    private static final long OVERPASS_AREA_ID_OFFSET = 3600000000L;

    /**
     * Private constructor to stop instantiation
     */
    private Server() {
        // private
    }

    /**
     * Replace Overpass Turbo placeholders
     * 
     * @param context an Android context
     * @param query the original query
     * @return the query with the place holders replaced or removed
     * @throws OsmException
     */
    @NonNull
    public static String replacePlaceholders(@NonNull Context context, @NonNull String query) {
        StringBuilder builder = new StringBuilder();
        StringBuilder param = new StringBuilder();
        StringBuilder argument = new StringBuilder();

        int state = BASE_STATE;
        for (char c : query.toCharArray()) {
            switch (state) {
            case BASE_STATE:
                if (c == OPEN_BRACKET) {
                    state = CURLY_1_STATE;
                } else {
                    builder.append(c);
                }
                break;
            case CURLY_1_STATE:
                if (c == OPEN_BRACKET) {
                    state = CURLY_2_STATE;
                    param.setLength(0); // reset
                } else if (c == CLOSE_BRACKET) {
                    state = BASE_STATE;
                    builder.append(CLOSE_BRACKET);
                } else {
                    state = BASE_STATE;
                    builder.append(OPEN_BRACKET);
                }
                break;
            case CURLY_2_STATE:
                if (c == CLOSE_BRACKET) {
                    state = CURLY_FINISH_STATE;
                } else if (c == DOUBLE_COLON) {
                    state = ARGUMENT_STATE;
                    argument.setLength(0); // reset
                } else {
                    param.append(c);
                }
                break;
            case ARGUMENT_STATE:
                if (c == CLOSE_BRACKET) {
                    state = CURLY_FINISH_STATE;
                } else {
                    argument.append(c);
                }
                break;
            case CURLY_FINISH_STATE:
                state = BASE_STATE;
                if (c == CLOSE_BRACKET) {
                    placeholderValue(context, builder, param.toString(), argument.toString());
                } else {
                    builder.append("{{");
                    builder.append(param);
                    builder.append(CLOSE_BRACKET);
                }
                break;
            default:
                Log.e(DEBUG_TAG, "Unknown state " + state);
            }
        }
        return builder.toString();
    }

    /**
     * Add the value for the placeholder to the output string
     * 
     * @param context an Android Context
     * @param builder the output StringBuilder
     * @param placeholder the place holder
     * @param argument the argument if any
     */
    private static void placeholderValue(@NonNull Context context, @NonNull StringBuilder builder, @NonNull String placeholder, @NonNull String argument) {
        ViewBox box = App.getLogic().getViewBox();
        switch (placeholder.trim()) {
        case TURBO_BBOX:
            builder.append(coordToStr(box.getBottom()));
            builder.append(',');
            builder.append(coordToStr(box.getLeft()));
            builder.append(',');
            builder.append(coordToStr(box.getTop()));
            builder.append(',');
            builder.append(coordToStr(box.getRight()));
            break;
        case TURBO_CENTER:
            double[] center = box.getCenter();
            builder.append(Double.toString(center[1]));
            builder.append(',');
            builder.append(Double.toString(center[0]));
            break;
        case TURBO_GEOCODE_AREA:
            String url = QueryNominatim.getNominatimUrl(context);
            QueryNominatim querier = new QueryNominatim(null, url, null, false);
            querier.execute(argument);
            try {
                List<SearchResult> results = querier.get(5, TimeUnit.SECONDS);
                if (results != null && !results.isEmpty()) {
                    SearchResult result = results.get(0);
                    builder.append("area(" + Long.toString(result.getOsmId() + OVERPASS_AREA_ID_OFFSET) + ")");
                } // if there was an error QueryNominatim will have toasted
            } catch (InterruptedException | ExecutionException | TimeoutException e) { // NOSONAR
                querier.cancel();
            }
            break;
        default:
            Log.e(DEBUG_TAG, "Unknown place holder " + placeholder);
        }
    }

    /**
     * long coord to string
     * 
     * @param coord the coordinate
     */
    private static String coordToStr(long coord) {
        return Double.toString(coord / 1E7D);
    }

    /**
     * Query the configured Overpass server
     * 
     * If the query is successful the data in storage will have been replaced
     * 
     * @param context an Android Context
     * @param query the query
     * @param merge merge the received data instead of replacing existing data
     * @param select if true select results
     */
    @NonNull
    public static AsyncResult query(@NonNull final Context context, @NonNull String query, boolean merge, boolean select) {
        final String url = App.getPreferences(context).getOverpassServer();
        Log.d(DEBUG_TAG, "querying " + url + " for " + query);
        try {
            Storage storage = execQuery(url, query);
            if (storage.isEmpty()) {
                return new AsyncResult(ErrorCodes.NOT_FOUND);
            }
            final StorageDelegator delegator = App.getDelegator();
            final BoundingBox box = storage.calcBoundingBoxFromData();
            if (merge) {
                delegator.mergeData(storage, (OsmElement e) -> e.hasProblem(context, App.getDefaultValidator(context)));
                delegator.mergeBoundingBox(box);
            } else {
                delegator.reset(false);
                delegator.setCurrentStorage(storage); // this sets dirty flag
                delegator.setOriginalBox(box);
            }
            if (select) {
                selectResult(storage, delegator);
            }
            return new AsyncResult(ErrorCodes.OK);
        } catch (StorageException sex) {
            return new AsyncResult(ErrorCodes.OUT_OF_MEMORY);
        } catch (OsmServerException e) {
            return new AsyncResult(ErrorCodes.UNKNOWN_ERROR, e.getMessage());
        } catch (DataConflictException dce) {
            return new AsyncResult(ErrorCodes.DATA_CONFLICT);
        } catch (IllegalStateException iex) {
            return new AsyncResult(ErrorCodes.CORRUPTED_DATA);
        } catch (OsmException e) {
            return new AsyncResult(ErrorCodes.NOT_FOUND, e.getMessage());
        } catch (SAXException e) {
            return new AsyncResult(ErrorCodes.INVALID_DATA_RECEIVED, e.getMessage());
        } catch (IOException e) {
            return new AsyncResult(ErrorCodes.NO_CONNECTION, e.getMessage());
        }
    }

    /**
     * Select the elements in storage, trying to avoid way nodes
     * 
     * @param storage the original results of the query
     * @param delegator the current StorageDelegator instance containing the merged results
     */
    private static void selectResult(@NonNull final Storage storage, @NonNull final StorageDelegator delegator) {
        Logic logic = App.getLogic();
        logic.deselectAll();
        List<Node> wayNodes = storage.getWayNodes();
        for (Node n : storage.getNodes()) {
            if (n.hasTags() || !wayNodes.contains(n)) {
                logic.addSelectedNode((Node) delegator.getOsmElement(Node.NAME, n.getOsmId()));
            }
        }
        for (Way w : storage.getWays()) {
            logic.addSelectedWay((Way) delegator.getOsmElement(Way.NAME, w.getOsmId()));
        }
        for (Relation r : storage.getRelations()) {
            logic.addSelectedRelation((Relation) delegator.getOsmElement(Relation.NAME, r.getOsmId()));
        }
    }

    /**
     * Query an Overpass server
     * 
     * @param url API url of the server
     * @param query the query
     * @return a Storage object holding the results
     * @throws IOException an IO error
     * @throws SAXException if the input fails parsing
     */
    @NonNull
    private static Storage execQuery(@NonNull String url, @NonNull String query) throws IOException, SAXException {
        Request.Builder requestBuilder = new Request.Builder().url(url);

        RequestBody body = new FormBody.Builder().add(BODY_DATA, query).build();
        Request request = requestBuilder.post(body).build();

        OkHttpClient client = App.getHttpClient().newBuilder().connectTimeout(TIMEOUT, TimeUnit.SECONDS).readTimeout(TIMEOUT, TimeUnit.SECONDS).build();

        Response response = client.newCall(request).execute();
        if (response.isSuccessful()) {
            final OsmParser osmParser = new OsmParser();
            try (ResponseBody responseBody = response.body(); InputStream in = responseBody.byteStream()) {
                osmParser.start(in);
            } catch (ParserConfigurationException pcex) {
                throw new IOException(pcex);
            }
            return osmParser.getStorage();
        }
        throw new OsmServerException(response.code(), de.blau.android.osm.Server.readStream(response.body().byteStream()));
    }
}
