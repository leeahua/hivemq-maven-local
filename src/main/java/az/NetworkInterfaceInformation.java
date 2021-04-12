package az;

import aj1.MacAddressFormatter;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.Enumeration;

public class NetworkInterfaceInformation extends AbstractInformation {
    public String get() {
        try {
            StringBuilder builder = new StringBuilder();
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            Collections.list(networkInterfaces).forEach(networkInterface ->
                    addInterfaceInformation(builder, networkInterface)
            );
            return builder.toString();
        } catch (Exception e) {
            return "Could not determine network interfaces. Exception: " + ExceptionUtils.getStackTrace(e);
        }
    }

    private StringBuilder addInterfaceInformation(StringBuilder builder,
                                                  NetworkInterface networkInterface) {
        builder.append(String.format("┌[%s]\n", networkInterface.getName()));
        append(builder, "Display Name", networkInterface.getDisplayName());
        append(builder, "MAC Address", determineMACAddress(networkInterface));
        append(builder, "MTU", determineMTU(networkInterface));
        append(builder, "Is Loopback?", determineIfInterfaceIsLoopbackInterface(networkInterface));
        append(builder, "Is P2P?", determineIfInterfaceIsP2PInterface(networkInterface));
        append(builder, "Is Up?", determineIfInterfaceIsUp(networkInterface));
        append(builder, "Is Virtual?", determineIfInterfaceIsVirtual(networkInterface));
        append(builder, "Supports Multicast?", determineIfInterfaceSupportsMulticast(networkInterface));
        builder.append(String.format("└─[%s]\n", "Inet Addresses"));
        Enumeration<InetAddress> addressEnumeration = networkInterface.getInetAddresses();
        Collections.list(addressEnumeration).forEach(inetAddress ->
                builder.append(String.format("\t├─[%s]\n", inetAddress)));
        builder.append("\n");
        return builder;
    }

    private String determineIfInterfaceIsLoopbackInterface(NetworkInterface paramNetworkInterface) {
        try {
            return String.valueOf(paramNetworkInterface.isLoopback());
        } catch (Exception e) {
        }
        return "Could not determine if interface is loopback interface";
    }

    private String determineIfInterfaceIsP2PInterface(NetworkInterface paramNetworkInterface) {
        try {
            return String.valueOf(paramNetworkInterface.isPointToPoint());
        } catch (Exception e) {
        }
        return "Could not determine if interface is P2P interface";
    }

    private String determineIfInterfaceIsUp(NetworkInterface paramNetworkInterface) {
        try {
            return String.valueOf(paramNetworkInterface.isUp());
        } catch (Exception e) {
        }
        return "Could not determine if interface is up";
    }

    private String determineIfInterfaceIsVirtual(NetworkInterface paramNetworkInterface) {
        try {
            return String.valueOf(paramNetworkInterface.isVirtual());
        } catch (Exception e) {
        }
        return "Could not determine if interface is virtual";
    }

    private String determineIfInterfaceSupportsMulticast(NetworkInterface paramNetworkInterface) {
        try {
            return String.valueOf(paramNetworkInterface.supportsMulticast());
        } catch (Exception e) {
        }
        return "Could not determine if interface supports multicast";
    }

    private String determineMTU(NetworkInterface networkInterface) {
        try {
            return String.valueOf(networkInterface.getMTU());
        } catch (Exception e) {
        }
        return "Could not determine MTU";
    }

    private String determineMACAddress(NetworkInterface networkInterface) {
        try {
            byte[] hardwareAddress = networkInterface.getHardwareAddress();
            if (hardwareAddress != null &&
                    hardwareAddress.length == MacAddressFormatter.HARDWARE_ADDRESS_LENGTH) {
                return MacAddressFormatter.format(hardwareAddress);
            }
            return "Could not determine MAC Address";
        } catch (Exception e) {
        }
        return "Could not determine MAC Address";
    }


    private StringBuilder append(StringBuilder builder, String name, String value) {
        return builder.append(String.format("├─[%s] = [%s]\n", name, value));
    }
}
