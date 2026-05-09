package de.blau.android.net;

import static de.blau.android.contract.Constants.LOG_TAG_LEN;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Collection;

import android.os.Build;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import okhttp3.OkHttpClient;
import okhttp3.tls.HandshakeCertificates;

/**
 *   Originally from
 * ¨ Nicked from https://github.com/slapperwan/gh4a/commit/985cc0459910bd8452db7e83e4427f01623d11d8
 *   as we now import Conscrypt we don't actually need the TLS bits anymore
 */
public final class OkHttpCompat {

    private static final int    TAG_LEN   = Math.min(LOG_TAG_LEN, OkHttpCompat.class.getSimpleName().length());
    private static final String DEBUG_TAG = OkHttpCompat.class.getSimpleName().substring(0, TAG_LEN);

    private static final String CACERT_PEM = "res/raw/cacert.pem"; // mozilla root certs extracted for use in curl

    /**
     * Private constructor to stop instantiation
     */
    private OkHttpCompat() {
        // private
    }

    /**
     * Add missing certificates for older for older Android versions
     * 
     * @param builder an OkHttpClient.Builder instance
     * @return the builder
     */
    public static OkHttpClient.Builder getBuilder(OkHttpClient.Builder builder) {
        try {
            addMissingCertificates(builder);
        } catch (CertificateException e) {
            Log.e(DEBUG_TAG, "Error adding additional certificates", e);
        }
        return builder;
    }

    /**
     * Add missing certificates for older Android versions
     * 
     * @param builder the OkHttp client builder
     * @throws CertificateException if something goes wrong ...
     */
    private static void addMissingCertificates(OkHttpClient.Builder builder) throws CertificateException {
        Log.d(DEBUG_TAG, "addMissingCertificates");
        HandshakeCertificates.Builder certBuilder = new HandshakeCertificates.Builder();
        ClassLoader loader = builder.getClass().getClassLoader();
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        // add missing CAs for devices prior to Android 14
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.TIRAMISU) {
            try {
                for (X509Certificate cert : readCertificatefromResources(cf, loader.getResourceAsStream(CACERT_PEM))) {
                    certBuilder.addTrustedCertificate(cert);
                }
            } catch (CertificateException | IOException e) {
                Log.e(DEBUG_TAG, "Error adding certificate from " + CACERT_PEM, e);
            }
        } else {
            certBuilder.addPlatformTrustedCertificates();
        }

        HandshakeCertificates certificates = certBuilder.build();
        builder.sslSocketFactory(certificates.sslSocketFactory(), certificates.trustManager());
    }

    /**
     * Read a certificate(s) from resources, both .pem and .crt format will work
     * 
     * @param cf a CertificateFactory
     * 
     * @return a Certificate
     * @throws FileNotFoundException if the file isn't found or couldn't be read
     * @throws CertificateException
     */
    @SuppressWarnings("unchecked")
    @NonNull
    private static Collection<X509Certificate> readCertificatefromResources(@NonNull CertificateFactory cf, @Nullable final InputStream certInputStream)
            throws FileNotFoundException, CertificateException {
        if (certInputStream == null) {
            throw new FileNotFoundException();
        }
        return (Collection<X509Certificate>) cf.generateCertificates(certInputStream);
    }
}
