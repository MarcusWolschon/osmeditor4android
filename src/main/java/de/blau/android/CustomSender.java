package de.blau.android;

import static de.blau.android.contract.FileExtensions.JSON;
import static de.blau.android.contract.Paths.DIRECTORY_PATH_CRASHES;

import java.io.File;
import java.io.FileWriter;

import org.acra.config.CoreConfiguration;
import org.acra.data.CrashReportData;
import org.acra.sender.HttpSender;
import org.acra.sender.ReportSenderException;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;
import de.blau.android.util.FileUtil;

public class CustomSender extends HttpSender {
    private static final String DEBUG_TAG = CustomSender.class.getCanonicalName();

    private static final String ACRA_REPORT_ID        = "REPORT_ID";
    public static final String  ACRA_SAVE_LOCALLY_KEY = "acra.save_locally";

    public CustomSender(@NonNull CoreConfiguration config) {
        super(config, null, null);
    }

    @Override
    public void send(@NonNull Context context, @NonNull CrashReportData report) throws ReportSenderException {
        try {
            // Leverage ACRA's native HTTP/Network sending mechanism
            super.send(context, report);
            if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean(ACRA_SAVE_LOCALLY_KEY, false)) {
                saveToPublicStorage(report);
            }
        } catch (ReportSenderException e) {
            // Network transmission failed; save the backup copy locally
            saveToPublicStorage(report);
            // Re-throw so ACRA scheduler knows it failed and handles retries later
            throw e;
        }
    }

    /**
     * Save report to local file
     * 
     * @param report the report to save
     */
    private void saveToPublicStorage(@NonNull CrashReportData report) {
        try {
            File publicDir = new File(FileUtil.getPublicDirectory(), DIRECTORY_PATH_CRASHES);
            if (!publicDir.exists()) {
                publicDir.mkdirs();
            }

            File crashFile = new File(publicDir, report.get(ACRA_REPORT_ID) + "." + JSON);
            if (!crashFile.exists()) {
                try (FileWriter writer = new FileWriter(crashFile)) {
                    writer.write(report.toJSON());
                }
            }
        } catch (Exception ex) {
            Log.e(DEBUG_TAG, ex.getMessage());
        }
    }

}
