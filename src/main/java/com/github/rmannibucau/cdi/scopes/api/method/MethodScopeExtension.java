package com.github.rmannibucau.cdi.scopes.api.method;

import com.github.rmannibucau.cdi.scopes.internal.method.MethodContext;

import javax.enterprise.context.Dependent;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.InterceptionType;
import javax.enterprise.inject.spi.Interceptor;
import javax.interceptor.InvocationContext;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Set;
import java.util.function.Supplier;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;

public class MethodScopeExtension implements Extension {
    private final MethodContext methodContext = new MethodContext();

    void addScope(@Observes final BeforeBeanDiscovery beforeBeanDiscovery) {
        beforeBeanDiscovery.addScope(WrappingMethodScoped.class, true, false);
        beforeBeanDiscovery.addInterceptorBinding(WithScope.class);
    }

    void addContext(@Observes final AfterBeanDiscovery afterBeanDiscovery) {
        afterBeanDiscovery.addContext(methodContext);

        afterBeanDiscovery.addBean(new Interceptor<Object>() { // fake an interceptor but impl is in intercept()
            public final Set<Type> types = singleton(Object.class);
            public final Set<Annotation> bindings = singleton(new WithScope() {
                @Override
                public Class<? extends Annotation> annotationType() {
                    return WithScope.class;
                }
            });

            @Override
            public Set<Annotation> getInterceptorBindings() {
                return bindings;
            }

            @Override
            public boolean intercepts(final InterceptionType type) {
                return type == InterceptionType.AROUND_INVOKE;
            }

            @Override
            public Object intercept(final InterceptionType type, final Object instance, final InvocationContext ctx) throws Exception {
                return methodContext.execute(ctx::proceed);
            }

            @Override
            public Set<InjectionPoint> getInjectionPoints() {
                return emptySet();
            }

            @Override
            public Class<?> getBeanClass() {
                return Object.class;
            }

            @Override
            public boolean isNullable() {
                return false;
            }

            @Override
            public Set<Type> getTypes() {
                return types;
            }

            @Override
            public Set<Annotation> getQualifiers() {
                return emptySet();
            }

            @Override
            public Class<? extends Annotation> getScope() {
                return Dependent.class;
            }

            @Override
            public String getName() {
                return null;
            }

            @Override
            public Set<Class<? extends Annotation>> getStereotypes() {
                return emptySet();
            }

            @Override
            public boolean isAlternative() {
                return false;
            }

            @Override
            public Object create(final CreationalContext<Object> context) {
                return this; // ignored anyway
            }

            @Override
            public void destroy(final Object instance, final CreationalContext<Object> context) {
                // no-op
            }
        });
    }

    public void executeInScope(final Runnable runnable) {
        executeInScope(() -> { runnable.run(); return null; });
    }

    public <T> T executeInScope(final Supplier<T> task) {
        try {
            return (T) methodContext.execute(() -> task.get());
        } catch (final Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
