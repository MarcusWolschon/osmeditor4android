package io.vespucci.util;

import java.io.Serializable;
import java.util.Objects;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import io.vespucci.R;
import io.vespucci.contract.Files;

public class Version implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final String DEBUG_TAG = Version.class.getSimpleName().substring(0, Math.min(23, Version.class.getSimpleName().length()));

    private int major = 0;
    private int minor = 0;
    private int patch = 0;
    private int beta  = -1;

    transient String               lastVersion;
    transient SavingHelper<String> savingHelperVersion;

    /**
     * Determine various version related things
     * 
     * @param ctx Android Context
     */
    public Version(@NonNull Context ctx) {
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
            String[] temp = new String[3];
            Log.w(DEBUG_TAG, "Trying to fix short version string " + v);
            temp[0] = numbers[0];
            temp[2] = "0";
            switch (numbers.length) {
            case 1:
                temp[1] = "0";
                break;
            case 2:
                temp[1] = numbers[1];
                break;
            case 0:
            default:
                return;
            }
            numbers = temp;
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
     * 
     * @param ctx an Android Context
     */
    public void save(@NonNull Context ctx) {
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

    /**
     * Checks if a version is larger or equals than one provided as a String
     * 
     * Ignores beta values
     * 
     * @param versionStr the version to compare to
     * @return true if larger or equals
     */
    public boolean largerThanOrEqual(@NonNull String versionStr) {
        Version v = new Version(versionStr);
        return major > v.getMajor() || (major == v.getMajor() && minor >= v.getMinor())
                || (major == v.getMajor() && minor == v.getMinor() && patch >= v.getPatch());
    }

    @Override
    public String toString() {
        return major + "." + minor + "." + patch + (beta >= 0 ? "." + beta : "");
    }

    @Override
    public int hashCode() {
        return Objects.hash(beta, major, minor, patch);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Version)) {
            return false;
        }
        Version other = (Version) obj;
        return beta == other.beta && major == other.major && minor == other.minor && patch == other.patch;
    }
}
