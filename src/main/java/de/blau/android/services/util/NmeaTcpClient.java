package de.blau.android.services.util;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

import android.annotation.TargetApi;
import android.location.GpsStatus.NmeaListener;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.util.Log;
import de.blau.android.services.TrackerService;
import de.blau.android.util.SavingHelper;

public class NmeaTcpClient implements Runnable {

    private static final String DEBUG_TAG = "NmeaTcpClient";
    String                      host;
    int                         port;
    boolean                     canceled  = false;
    final NmeaListener          listener;
    final Handler               handler;

    public NmeaTcpClient(@NonNull String hostAndPort, @NonNull NmeaListener listener, Handler handler) {
        int doubleColon = hostAndPort.indexOf(':');
        if (doubleColon > 0) {
            host = hostAndPort.substring(0, doubleColon);
            port = Integer.parseInt(hostAndPort.substring(doubleColon + 1));
        } // otherwise crash and burn?
        this.listener = listener;
        this.handler = handler;
    }

    public void cancel() {
        canceled = true;
    }

    @Override
    @TargetApi(19)
    public void run() {
        Socket socket = null;
        DataOutputStream dos = null;
        BufferedReader input = null;
        try {
            Log.d(DEBUG_TAG, "Connecting to " + host + ":" + port + " ...");

            socket = new Socket(host, port);
            dos = new DataOutputStream(socket.getOutputStream());

            InputStreamReader isr = new InputStreamReader(socket.getInputStream());
            input = new BufferedReader(isr);

            while (!canceled) {
                listener.onNmeaReceived(-1, input.readLine());
            }

        } catch (Exception e) {
            Log.e(DEBUG_TAG, "failed to open/read " + host + ":" + port, e);
            Message failed = handler.obtainMessage(TrackerService.CONNECTION_FAILED, e.getMessage());
            failed.sendToTarget();
        } catch (Error e) {
            Message failed = handler.obtainMessage(TrackerService.CONNECTION_FAILED, e.getMessage());
            failed.sendToTarget();
        } finally {
            SavingHelper.close(dos);
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
}
