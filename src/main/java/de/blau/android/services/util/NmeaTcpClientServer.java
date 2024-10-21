package de.blau.android.services.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

import android.annotation.TargetApi;
import android.location.GpsStatus.NmeaListener;
import android.location.OnNmeaMessageListener;
import android.os.Handler;
import android.util.Log;
import androidx.annotation.NonNull;

/**
 * The current android RTKLIB port doesn't support TCP servers. just clients ... so we run one
 * 
 * @author Simon
 *
 */
@SuppressWarnings("deprecation")
public class NmeaTcpClientServer implements Runnable {
    private static final String DEBUG_TAG = "NmeaTcpClientServer";

    private static final String CLOSED = "closed";

    private boolean                     canceled = false;
    private int                         port     = 1959;
    private final NmeaListener          oldListener;
    private final OnNmeaMessageListener newListener;
    private final Handler               handler;
    private Socket                      socket;
    private ServerSocket                listenSocket;

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
    public synchronized void cancel() {
        Log.w(DEBUG_TAG, "Stopping server");
        canceled = true;
        try {
            if (socket != null) {
                socket.close();
                socket = null;
            }
            if (listenSocket != null) {
                listenSocket.close();
                listenSocket = null;
            }
        } catch (Exception ex) {
            // Ignore
        }
    }

    @Override
    public void run() {
        Log.w(DEBUG_TAG, "Starting server");
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            serverSocket.setReuseAddress(true);
            listenSocket = serverSocket;
            while (!canceled) {
                Log.d(DEBUG_TAG, "Listening for incoming connection on " + port);
                socket = serverSocket.accept();
                Log.d(DEBUG_TAG, "Incoming connection from " + socket.getRemoteSocketAddress().toString() + " ...");
                NmeaTcpClient.connectionMessage(handler, socket.getRemoteSocketAddress().toString());
                readFromSocket(socket);
            }
        } catch (Throwable e) { // NOSONAR
            Log.w(DEBUG_TAG, "Exception  " + e);
            String message = e.getMessage();
            // there is no good way to avoid this
            if (e instanceof SocketException && message != null && message.toLowerCase().contains(CLOSED)) {
                return; // don't show message as it is expected
            }
            NmeaTcpClient.reportError(handler, e);
        }
    }

    /**
     * Read sentences from socket until closed or canceled
     * 
     * @param socket the Socket
     */
    @TargetApi(24)
    private void readFromSocket(@NonNull Socket socket) {
        try (InputStreamReader isr = new InputStreamReader(socket.getInputStream()); BufferedReader input = new BufferedReader(isr)) {
            while (!canceled) {
                String line = input.readLine();
                if (line != null) {
                    if (newListener == null) {
                        oldListener.onNmeaReceived(-1, line);
                    } else {
                        newListener.onNmeaMessage(line, -1);
                    }
                } else {
                    NmeaTcpClient.closedMessage(handler);
                    break; // EOF
                }
            }
        } catch (IOException ioex) {
            Log.w(DEBUG_TAG, "Exception reading from socket " + ioex);
            // happens if client closes the socket
        }
    }
}
