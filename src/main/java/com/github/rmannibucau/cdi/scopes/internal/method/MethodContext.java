package com.github.rmannibucau.cdi.scopes.internal.method;

import com.github.rmannibucau.cdi.scopes.api.method.WrappingMethodScoped;

import javax.enterprise.context.ContextNotActiveException;
import javax.enterprise.context.spi.AlterableContext;
import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;
import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

import static java.util.Optional.ofNullable;

public class MethodContext implements AlterableContext {
    private static final RuntimeException NOT_ACTIVE_EXCEPTION = new ContextNotActiveException("@" + WrappingMethodScoped.class.getName() + " is not active.");

    private final ThreadLocal<Map<Contextual<?>, Instance>> instances = new ThreadLocal<>();

    public Class<? extends Annotation> getScope() {
        return WrappingMethodScoped.class;
    }

    public Object execute(final SupplierWithException task) throws Exception { // inheritance is the default behavior for this scope
        Map<Contextual<?>, Instance> map = instances.get();
        final boolean clean = map == null;
        if (map == null) {
            map = new HashMap<>();
            instances.set(map);
        }
        try {
            return task.run();
        } finally {
            if (clean) {
                map.values().stream().forEach(Instance::destroy);
                map.clear();
                instances.remove();
            }
        }
    }

    @Override
    public boolean isActive() {
        final boolean initialized = instances.get() != null;
        if (!initialized) {
            instances.remove();
        }
        return initialized;
    }

    @Override
    public <T> T get(final Contextual<T> contextual, final CreationalContext<T> creationalContext) {
        checkActive();

        final Map<Contextual<?>, Instance> localMap = instances.get();
        return (T) ofNullable(localMap.get(contextual))
            .orElseGet(() -> localMap.computeIfAbsent(contextual, c -> new Instance(contextual, creationalContext)))
            .create();
    }

    @Override
    public <T> T get(final Contextual<T> contextual) {
        checkActive();
        return (T) ofNullable(instances.get().get(contextual)).map(i -> i.instance).orElse(null);
    }

    @Override
    public void destroy(final Contextual<?> contextual) {
        ofNullable(instances.get().get(contextual)).filter(c -> c.instance != null)
            .ifPresent(Instance::destroy);
    }

    private void checkActive() {
        if (!isActive()) {
            throw NOT_ACTIVE_EXCEPTION;
        }
    }

    // wrapper for an instance keeping track of the creational context and bean
    private static class Instance<T> {
        private final CreationalContext<T> context;
        private final Contextual<T> bean;

        private T instance;

        public Instance(final Contextual<T> contextual, final CreationalContext<T> context) {
            this.bean = contextual;
            this.context = context;
        }

        // no lock needed cause of the nature of the scope
        public T create() {
            if (instance == null) {
                instance = bean.create(context);
            }
            return instance;
        }

        public void destroy() {
            if (instance == null) {
                return;
            }

            bean.destroy(instance, context);
            instance = null;
        }
    }
}
