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

import java.util.ArrayList;
import java.util.List;

import org.codehaus.mojo.mrm.api.maven.ArtifactStore;
import org.codehaus.mojo.mrm.impl.digest.AutoDigestFileSystem;
import org.codehaus.mojo.mrm.impl.maven.ArtifactStoreFileSystem;
import org.codehaus.mojo.mrm.impl.maven.CompositeArtifactStore;
import org.codehaus.mojo.mrm.plugin.ArtifactStoreFactory;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

/**
 * JUnit Jupiter extension that starts and stops a Mock Repository Manager server around all tests in a class.
 * <p>
 * Typically used via the {@link MockRepositoryManager} annotation:
 * </p>
 * <pre>
 * {@code
 * @MockRepositoryManager
 * class MyTest {
 *     @Test
 *     void test(MockRepositoryManagerServer server) {
 *         assertThat(server.getUrl()).startsWith("http://localhost:");
 *     }
 * }
 * }
 * </pre>
 */
public class MockRepositoryManagerExtension implements BeforeAllCallback, AfterAllCallback, ParameterResolver {

    private static final ExtensionContext.Namespace NAMESPACE =
            ExtensionContext.Namespace.create(MockRepositoryManagerExtension.class);

    private static final String SERVER_KEY = "fileSystemServer";

    @Override
    public void beforeAll(ExtensionContext context) {
        MockRepositoryManager annotation = context.getRequiredTestClass().getAnnotation(MockRepositoryManager.class);
        int port = annotation != null ? annotation.port() : 0;
        String basePath = annotation != null ? annotation.basePath() : "/";
        Class<? extends ArtifactStoreFactory>[] repositoryClasses =
                annotation != null ? annotation.repositories() : new Class[0];

        ArtifactStore artifactStore = createArtifactStore(repositoryClasses);
        AutoDigestFileSystem fileSystem = new AutoDigestFileSystem(new ArtifactStoreFileSystem(artifactStore));

        FileSystemServer server = new FileSystemServer("mrm-jupiter", port, basePath, fileSystem);
        server.ensureStarted();

        context.getStore(NAMESPACE).put(SERVER_KEY, server);
    }

    @Override
    public void afterAll(ExtensionContext context) throws InterruptedException {
        FileSystemServer server = context.getStore(NAMESPACE).get(SERVER_KEY, FileSystemServer.class);
        if (server != null) {
            server.finish();
            server.waitForFinished();
        }
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        return parameterContext.getParameter().getType().equals(MockRepositoryManagerServer.class);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        FileSystemServer server = extensionContext.getStore(NAMESPACE).get(SERVER_KEY, FileSystemServer.class);
        if (server == null) {
            throw new ParameterResolutionException(
                    "MockRepositoryManagerServer is not available. Make sure the test class is annotated with @MockRepositoryManager.");
        }
        return new MockRepositoryManagerServer(server.getUrl(), server.getPort());
    }

    private ArtifactStore createArtifactStore(Class<? extends ArtifactStoreFactory>[] repositoryClasses) {
        if (repositoryClasses == null || repositoryClasses.length == 0) {
            return new CompositeArtifactStore(new ArtifactStore[0]);
        }
        List<ArtifactStore> stores = new ArrayList<>();
        for (Class<? extends ArtifactStoreFactory> factoryClass : repositoryClasses) {
            try {
                ArtifactStoreFactory factory =
                        factoryClass.getDeclaredConstructor().newInstance();
                stores.add(factory.newInstance());
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(
                        "Failed to instantiate ArtifactStoreFactory: " + factoryClass.getName(), e);
            }
        }
        ArtifactStore[] artifactStores = stores.toArray(new ArtifactStore[0]);
        return artifactStores.length == 1 ? artifactStores[0] : new CompositeArtifactStore(artifactStores);
    }
}
