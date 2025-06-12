package de.blau.android.layer;

import java.io.InputStream;

import android.content.Context;
import androidx.annotation.NonNull;
import de.blau.android.contract.FileExtensions;
import de.blau.android.util.Hash;

/**
 * StyleableLayer that is loaded from a file
 * 
 * @author simon
 *
 */
public abstract class StyleableFileLayer extends StyleableLayer {

    private static final long serialVersionUID = 1L;

    /**
     * State file file name
     */
    protected String stateFileName;

    protected String contentId; // could potentially be transient

    protected StyleableFileLayer(@NonNull String contentId, String defaultStateFileName) {
        this.contentId = contentId;
        this.stateFileName = defaultStateFileName;
    }

    /**
     * Check if we have a state file
     * 
     * @param context an Android Context
     * @return true if a state file exists
     */
    protected boolean hasStateFile(@NonNull Context context) {
        if (stateFileName == null) {
            return false;
        }
        try (InputStream stream = context.openFileInput(stateFileName)) {
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    /**
     * Set the name of the state file
     * 
     * This needs to be unique across all instances so best an encoded uri, to avoid filename length issues we use the
     * SHA-256 hash
     * 
     * @param baseName the base name for this specific instance
     */
    protected void setStateFileName(@NonNull String baseName) {
        stateFileName = Hash.sha256(baseName) + "." + FileExtensions.RES;
    }
}
