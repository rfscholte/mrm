package org.codehaus.mojo.mrm.jupiter;

/*
 * Copyright 2011 Stephen Connolly
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.StoreScope;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the singleton-per-run behaviour of {@link MockRepositoryManagerExtension}.
 */
class MockRepositoryManagerExtensionSingletonTest {

    /** Inner class carrying a default (empty) {@code @MockRepositoryManager} annotation. */
    @MockRepositoryManager
    static class DefaultConfigTestClass {}

    /** Inner class carrying a {@code @MockRepositoryManager} annotation with a different basePath. */
    @MockRepositoryManager(basePath = "/different")
    static class DifferentBasePathTestClass {}

    private final Map<Object, Object> rootStoreData = new HashMap<>();
    private final ExtensionContext.Store rootStore = createMapBackedStore(rootStoreData);

    @AfterEach
    void cleanup() throws Exception {
        Object resource = rootStoreData.get("serverResource");
        if (resource instanceof AutoCloseable) {
            ((AutoCloseable) resource).close();
        }
    }

    @Test
    void secondCallWithSameAnnotationReusesServer() throws Exception {
        MockRepositoryManagerExtension extension = new MockRepositoryManagerExtension();

        ExtensionContext ctx1 = createContextForClass(DefaultConfigTestClass.class);
        ExtensionContext ctx2 = createContextForClass(DefaultConfigTestClass.class);

        extension.beforeAll(ctx1);
        extension.beforeAll(ctx2);

        // Both contexts should resolve to the same server handle
        Object handle1 = rootStoreData.get("serverResource");
        Object handle2 = rootStoreData.get("serverResource");
        assertSame(handle1, handle2, "The same ServerResource instance should be reused for compatible annotations");
    }

    @Test
    void secondCallWithDifferentAnnotationThrows() throws Exception {
        MockRepositoryManagerExtension extension = new MockRepositoryManagerExtension();

        ExtensionContext ctx1 = createContextForClass(DefaultConfigTestClass.class);
        ExtensionContext ctx2 = createContextForClass(DifferentBasePathTestClass.class);

        extension.beforeAll(ctx1);

        assertThrows(
                ExtensionConfigurationException.class,
                () -> extension.beforeAll(ctx2),
                "Expected ExtensionConfigurationException when a conflicting @MockRepositoryManager configuration is used");
    }

    // ---- helpers ----

    private ExtensionContext createContextForClass(Class<?> testClass) {
        ExtensionContext ctx = mock(ExtensionContext.class);
        doReturn(testClass).when(ctx).getRequiredTestClass();
        when(ctx.getStore(eq(StoreScope.EXECUTION_REQUEST), any())).thenReturn(rootStore);
        return ctx;
    }

    private ExtensionContext.Store createMapBackedStore(Map<Object, Object> data) {
        ExtensionContext.Store store = mock(ExtensionContext.Store.class);
        doAnswer(inv -> {
                    data.put(inv.getArgument(0), inv.getArgument(1));
                    return null;
                })
                .when(store)
                .put(any(), any());
        doAnswer(inv -> {
                    Class<?> type = inv.getArgument(1);
                    return type.cast(data.get(inv.getArgument(0)));
                })
                .when(store)
                .get(any(), any(Class.class));
        return store;
    }
}
