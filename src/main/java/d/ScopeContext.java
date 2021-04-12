package d;

import com.google.inject.Scope;

public class ScopeContext {
    private static final Scope SCOPE = new CacheScope();

    public static Scope get() {
        return SCOPE;
    }
}
