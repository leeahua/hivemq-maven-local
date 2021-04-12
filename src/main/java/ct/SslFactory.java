package ct;

import com.hivemq.spi.annotations.Nullable;
import com.hivemq.spi.services.configuration.entity.Tls;
import com.hivemq.spi.util.SslException;
import io.netty.channel.Channel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.SupportedCipherSuiteFilter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManagerFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.List;

public class SslFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(SslFactory.class);
    private final SslContextStore store;

    public SslFactory(SslContextStore store) {
        this.store = store;
    }

    public SslHandler createSslHandler(Channel channel, Tls tls) {
        SslHandler sslHandler = new SslHandler(createSSLEngine(channel, tls));
        sslHandler.setHandshakeTimeoutMillis(tls.getHandshakeTimeout());
        return sslHandler;
    }

    public SSLEngine createSSLEngine(Channel channel, Tls tls) {
        SSLEngine sslEngine = createSslContext(tls).newEngine(channel.alloc());
        if (tls.getProtocols().size() > 0) {
            List<String> protocols = tls.getProtocols();
            String[] enableProtocols = protocols.toArray(new String[protocols.size()]);
            sslEngine.setEnabledProtocols(enableProtocols);
        }
        sslEngine.setUseClientMode(false);
        if (Tls.ClientAuthMode.REQUIRED.equals(tls.getClientAuthMode())) {
            sslEngine.setNeedClientAuth(true);
        }
        if (Tls.ClientAuthMode.OPTIONAL.equals(tls.getClientAuthMode())) {
            sslEngine.setWantClientAuth(true);
        }
        return sslEngine;
    }

    public SslContext createSslContext(Tls tls) {
        try {
            if (this.store.contains(tls)) {
                return this.store.get(tls);
            }
            KeyManagerFactory keyManagerFactory = createKeyManagerFactory(tls);
            TrustManagerFactory trustManagerFactory = createTrustManagerFactory(tls);
            SslContext sslContext;
            if (tls.getCipherSuites().size() > 0) {
                sslContext = SslContext.newServerContext(SslProvider.JDK, null, trustManagerFactory, null, null, null, keyManagerFactory, tls.getCipherSuites(), SupportedCipherSuiteFilter.INSTANCE, null, 0L, 0L);
            } else {
                sslContext = SslContext.newServerContext(SslProvider.JDK, null, trustManagerFactory, null, null, null, keyManagerFactory, null, SupportedCipherSuiteFilter.INSTANCE, null, 0L, 3600L);
            }
            this.store.put(tls, sslContext);
            return sslContext;
        } catch (SSLException e) {
            throw new SslException("Not able to create SSL server context", e);
        }
    }

    public KeyManagerFactory createKeyManagerFactory(Tls tls) {
        try {
            KeyStore keyStore = KeyStore.getInstance(tls.getKeystoreType());
            keyStore.load(new FileInputStream(new File(tls.getKeystorePath())), tls.getKeystorePassword().toCharArray());
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keyStore, tls.getPrivateKeyPassword().toCharArray());
            return keyManagerFactory;
        } catch (UnrecoverableKeyException e) {
            throw new SslException("Not able to recover key from KeyStore, please check your private-key-password and your keyStorePassword", e);
        } catch (KeyStoreException | IOException e) {
            throw new SslException("Not able to open or read KeyStore '" + tls.getKeystorePath() + "' with type '" + tls.getKeystoreType() + "'", e);
        } catch (NoSuchAlgorithmException | CertificateException e) {
            throw new SslException("Not able to read certificate from KeyStore '" + tls.getKeystorePath(), e);
        }
    }

    @Nullable
    public TrustManagerFactory createTrustManagerFactory(Tls tls) {
        if (StringUtils.isBlank(tls.getTruststorePath())) {
            return null;
        }
        try {
            KeyStore keyStore = KeyStore.getInstance(tls.getTruststoreType());
            keyStore.load(new FileInputStream(new File(tls.getTruststorePath())), tls.getTruststorePassword().toCharArray());
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(keyStore);
            return trustManagerFactory;
        } catch (KeyStoreException | IOException e) {
            throw new SslException("Not able to open or read TrustStore '" + tls.getTruststorePath() + "' with type '" + tls.getTruststoreType() + "'", e);
        } catch (NoSuchAlgorithmException | CertificateException e) {
            throw new SslException("Not able to read certificate from TrustStore '" + tls.getTruststorePath(), e);
        }
    }
}
