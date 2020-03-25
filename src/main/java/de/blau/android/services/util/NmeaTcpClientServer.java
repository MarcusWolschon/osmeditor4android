package de.blau.android.services.util;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

import android.annotation.TargetApi;
import android.location.GpsStatus.NmeaListener;
import android.location.OnNmeaMessageListener;
import android.os.Handler;
import androidx.annotation.NonNull;
import android.util.Log;
import de.blau.android.util.SavingHelper;

/**
 * The current android RTKLIB port doesn't support TCP servers. just clients ... so we run one
 * 
 * @author Simon
 *
 */
@SuppressWarnings("deprecation")
public class NmeaTcpClientServer implements Runnable {

    private static final String DEBUG_TAG = "NmeaTcpClientServer";

    boolean                     canceled = false;
    int                         port     = 1959;
    final NmeaListener          oldListener;
    final OnNmeaMessageListener newListener;
    final Handler               handler;

    /**
     * Create an instance of a server that will read lines from a socket until it is stopped
     * 
     * @param hostAndPort host:port to listen on
     * @param oldListener the listener that will be called with the input
     * @param handler for sending messages to caller
     */
    public NmeaTcpClientServer(@NonNull String hostAndPort, @NonNull NmeaListener oldListener, @NonNull Handler handler) {
        getPort(hostAndPort);
        this.oldListener = oldListener;
        this.newListener = null;
        this.handler = handler;
    }

    /**
     * Create an instance of a server that will read lines from a socket until it is stopped
     * 
     * @param hostAndPort host:port to listen on
     * @param newListener the listener that will be called with the input
     * @param handler for sending messages to caller
     */
    public NmeaTcpClientServer(@NonNull String hostAndPort, @NonNull OnNmeaMessageListener newListener, @NonNull Handler handler) {
        getPort(hostAndPort);
        this.newListener = newListener;
        this.oldListener = null;
        this.handler = handler;
    }

    /**
     * Extract the port from a host:port String
     * 
     * @param hostAndPort host:port to listen on
     */
    private void getPort(@NonNull String hostAndPort) {
        // only use the port string
        int doubleColon = hostAndPort.indexOf(':');
        if (doubleColon > 0) {
            try {
                port = Integer.parseInt(hostAndPort.substring(doubleColon + 1));
            } catch (NumberFormatException e) {
                NmeaTcpClient.reportError(handler, e);
                cancel();
            }
        }
    }

    /**
     * Stop reading input and exit
     */
    public void cancel() {
        canceled = true;
    }

    @TargetApi(24)
    @Override
    public void run() {
        DataOutputStream dos = null;
        BufferedReader input = null;
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(port);
            while (!canceled) {
                Log.d(DEBUG_TAG, "Listening for incoming connection on " + port);
                Socket socket = serverSocket.accept();
                Log.d(DEBUG_TAG, "Incoming connection from " + socket.getRemoteSocketAddress().toString() + " ...");
                NmeaTcpClient.connectionMessage(handler, socket.getRemoteSocketAddress().toString());
                dos = new DataOutputStream(socket.getOutputStream());

                InputStreamReader isr = new InputStreamReader(socket.getInputStream());
                input = new BufferedReader(isr);
                try {
                    while (!canceled) {
                        String line = input.readLine();
                        if (line != null) {
                            if (newListener == null) {
                                oldListener.onNmeaReceived(-1, input.readLine());
                            } else {
                                newListener.onNmeaMessage(input.readLine(), -1);
                            }
                        } else {
                            NmeaTcpClient.closedMessage(handler);
                            break; // EOF
                        }
                    }
                } catch (IOException ioex) {
                    // happens if client closes the socket
                    SavingHelper.close(dos);
                    SavingHelper.close(input);
                }
            }
        } catch (Exception e) {
            NmeaTcpClient.reportError(handler, e);
        } catch (Error e) {
            NmeaTcpClient.reportError(handler, e);
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
