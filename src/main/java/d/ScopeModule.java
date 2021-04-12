package d;

import c.BaseModule;
import com.google.inject.Scope;

public class ScopeModule extends BaseModule<ScopeModule> {

    public ScopeModule() {
        super(ScopeModule.class);
    }

    protected void configure() {
        Scope scope = ScopeContext.get();
        bindScope(CacheScoped.class, scope);
    }
}
