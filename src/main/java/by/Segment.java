package by;

import bx.SubscriberWithQoS;
import com.google.common.collect.ImmutableMap;

import java.util.Set;

public class Segment {
    private final String key;
    private final ImmutableMap<String, Set<SubscriberWithQoS>> entries;

    public Segment(String key,
                   ImmutableMap<String, Set<SubscriberWithQoS>> entries) {
        this.key = key;
        this.entries = entries;
    }

    public String getKey() {
        return key;
    }

    public ImmutableMap<String, Set<SubscriberWithQoS>> getEntries() {
        return entries;
    }
}
