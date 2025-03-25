package io.vespucci.gpx;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import io.vespucci.SignalHandler;;

public class SendNMEA {

    private static final String DEBUG_TAG = "SendNMEA";

    /**
     * Send a file containing NMEA sentences line by line in a thread
     * 
     * @param context Android Context
     * @param fileName name of the file in the test resources
     * @param signalHandler handler to use when we are finished
     */
    public static void send(@NonNull Context context, @NonNull String fileName, @NonNull SignalHandler signalHandler) {
        try {
            Log.d(DEBUG_TAG, "preparing to send");
            SendThread r = new SendThread(context, fileName, signalHandler);

            Thread t = new Thread(null, r, DEBUG_TAG);
            t.start();
        } catch (Exception e) {
            Log.e(DEBUG_TAG, e.getMessage());
        }
    }

    static class SendThread implements Runnable {
        final Context       context;
        final String        fileName;
        final SignalHandler signalHandler;

        /**
         * The thread that does the actual sending
         * 
         * @param context Android Context
         * @param fileName name of the file in the test resources
         * @param signalHandler handler to use when we are finished
         */
        SendThread(@NonNull Context context, @NonNull String fileName, @NonNull SignalHandler signalHandler) {
            this.context = context;
            this.fileName = fileName;
            this.signalHandler = signalHandler;
        }

        @Override
        public void run() {
            Log.d(DEBUG_TAG, "send thread started");
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            try (BufferedReader in = new BufferedReader(new InputStreamReader(loader.getResourceAsStream(fileName)));
                    Socket clientSocket = new Socket("localhost", 1958);
                    BufferedWriter out = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()))) {
                String line = in.readLine();
                while (line != null) {
                    // send line to server
                    out.write(line);
                    out.write("\r\n");
                    out.flush();
                    try {
                        Thread.sleep(50); // NOSONAR
                    } catch (InterruptedException e) { // NOSONAR
                        return;
                    }
                    line = in.readLine();
                }
                Log.d(DEBUG_TAG, "send thread finished");
            } catch (IOException e) {
                Log.e(DEBUG_TAG, "SendThread " + e.getMessage());
                e.printStackTrace();
            }
            signalHandler.onSuccess();
        }
    }
}
