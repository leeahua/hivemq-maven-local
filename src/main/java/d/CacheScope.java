package d;

import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.Scope;
import com.google.inject.Scopes;

class CacheScope implements Scope {
    public <T> Provider<T> scope(Key<T> key, Provider<T> provider) {
        return Scopes.SINGLETON.scope(key, provider);
    }
}
