package com.github.rmannibucau.cdi.scopes.internal.method;

import com.github.rmannibucau.cdi.scopes.api.method.MethodScopeExtension;
import com.github.rmannibucau.cdi.scopes.api.method.WrappingMethodScoped;
import com.github.rmannibucau.cdi.scopes.api.method.WithScope;
import org.apache.openejb.jee.WebApp;
import org.apache.openejb.junit.ApplicationComposer;
import org.apache.openejb.testing.Classes;
import org.apache.openejb.testing.Module;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ContextNotActiveException;
import javax.inject.Inject;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(ApplicationComposer.class)
public class WithScopeExtensionTest {
    @Module
    @Classes(innerClassesAsBean = true, cdi = true)
    public WebApp app() {
        return new WebApp();
    }

    @Inject
    private ScopeUser user;

    @Inject
    private MethodScopeExtension extension;

    @Inject
    private Value value;

    @Before
    public void reset() {
        Value.GENERATOR.set(0);
    }

    @Test
    public void scopeIsOn() {
        user.scopeWorks();
    }

    @Test
    public void sameInstanceInSingleScope() {
        user.singleInstance();
    }

    @Test
    public void programmatic() {
        extension.executeInScope(() -> assertEquals(1, value.getValue()));
    }

    @Test(expected = ContextNotActiveException.class)
    public void notActive() {
        value.getValue();
    }

    public static class ScopeUser {
        @Inject
        private Value value;

        @WithScope
        public void scopeWorks() {
            assertTrue(value.getValue() > 0);
        }

        @WithScope
        public void singleInstance() {
            assertEquals(value.getValue(), value.getValue());
        }
    }

    @WrappingMethodScoped
    public static class Value {
        private static final AtomicInteger GENERATOR = new AtomicInteger();

        private int value;

        @PostConstruct
        private void init() {
            value = GENERATOR.incrementAndGet();
        }

        public int getValue() {
            return value;
        }
    }
}
