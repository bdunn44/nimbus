package com.kbdunn.nimbus.api.network.security;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SSLTrustManager {

	private static final Logger log = LoggerFactory.getLogger(SSLTrustManager.class);
	private static SSLTrustManager instance;
	
	private SSLContext sslContext;
	
	public SSLTrustManager() {
		try {
			KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
			Path ksPath = Paths.get(System.getProperty("java.home"), "lib", "security", "cacerts");
			keyStore.load(Files.newInputStream(ksPath), "changeit".toCharArray());
			
			CertificateFactory cf = CertificateFactory.getInstance("X.509");
			addCertificate("/lets-encrypt-x1-cross-signed.der", "lets-encrypt-x1", cf, keyStore);
			addCertificate("/lets-encrypt-x2-cross-signed.der", "lets-encrypt-x2", cf, keyStore);
			addCertificate("/lets-encrypt-x3-cross-signed.der", "lets-encrypt-x3", cf, keyStore);
			addCertificate("/lets-encrypt-x4-cross-signed.der", "lets-encrypt-x4", cf, keyStore);
			
			/*log.info("Truststore now trusting: ");
			PKIXParameters params = new PKIXParameters(keyStore);
			params.getTrustAnchors().stream()
			        .map(TrustAnchor::getTrustedCert)
			        .map(X509Certificate::getSubjectDN)
			        .forEach(System.out::println);*/
			
			TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			tmf.init(keyStore);
			sslContext = SSLContext.getInstance("TLS");
			sslContext.init(null, tmf.getTrustManagers(), null);
			SSLContext.setDefault(sslContext);
			HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
		} catch (NoSuchAlgorithmException | KeyManagementException | KeyStoreException | CertificateException | IOException e) {
			log.error("Error retrieving TLS context", e);
		}
	}
	
	public static SSLTrustManager instance() {
		if (instance == null) {
			instance = new SSLTrustManager();
		}
		return instance;
	}
	
	// Could be obtained via SSLContext.getDefault(), but this ensures certs are loaded
	public SSLContext getSSLContext() {
		return sslContext;
	}
	
	// This is for Dev/Test purposes only!
	public void disableSSLVerification() {
		log.warn("Diabling SSL Verification!!");
		try {
			// Create a trust manager that does not validate certificate chains
			TrustManager[] trustAllCerts = new TrustManager[] { 
				new X509TrustManager() {
					public X509Certificate[] getAcceptedIssuers() { return null; }
					public void checkClientTrusted(java.security.cert.X509Certificate[] arg0, String authType) throws CertificateException {  }
					public void checkServerTrusted(java.security.cert.X509Certificate[] arg0, String authType) throws CertificateException {  }
				}
			};
			
			// Install the all-trusting trust manager
			SSLContext sc = SSLContext.getInstance("SSL");
			sc.init(null, trustAllCerts, new java.security.SecureRandom());
			HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
			
			// Create all-trusting host name verifier
			HostnameVerifier allHostsValid = new HostnameVerifier() {
				public boolean verify(String hostname, SSLSession session) { return true; }
			};
			   
			// Install the all-trusting host verifier
			HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
		} catch (NoSuchAlgorithmException|KeyManagementException e) {
			log.error("Error disabling SSL verification", e);
		}
	}
	
	private void addCertificate(String resourcePath, String alias, CertificateFactory cf, KeyStore keyStore) 
			throws KeyStoreException, CertificateException, IOException {
		try (InputStream caInput = new BufferedInputStream(getClass().getResourceAsStream(resourcePath))) {
			Certificate crt = cf.generateCertificate(caInput);
			keyStore.setCertificateEntry(alias, crt);
			log.info("Added X.509 certificate for {}", ((X509Certificate) crt).getSubjectDN());
		}
	}
	
    public static void main(String[] args) throws IOException {
    	SSLTrustManager.instance();
        // signed by default trusted CAs.
        testUrl(new URL("https://google.com"));
        testUrl(new URL("https://www.thawte.com"));
        
        testUrl(new URL("https://helloworld.letsencrypt.org/")); // signed by letsencrypt
        testUrl(new URL("https://letsencrypt.org/")); // signed by LE's cross-sign CA (IdenTrust)
        testUrl(new URL("https://tv.eurosport.com/")); // expired
        testUrl(new URL("https://www.pcwebshop.co.uk/")); // self-signed
    }
    
    static void testUrl(URL url) throws IOException {
        URLConnection connection = url.openConnection();
        try {
            connection.connect();
            System.out.println("Headers of " + url + ": "
                    + connection.getHeaderFields());
        } catch (SSLHandshakeException e) {
            System.out.println("Untrusted: " + url);
        }
    }
}
