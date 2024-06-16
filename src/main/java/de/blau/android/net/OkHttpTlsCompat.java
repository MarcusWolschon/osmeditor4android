package de.blau.android.net;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import android.os.Build;
import android.util.Log;
import androidx.annotation.NonNull;
import okhttp3.ConnectionSpec;
import okhttp3.OkHttpClient;
import okhttp3.TlsVersion;
import okhttp3.tls.HandshakeCertificates;

/**
 * ¨ Nicked from https://github.com/slapperwan/gh4a/commit/985cc0459910bd8452db7e83e4427f01623d11d8
 */
public final class OkHttpTlsCompat {
    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, OkHttpTlsCompat.class.getSimpleName().length());
    private static final String DEBUG_TAG = OkHttpTlsCompat.class.getSimpleName().substring(0, TAG_LEN);

    private static final String TL_SV1_2     = "TLSv1.2";
    private static final String ISRG_ROOT_X1 = "res/raw/isrg_root_x1.pem";

    /**
     * Private constructor to stop instantiation
     */
    private OkHttpTlsCompat() {
        // private
    }

    /**
     * Add TLS 1.2 support and ISRG X1 cert for letsencrypt for older for older Android versions
     * 
     * @param builder an OkHttpClient.Builder instance
     * @return the builder with TLS1.2 added if necessary
     */
    public static OkHttpClient.Builder getBuilder(OkHttpClient.Builder builder) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN && Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1) {
            Log.d(DEBUG_TAG, "Adding TLSv1.2 support");
            try {
                SSLContext sc = SSLContext.getInstance(TL_SV1_2);
                sc.init(null, null, null);

                TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                tmf.init((KeyStore) null);
                TrustManager[] trustManagers = tmf.getTrustManagers();
                if (trustManagers.length != 1 || !(trustManagers[0] instanceof X509TrustManager)) {
                    throw new IllegalStateException("Unexpected default trust managers:" + Arrays.toString(trustManagers));
                }
                X509TrustManager tm = (X509TrustManager) trustManagers[0];

                ConnectionSpec cs = new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS).tlsVersions(TlsVersion.TLS_1_2).build();

                List<ConnectionSpec> specs = new ArrayList<>();
                specs.add(cs);
                specs.add(ConnectionSpec.COMPATIBLE_TLS);
                specs.add(ConnectionSpec.CLEARTEXT);

                builder.sslSocketFactory(new Tls12SocketFactory(sc.getSocketFactory()), tm);
                builder.connectionSpecs(specs);
            } catch (Exception exc) {
                Log.e(DEBUG_TAG, "Error while setting TLS 1.2", exc);
            }
        }
        // Add ISRG X1 cert for letsencrypt on older devices (pre and including 7.1)
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N_MR1) {
            Log.d(DEBUG_TAG, "Adding ISRG X1 root certificate");
            // download fresh from https://letsencrypt.org/certs/isrgrootx1.pem
            try {
                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                // we don't have an context here and it would be very inconvenient to get one, this is a hack around
                // that
                final InputStream certInputStream = builder.getClass().getClassLoader().getResourceAsStream(ISRG_ROOT_X1);
                if (certInputStream == null) {
                    throw new FileNotFoundException(ISRG_ROOT_X1);
                }
                java.security.cert.Certificate isgCertificate = cf.generateCertificate(certInputStream);
                HandshakeCertificates certificates = new HandshakeCertificates.Builder().addTrustedCertificate((X509Certificate) isgCertificate)
                        // Uncomment to allow connection to any site generally, but could possibly cause
                        // noticeable memory pressure in Android apps.
                        .addPlatformTrustedCertificates().build();

                builder.sslSocketFactory(certificates.sslSocketFactory(), certificates.trustManager());
            } catch (CertificateException | IOException e) {
                Log.e(DEBUG_TAG, "Error adding ISRG X1 cert connections to letsencrypt secured servers will not work", e);
            }
        }

        return builder;
    }

    private static class Tls12SocketFactory extends SSLSocketFactory {

        private static final String[] TLS_V12_ONLY = { TL_SV1_2 };

        final SSLSocketFactory delegate;

        /**
         * Construct a new Tls12SocketFactory
         * 
         * @param base the original SSLSocketFactory
         */
        public Tls12SocketFactory(@NonNull SSLSocketFactory base) {
            this.delegate = base;
        }

        @Override
        public String[] getDefaultCipherSuites() {
            return delegate.getDefaultCipherSuites();
        }

        @Override
        public String[] getSupportedCipherSuites() {
            return delegate.getSupportedCipherSuites();
        }

        @Override
        public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
            return patch(delegate.createSocket(s, host, port, autoClose));
        }

        @Override
        public Socket createSocket(String host, int port) throws IOException {
            return patch(delegate.createSocket(host, port));
        }

        @Override
        public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException {
            return patch(delegate.createSocket(host, port, localHost, localPort));
        }

        @Override
        public Socket createSocket(InetAddress host, int port) throws IOException {
            return patch(delegate.createSocket(host, port));
        }

        @Override
        public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
            return patch(delegate.createSocket(address, port, localAddress, localPort));
        }

        /**
         * Enable tls 1.2 on a Socket
         * 
         * @param s the Socket
         * @return the provided Socket
         */
        private Socket patch(Socket s) {
            if (s instanceof SSLSocket) {
                ((SSLSocket) s).setEnabledProtocols(TLS_V12_ONLY);
            }
            return s;
        }
    }
}
