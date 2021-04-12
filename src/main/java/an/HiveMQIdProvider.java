package an;

import ao.Preference;

import javax.inject.Inject;
import java.util.Optional;
import java.util.UUID;
import java.util.prefs.Preferences;

public class HiveMQIdProvider {
    public final Preferences preferences;
    public static final String KEY = "hmqId";

    @Inject
    public HiveMQIdProvider(@Preference Preferences preferences) {
        this.preferences = preferences;
    }

    public String get() {
        Optional<String> mayHmqId = Optional.ofNullable(this.preferences.get(KEY, null));
        if (mayHmqId.isPresent()) {
            return mayHmqId.get();
        }
        String hmqId = UUID.randomUUID().toString();
        this.preferences.put(KEY, hmqId);
        return hmqId;
    }
}
