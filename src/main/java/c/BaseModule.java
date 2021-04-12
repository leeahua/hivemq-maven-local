package c;

import com.google.inject.AbstractModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BaseModule<T> extends AbstractModule {
    private static final Logger LOGGER = LoggerFactory.getLogger(BaseModule.class);
    protected Class<T> target;

    public BaseModule(Class<T> target) {
        this.target = target;
    }

    public boolean equals(Object obj) {
        return (obj instanceof BaseModule) &&
                (((BaseModule) obj).target.equals(this.target));
    }

    public int hashCode() {
        return this.target.hashCode();
    }

    public String toString() {
        return getClass().getName() + "(key=" + this.target.toString() + ")";
    }

    public void asEagerSingleton(Class<? extends Object> clazz) {
        LOGGER.trace("Instantiating {} as eager singleton", clazz.getCanonicalName());
        bind(clazz).asEagerSingleton();
    }
}
