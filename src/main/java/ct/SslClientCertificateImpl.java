package ct;

import com.google.common.base.Preconditions;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.security.SslClientCertificate;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x500.style.IETFUtils;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;

import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

public class SslClientCertificateImpl implements SslClientCertificate {
    private final Certificate[] certificateChain;

    public SslClientCertificateImpl(@NotNull Certificate[] certificateChain) {
        Preconditions.checkNotNull(certificateChain, "Certificates must not be null");
        this.certificateChain = certificateChain;
    }

    public Certificate certificate() {
        return this.certificateChain[0];
    }

    public Certificate[] certificateChain() {
        return this.certificateChain;
    }

    public String commonName() {
        return getAttribute(BCStyle.CN);
    }

    public String organization() {
        return getAttribute(BCStyle.O);
    }

    public String organizationalUnit() {
        return getAttribute(BCStyle.OU);
    }

    public String title() {
        return getAttribute(BCStyle.T);
    }

    public String serial() {
        return getAttribute(BCStyle.SN);
    }

    public String country() {
        return getAttribute(BCStyle.C);
    }

    public String locality() {
        return getAttribute(BCStyle.L);
    }

    public String state() {
        return getAttribute(BCStyle.ST);
    }

    private String getAttribute(ASN1ObjectIdentifier attributeType) {
        try {
            X509Certificate certificate = (X509Certificate) certificate();
            if (attributeType.equals(BCStyle.CN)) {
                X500Name subject = new JcaX509CertificateHolder(certificate).getSubject();
                RDN[] rdNs = subject.getRDNs(attributeType);
                if (rdNs.length < 1) {
                    return null;
                }
                RDN rdN = rdNs[0];
                return IETFUtils.valueToString(rdN.getFirst().getValue());
            }
            if (attributeType.equals(BCStyle.SN)) {
                return certificate.getSerialNumber().toString();
            }
            Object extension = new JcaX509CertificateHolder(certificate).getExtension(attributeType);
            if (extension == null) {
                return null;
            }
            return ((Extension) extension).getParsedValue().toString();
        } catch (Exception e) {
            throw new RuntimeException("Not able to get property from certificate", e);
        }
    }
}
