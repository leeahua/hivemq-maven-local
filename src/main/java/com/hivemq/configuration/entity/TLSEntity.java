package com.hivemq.configuration.entity;

import com.hivemq.spi.util.DefaultSslEngineUtil;
import com.hivemq.spi.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.List;

@XmlType(propOrder = {})
public class TLSEntity {
    private static final Logger LOGGER = LoggerFactory.getLogger(TLSEntity.class);
    @XmlElement(name = "keystore", required = true)
    private Keystore keystore = new Keystore();
    @XmlElement(name = "truststore")
    private Truststore truststore = new Truststore();
    @XmlElement(name = "handshake-timeout", defaultValue = "10000")
    private Integer handshakeTimeout = 10000;
    @XmlElement(name = "client-authentication-mode", defaultValue = "NONE")
    private ClientAuthenticationModeEntity clientAuthMode = ClientAuthenticationModeEntity.NONE;
    @XmlElementWrapper(name = "protocols")
    @XmlElement(name = "protocol")
    private List<String> protocols = defaultProtocols();
    @XmlElementWrapper(name = "cipher-suites")
    @XmlElement(name = "cipher-suite")
    private List<String> cipherSuites = defaultCipherSuites();

    public Keystore getKeystore() {
        return this.keystore;
    }

    public Truststore getTruststore() {
        return this.truststore;
    }

    public int getHandshakeTimeout() {
        return this.handshakeTimeout;
    }

    public ClientAuthenticationModeEntity getClientAuthMode() {
        return this.clientAuthMode;
    }

    public List<String> getProtocols() {
        return this.protocols;
    }

    public List<String> getCipherSuites() {
        return this.cipherSuites;
    }

    private List<String> defaultCipherSuites() {
        return new ArrayList(new DefaultSslEngineUtil().getEnabledCipherSuites());
    }

    private List<String> defaultProtocols() {
        return new ArrayList(new DefaultSslEngineUtil().getEnabledProtocols());
    }

    @XmlType(name = "truststoreType", propOrder = {})
    public static class Truststore {
        @XmlElement(name = "path", required = true)
        private String path = "";
        @XmlElement(name = "password", required = true)
        private String password = "";

        public String getPath() {
            return this.path;
        }

        public String getPassword() {
            return this.password;
        }

        void afterUnmarshal(Unmarshaller paramUnmarshaller, Object paramObject) {
            this.path = PathUtils.findAbsoluteAndRelative(this.path).getAbsolutePath();
        }
    }

    @XmlType(name = "keystoreType", propOrder = {})
    public static class Keystore {
        @XmlElement(name = "path", required = true)
        private String path = "";
        @XmlElement(name = "password", required = true)
        private String password = "";
        @XmlElement(name = "private-key-password", required = true)
        private String privateKeyPassword = "";

        public String getPath() {
            return this.path;
        }

        public String getPassword() {
            return this.password;
        }

        public String getPrivateKeyPassword() {
            return this.privateKeyPassword;
        }

        void afterUnmarshal(Unmarshaller paramUnmarshaller, Object paramObject) {
            this.path = PathUtils.findAbsoluteAndRelative(this.path).getAbsolutePath();
        }
    }
}
