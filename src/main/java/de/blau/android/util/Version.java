package de.blau.android.util;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;
import de.blau.android.R;
import de.blau.android.contract.Files;

public class Version {

    private static final String DEBUG_TAG = "Version";
    private int                 major     = 0;
    private int                 minor     = 0;
    private int                 patch     = 0;
    private int                 beta      = 0;

    String               lastVersion;
    SavingHelper<String> savingHelperVersion;
    Context              ctx;

    /**
     * Determine various version related things
     * 
     * @param ctx Android Context
     */
    public Version(@NonNull Context ctx) {
        this.ctx = ctx;
        savingHelperVersion = new SavingHelper<>();
        lastVersion = savingHelperVersion.load(ctx, Files.VERSION, false);
        String currentVersion = ctx.getString(R.string.app_version);
        parse(currentVersion);
    }

    /**
     * Construct a Version from a String
     * 
     * @param version the version string
     */
    public Version(@NonNull String version) {
        parse(version);
    }

    /**
     * Parser a semver String
     * 
     * @param v the version string
     */
    private void parse(@NonNull String v) {
        String[] numbers = v.split("\\.", 4);

        if (numbers.length < 3) {
            Log.e(DEBUG_TAG, "Invalid version string " + v);
            return;
        }
        try {
            major = Integer.parseInt(numbers[0]);
            minor = Integer.parseInt(numbers[1]);
            patch = Integer.parseInt(numbers[2]);
            if (numbers.length == 4) {
                beta = Integer.parseInt(numbers[3]);
            }
        } catch (NumberFormatException nfe) {
            Log.e(DEBUG_TAG, "Invalid version string " + v);
        }
    }

    /**
     * Check if this is a new install
     * 
     * @return true if a new install
     */
    public boolean isNewInstall() {
        return lastVersion == null || "".equals(lastVersion);
    }

    /**
     * Check if this is a new version
     * 
     * @return true if a new version or a new install
     */
    public boolean isNewVersion() {
        if (isNewInstall()) {
            return false;
        }
        Version last = new Version(lastVersion);
        return last.major != major || (last.major == major && last.minor != minor);
    }

    /**
     * Save the current version to a file
     */
    public void save() {
        savingHelperVersion.save(ctx, Files.VERSION, ctx.getString(R.string.app_version), false);
    }

    /**
     * @return the major version
     */
    public int getMajor() {
        return major;
    }

    /**
     * @return the minor version
     */
    public int getMinor() {
        return minor;
    }

    /**
     * @return the patch version
     */
    public int getPatch() {
        return patch;
    }

    /**
     * @return the beta version
     */
    public int getBeta() {
        return beta;
    }
}
