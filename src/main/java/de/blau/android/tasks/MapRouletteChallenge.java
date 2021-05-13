package de.blau.android.tasks;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;

import com.google.gson.stream.JsonReader;

import android.util.Log;
import androidx.annotation.Nullable;

public class MapRouletteChallenge implements Serializable {

    private static final long serialVersionUID = 2L;

    private static final String DEBUG_TAG = "MapRouletteChallenge";

    String blurb;
    String instruction;
    String description;

    /**
     * Parse an InputStream containing data for a MapRoulette Challenge
     * 
     * @param is the InputString
     * @return a MapRouletteTask.Challenge or null
     * @throws IOException for JSON reading issues
     * @throws NumberFormatException if a number conversion fails
     */
    @Nullable
    public static MapRouletteChallenge parseChallenge(InputStream is) throws IOException, NumberFormatException {

        try (JsonReader reader = new JsonReader(new InputStreamReader(is));) {
            reader.beginObject();
            MapRouletteChallenge challenge = new MapRouletteChallenge();
            while (reader.hasNext()) {
                switch (reader.nextName()) {
                case "blurb":
                    challenge.blurb = reader.nextString();
                    break;
                case "instruction":
                    challenge.instruction = reader.nextString();
                    break;
                case "description":
                    challenge.description = reader.nextString();
                    break;
                default:
                    reader.skipValue();
                }
            }
            reader.endObject();
            return challenge;
        } catch (IOException | IllegalStateException ex) {
            Log.d(DEBUG_TAG, "Ignoring " + ex);
        } 
        return null;
    }
}
