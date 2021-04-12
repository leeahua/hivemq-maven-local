package aw;

import av.SharedSubscriptionsConfigurationService;

import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;

@Singleton
public class SharedSubscriptionsConfigurationServiceImpl implements SharedSubscriptionsConfigurationService {
    private List<String> sharedSubscriptions = new ArrayList<>();

    public List<String> getSharedSubscriptions() {
        return sharedSubscriptions;
    }

    public void setSharedSubscriptions(List<String> sharedSubscriptions) {
        this.sharedSubscriptions = sharedSubscriptions;
    }
}
