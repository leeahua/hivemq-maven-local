package av;

import java.util.List;

public interface SharedSubscriptionsConfigurationService {

    List<String> getSharedSubscriptions();

    void setSharedSubscriptions(List<String> sharedSubscriptions);
}
