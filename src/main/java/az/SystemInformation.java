package az;

import com.sun.management.UnixOperatingSystemMXBean;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.lang.management.ManagementFactory;

public class SystemInformation extends AbstractInformation {
    public String get() {
        try {
            java.lang.management.OperatingSystemMXBean operatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean();
            StringBuilder builder = new StringBuilder();
            addInformation(builder, "Available Processors", String.valueOf(operatingSystemMXBean.getAvailableProcessors()));
            addInformation(builder, "System Load Average", String.valueOf(operatingSystemMXBean.getSystemLoadAverage()));
            if (operatingSystemMXBean instanceof com.sun.management.OperatingSystemMXBean) {
                com.sun.management.OperatingSystemMXBean bean = (com.sun.management.OperatingSystemMXBean) operatingSystemMXBean;
                addInformation(builder, "Total Swap Space Size", String.valueOf(bean.getTotalSwapSpaceSize()));
                addInformation(builder, "Committed Virtual Memory Size", String.valueOf(bean.getCommittedVirtualMemorySize()));
                addInformation(builder, "Total Physical Memory Size", String.valueOf(bean.getCommittedVirtualMemorySize()));
            }
            if (operatingSystemMXBean instanceof UnixOperatingSystemMXBean) {
                UnixOperatingSystemMXBean bean = (UnixOperatingSystemMXBean) operatingSystemMXBean;
                addInformation(builder, "Max File Descriptor Count", String.valueOf(bean.getMaxFileDescriptorCount()));
                addInformation(builder, "Open File Descriptor Count", String.valueOf(bean.getOpenFileDescriptorCount()));
            }
            return builder.toString();
        } catch (Exception e) {
            return "Could not get System Information. Exception: " + ExceptionUtils.getStackTrace(e);
        }
    }
}
