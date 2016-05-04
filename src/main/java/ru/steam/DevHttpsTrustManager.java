package ru.steam;


import lombok.extern.slf4j.Slf4j;

import javax.net.ssl.*;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

@Slf4j
class DevHttpsTrustManager implements X509TrustManager {
    private static final X509Certificate[] _AcceptedIssuers;
    private static TrustManager[] trustManagers;

    /* renamed from: com.valvesoftware.android.steam.community.DevHttpsTrustManager.1 */
    static class C01401 implements HostnameVerifier {
        C01401() {
        }

        public boolean verify(String arg0, SSLSession arg1) {
            return arg0 != null && arg0.contains("valvesoftware.com");
        }
    }

    DevHttpsTrustManager() {
    }

    static {
        _AcceptedIssuers = new X509Certificate[0];
    }

    public static void allowSslToValveDev() {
        HttpsURLConnection.setDefaultHostnameVerifier(new C01401());
        SSLContext context = null;
        if (trustManagers == null) {
            trustManagers = new TrustManager[]{new DevHttpsTrustManager()};
        }
        try {
            context = SSLContext.getInstance("TLS");
            context.init(null, trustManagers, new SecureRandom());
        } catch (NoSuchAlgorithmException e) {
            log.error(DevHttpsTrustManager.class.getSimpleName(), e.toString());
        } catch (KeyManagementException e2) {
            log.error(DevHttpsTrustManager.class.getSimpleName(), e2.toString());
        }
        HttpsURLConnection.setDefaultSSLSocketFactory(context.getSocketFactory());
    }

    public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
    }

    public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
    }

    public X509Certificate[] getAcceptedIssuers() {
        return _AcceptedIssuers;
    }
}
