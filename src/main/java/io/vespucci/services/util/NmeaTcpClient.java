package io.vespucci.services.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

import android.annotation.TargetApi;
import android.location.GpsStatus.NmeaListener;
import android.location.OnNmeaMessageListener;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import androidx.annotation.NonNull;
import io.vespucci.services.TrackerService;
import io.vespucci.util.SavingHelper;

@SuppressWarnings("deprecation")
public class NmeaTcpClient implements Runnable {

    private static final String DEBUG_TAG = NmeaTcpClient.class.getSimpleName().substring(0, Math.min(23, NmeaTcpClient.class.getSimpleName().length()));
    String                      host;
    int                         port;
    boolean                     canceled  = false;
    final NmeaListener          oldListener;
    final OnNmeaMessageListener newListener;
    final Handler               handler;
    Socket                      socket    = null;

    /**
     * Create an instance of a client that will read lines from a socket until it is stopped
     * 
     * @param hostAndPort host:port to connect to
     * @param oldListener the listener that will be called with the input
     * @param handler for sending messages to caller
     */
    public NmeaTcpClient(@NonNull String hostAndPort, @NonNull NmeaListener oldListener, @NonNull Handler handler) {
        setHostAndPort(handler, hostAndPort);
        this.oldListener = oldListener;
        this.newListener = null;
        this.handler = handler;
    }

    /**
     * Create an instance of a client that will read lines from a socket until it is stopped
     * 
     * @param hostAndPort host:port to connect to
     * @param newListener the listener that will be called with the input
     * @param handler for sending messages to caller
     */
    public NmeaTcpClient(@NonNull String hostAndPort, @NonNull OnNmeaMessageListener newListener, @NonNull Handler handler) {
        setHostAndPort(handler, hostAndPort);
        this.newListener = newListener;
        this.oldListener = null;
        this.handler = handler;
    }

    /**
     * Extract the host and port from the config string
     * 
     * FIXME support IPv6
     * 
     * @param handler for sending messages to caller
     * @param hostAndPort host:port to connect to
     */
    private void setHostAndPort(@NonNull Handler handler, @NonNull String hostAndPort) {
        int doubleColon = hostAndPort.indexOf(':');
        if (doubleColon > 0) {
            try {
                host = hostAndPort.substring(0, doubleColon);
                port = Integer.parseInt(hostAndPort.substring(doubleColon + 1));
            } catch (NumberFormatException e) {
                reportError(handler, e);
                cancel();
            }
        } // otherwise crash and burn?
    }

    /**
     * Stop reading input and exit
     */
    public void cancel() {
        Log.d(DEBUG_TAG, "Cancel called");
        canceled = true;
        closeSocket();
    }

    @Override
    @TargetApi(24)
    public void run() {
        boolean useOldListener = newListener == null;
        OutputStreamWriter osw = null;
        BufferedReader input = null;
        try { // NOSONAR Android Socket doesn't implement AutoClose
            Log.d(DEBUG_TAG, "Connecting to " + host + ":" + port + " ...");
            socket = new Socket(host, port);
            osw = new OutputStreamWriter(socket.getOutputStream());

            InputStreamReader isr = new InputStreamReader(socket.getInputStream());
            input = new BufferedReader(isr);
            String firstLine = input.readLine();
            if (firstLine == null) {
                throw new IOException("unexpected EOF");
            }
            connectionMessage(handler, host + ":" + port);
            // gpsd message is json
            // NOSONAR {"class":"VERSION","release":"3.17","rev":"3.17","proto_major":3,"proto_minor":12}
            if (firstLine.contains("\"VERSION\"")) { // HACKALERT assume this is not NMEA and try to switch gpsd to NMEA
                                                     // output
                Log.d(DEBUG_TAG, "gpsd detected, trying to switch to NMEA output");
                osw.write("?WATCH={\"enable\":true,\"nmea\":true};\r\n");
                osw.flush();
                // skip stuff we don't need from gpsd
                input.readLine(); // NOSONAR
                input.readLine(); // NOSONAR
            } else {
                if (useOldListener) {
                    oldListener.onNmeaReceived(-1, firstLine);
                } else {
                    newListener.onNmeaMessage(firstLine, -1);
                }
            }
            while (!canceled) {
                String line = input.readLine();
                if (line != null) {
                    if (useOldListener) {
                        oldListener.onNmeaReceived(-1, line);
                    } else {
                        newListener.onNmeaMessage(input.readLine(), -1);
                    }
                } else {
                    closedMessage(handler);
                    break; // EOF
                }
            }
        } catch (Exception | Error e) { // NOSONAR never fail
            Log.e(DEBUG_TAG, "failed to open/read " + host + ":" + port + " " + e.getMessage());
            reportError(handler, e);
        } finally {
            SavingHelper.close(osw);
            // see https://code.google.com/p/android/issues/detail?id=62909
            // for why we can't use helper methods here
            closeSocket();
            SavingHelper.close(input);
        }
    }

    /**
     * Close the socket we've been using
     */
    private void closeSocket() {
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                Log.e(DEBUG_TAG, "Closing socket threw " + e.getMessage());
            }
        }
    }

    /**
     * Send the message from an exception
     * 
     * @param handler for sending messages to caller
     * @param e the Exception
     */
    static void reportError(@NonNull Handler handler, @NonNull Throwable e) {
        Message failed = handler.obtainMessage(TrackerService.CONNECTION_FAILED, e.getMessage());
        failed.sendToTarget();
    }

    /**
     * Send the the connected host and port
     * 
     * @param handler for sending messages to caller
     * @param hostAndPort the host and port
     */
    static void connectionMessage(@NonNull Handler handler, @NonNull String hostAndPort) {
        Message message = handler.obtainMessage(TrackerService.CONNECTION_MESSAGE, hostAndPort);
        message.sendToTarget();
    }

    /**
     * Note that the connection has closed
     * 
     * @param handler for sending messages to caller
     */
    static void closedMessage(@NonNull Handler handler) {
        Message message = handler.obtainMessage(TrackerService.CONNECTION_CLOSED);
        message.sendToTarget();
    }
}
