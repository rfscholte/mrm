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

import javax.inject.Provider;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.io.FileUtils;
import org.codehaus.mojo.mrm.api.maven.ArtifactStore;
import org.codehaus.mojo.mrm.impl.digest.AutoDigestFileSystem;
import org.codehaus.mojo.mrm.impl.maven.ArtifactStoreFileSystem;
import org.codehaus.mojo.mrm.impl.maven.CompositeArtifactStore;
import org.codehaus.mojo.mrm.impl.maven.DiskArtifactStore;
import org.codehaus.mojo.mrm.impl.maven.MockArtifactStore;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.ear.EarArchiver;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.manager.DefaultArchiverManager;
import org.codehaus.plexus.archiver.war.WarArchiver;
import org.codehaus.plexus.archiver.zip.ZipArchiver;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.StoreScope;
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

    private static final String SERVER_RESOURCE_KEY = "serverResource";
    private static final String ANNOTATION_KEY = "annotation";

    @Override
    public void beforeAll(ExtensionContext context) {
        MockRepositoryManager annotation = context.getRequiredTestClass().getAnnotation(MockRepositoryManager.class);

        ExtensionContext.Store rootStore = context.getStore(StoreScope.EXECUTION_REQUEST, NAMESPACE);

        ServerResource existing = rootStore.get(SERVER_RESOURCE_KEY, ServerResource.class);
        if (existing == null) {
            int port = annotation != null ? annotation.port() : 0;
            String basePath = annotation != null ? annotation.basePath() : "/";
            ArtifactStore artifactStore = createArtifactStore(annotation);
            AutoDigestFileSystem fileSystem = new AutoDigestFileSystem(new ArtifactStoreFileSystem(artifactStore));
            FileSystemServer server = new FileSystemServer("mrm-jupiter", port, basePath, fileSystem);
            server.ensureStarted();
            MockRepositoryManagerServer handle = new MockRepositoryManagerServer(server.getUrl(), server.getPort());
            rootStore.put(SERVER_RESOURCE_KEY, new ServerResource(server, handle));
            rootStore.put(ANNOTATION_KEY, annotation);
        } else {
            MockRepositoryManager startedAnnotation = rootStore.get(ANNOTATION_KEY, MockRepositoryManager.class);
            // Annotation.equals() performs deep equality for all members, including array-typed members
            // (e.g. mockRepos), as required by the Java Language Specification (JLS 9.6.1).
            if (!Objects.equals(annotation, startedAnnotation)) {
                throw new ExtensionConfigurationException(
                        "A MockRepositoryManager server is already running with a different configuration. "
                                + "All test classes annotated with @MockRepositoryManager must use the same configuration.");
            }
        }
    }

    @Override
    public void afterAll(ExtensionContext context) {
        // Server lifecycle is managed at root context level via CloseableResource; nothing to do here.
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        return parameterContext.getParameter().getType().equals(MockRepositoryManagerServer.class);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        ServerResource resource = extensionContext
                .getStore(StoreScope.EXECUTION_REQUEST, NAMESPACE)
                .get(SERVER_RESOURCE_KEY, ServerResource.class);
        if (resource == null) {
            throw new ParameterResolutionException(
                    "MockRepositoryManagerServer is not available. Make sure the test class is annotated with @MockRepositoryManager.");
        }
        return resource.getHandle();
    }

    private ArtifactStore createArtifactStore(MockRepositoryManager annotation) {
        if (annotation == null) {
            return new CompositeArtifactStore(new ArtifactStore[0]);
        }

        List<ArtifactStore> stores = new ArrayList<>();

        for (MockRepo mockRepo : annotation.mockRepos()) {
            stores.add(createMockRepoStore(mockRepo));
        }
        for (LocalRepo localRepo : annotation.localRepos()) {
            stores.add(new DiskArtifactStore(new File(localRepo.source())));
        }
        for (HostedRepo hostedRepo : annotation.hostedRepos()) {
            File target = new File(hostedRepo.target());
            if (!target.isDirectory() && !target.mkdirs()) {
                throw new IllegalStateException("Failed to create hosted repository directory: " + target);
            }
            stores.add(new DiskArtifactStore(target).canWrite(true));
        }

        if (stores.isEmpty()) {
            return new CompositeArtifactStore(new ArtifactStore[0]);
        }
        ArtifactStore[] artifactStores = stores.toArray(new ArtifactStore[0]);
        return artifactStores.length == 1 ? artifactStores[0] : new CompositeArtifactStore(artifactStores);
    }

    private ArtifactStore createMockRepoStore(MockRepo mockRepo) {
        File root = new File(mockRepo.source());

        if (!mockRepo.cloneTo().isEmpty()) {
            File cloneTarget = new File(mockRepo.cloneTo());
            if (cloneTarget.exists() && mockRepo.cloneClean()) {
                try {
                    FileUtils.cleanDirectory(cloneTarget);
                } catch (IOException e) {
                    throw new IllegalStateException("Failed to clean directory: " + e.getMessage(), e);
                }
            } else if (!cloneTarget.isDirectory() && !cloneTarget.mkdirs()) {
                throw new IllegalStateException("Failed to create clone target directory: " + cloneTarget);
            }
            try {
                FileUtils.copyDirectory(root, cloneTarget);
                root = cloneTarget;
            } catch (IOException e) {
                throw new IllegalStateException("Failed to copy directory: " + e.getMessage(), e);
            }
        }

        return new MockArtifactStore(createArchiverManager(), root, mockRepo.lazyArchiver());
    }

    private static ArchiverManager createArchiverManager() {
        Map<String, Provider<Archiver>> archivers = new HashMap<>();
        archivers.put("jar", JarArchiver::new);
        archivers.put("zip", ZipArchiver::new);
        archivers.put("war", WarArchiver::new);
        archivers.put("ear", EarArchiver::new);
        return new DefaultArchiverManager(archivers, Collections.emptyMap(), Collections.emptyMap());
    }

    private static final class ServerResource implements AutoCloseable {

        private final FileSystemServer server;
        private final MockRepositoryManagerServer handle;

        ServerResource(FileSystemServer server, MockRepositoryManagerServer handle) {
            this.server = server;
            this.handle = handle;
        }

        MockRepositoryManagerServer getHandle() {
            return handle;
        }

        @Override
        public void close() throws Exception {
            try {
                server.finish();
                server.waitForFinished();
            } finally {
                handle.cleanup();
            }
        }
    }
}
