package de.blau.android.services.util;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

import android.location.GpsStatus.NmeaListener;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import de.blau.android.services.TrackerService;
import de.blau.android.util.SavingHelper;

/**
 * The current android RTKLIB port doesn't support TCP servers. just clients ... so we run one
 * 
 * @author Simon
 *
 */
public class NmeaTcpClientServer implements Runnable {
    boolean            canceled = false;
    int                port     = 1959;
    final NmeaListener listener;
    final Handler handler;

    public NmeaTcpClientServer(String hostAndPort, NmeaListener listener, Handler handler) {
        // only use the port string
        int doubleColon = hostAndPort.indexOf(':');
        if (doubleColon > 0) {
            port = Integer.parseInt(hostAndPort.substring(doubleColon + 1));
        }
        this.listener = listener;
        this.handler = handler;
    }

    public void cancel() {
        canceled = true;
    }

    @Override
    public void run() {
        DataOutputStream dos = null;
        BufferedReader input = null;
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(port);
            while (!canceled) {
                Socket socket = serverSocket.accept();
                Log.d("TrackerService", "Incoming connection from " + socket.getRemoteSocketAddress().toString() + " ...");
                dos = new DataOutputStream(socket.getOutputStream());

                InputStreamReader isr = new InputStreamReader(socket.getInputStream());
                input = new BufferedReader(isr);
                try {
                    while (!canceled) {
                        listener.onNmeaReceived(-1, input.readLine());
                    }
                } catch (IOException ioex) {
                    // happens if client closes the socket
                    SavingHelper.close(dos);
                    SavingHelper.close(input);
                }
            }
        } catch (Exception e) {
            Message failed = handler.obtainMessage(TrackerService.CONNECTION_FAILED, e.getMessage());
            failed.sendToTarget();
        } catch (Error e) {
            Message failed = handler.obtainMessage(TrackerService.CONNECTION_FAILED, e.getMessage());
            failed.sendToTarget();
        } finally {
            SavingHelper.close(dos);
            SavingHelper.close(input);
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }
}
