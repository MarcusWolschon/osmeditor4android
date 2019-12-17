package de.blau.android.services.util;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

import android.annotation.TargetApi;
import android.location.GpsStatus.NmeaListener;
import android.location.OnNmeaMessageListener;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.util.Log;
import de.blau.android.services.TrackerService;
import de.blau.android.util.SavingHelper;

@SuppressWarnings("deprecation")
public class NmeaTcpClient implements Runnable {

    private static final String DEBUG_TAG = "NmeaTcpClient";
    String                      host;
    int                         port;
    boolean                     canceled  = false;
    final NmeaListener          oldListener;
    final OnNmeaMessageListener newListener;
    final Handler               handler;

    /**
     * Create an instance of a client that will read lines from a socket until it is stopped
     * 
     * @param hostAndPort host:port to connect to
     * @param oldListener the listener that will be called with the input
     * @param handler for sending messages to caller
     */
    public NmeaTcpClient(@NonNull String hostAndPort, @NonNull NmeaListener oldListener, @NonNull Handler handler) {
        setHostAndPort(hostAndPort);
        this.oldListener = oldListener;
        this.newListener = null;
        this.handler = handler;
    }

    /**
     * Extract the host and port from the config string FIXME support IPv6
     * 
     * @param hostAndPort host:port to connect to
     */
    private void setHostAndPort(@NonNull String hostAndPort) {
        int doubleColon = hostAndPort.indexOf(':');
        if (doubleColon > 0) {
            try {
                host = hostAndPort.substring(0, doubleColon);
                port = Integer.parseInt(hostAndPort.substring(doubleColon + 1));
            } catch (NumberFormatException e) {
                reportError(e);
                cancel();
            }
        } // otherwise crash and burn?
    }

    /**
     * Create an instance of a client that will read lines from a socket until it is stopped
     * 
     * @param hostAndPort host:port to connect to
     * @param newListener the listener that will be called with the input
     * @param handler for sending messages to caller
     */
    public NmeaTcpClient(@NonNull String hostAndPort, @NonNull OnNmeaMessageListener newListener, @NonNull Handler handler) {
        setHostAndPort(hostAndPort);
        this.newListener = newListener;
        this.oldListener = null;
        this.handler = handler;
    }

    /**
     * Stop reading input and exit
     */
    public void cancel() {
        canceled = true;
    }

    @Override
    @TargetApi(19)
    public void run() {
        boolean useOldListener = newListener == null;
        Socket socket = null;
        OutputStreamWriter osw = null;
        BufferedReader input = null;
        try {
            Log.d(DEBUG_TAG, "Connecting to " + host + ":" + port + " ...");

            socket = new Socket(host, port);
            osw = new OutputStreamWriter(socket.getOutputStream());

            InputStreamReader isr = new InputStreamReader(socket.getInputStream());
            input = new BufferedReader(isr);
            String firstLine = input.readLine();
            // gpsd message is json {"class":"VERSION","release":"3.17","rev":"3.17","proto_major":3,"proto_minor":12}
            if (firstLine.contains("\"VERSION\"")) { // HACKALERT assume this is not NMEA and try to switch gpsd to NMEA
                                                     // output
                Log.d(DEBUG_TAG, "gpsd detected, trying to switch to NMEA output");
                osw.write("?WATCH={\"enable\":true,\"nmea\":true};\r\n");
                osw.flush();
                // skip stuff we don't need from gpsd
                input.readLine();
                input.readLine();
            } else {
                if (useOldListener) {
                    oldListener.onNmeaReceived(-1, firstLine);
                } else {
                    newListener.onNmeaMessage(firstLine, -1);
                }
            }

            if (useOldListener) {
                while (!canceled) {
                    oldListener.onNmeaReceived(-1, input.readLine());
                }
            } else {
                while (!canceled) {
                    newListener.onNmeaMessage(input.readLine(), -1);
                }
            }
        } catch (Exception e) {
            Log.e(DEBUG_TAG, "failed to open/read " + host + ":" + port + " " + e.getMessage());
            reportError(e);
        } catch (Error e) {
            reportError(e);
        } finally {
            SavingHelper.close(osw);
            // see https://code.google.com/p/android/issues/detail?id=62909
            // fow why we can't use helper methods here
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    Log.e(DEBUG_TAG, "Closing socket threw " + e.getMessage());
                }
            }
            SavingHelper.close(input);
        }
    }
    

    /**
     * Send the message from an exception
     * 
     * @param e the Exception
     */
    private void reportError(@NonNull Throwable e) {
        Message failed = handler.obtainMessage(TrackerService.CONNECTION_FAILED, e.getMessage());
        failed.sendToTarget();
    }
}
