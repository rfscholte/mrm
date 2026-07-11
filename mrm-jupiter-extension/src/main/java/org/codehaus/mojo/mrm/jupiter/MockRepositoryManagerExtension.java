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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.codehaus.mojo.mrm.api.maven.ArtifactStore;
import org.codehaus.mojo.mrm.impl.digest.AutoDigestFileSystem;
import org.codehaus.mojo.mrm.impl.maven.ArtifactStoreFileSystem;
import org.codehaus.mojo.mrm.impl.maven.CompositeArtifactStore;
import org.codehaus.mojo.mrm.impl.maven.DiskArtifactStore;
import org.codehaus.mojo.mrm.impl.maven.MockArtifactStore;
import org.codehaus.plexus.archiver.manager.DefaultArchiverManager;
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
    private static final String SERVER_HANDLE_KEY = "mockRepositoryManagerServer";

    @Override
    public void beforeAll(ExtensionContext context) {
        MockRepositoryManager annotation = context.getRequiredTestClass().getAnnotation(MockRepositoryManager.class);
        int port = annotation != null ? annotation.port() : 0;
        String basePath = annotation != null ? annotation.basePath() : "/";

        ArtifactStore artifactStore = createArtifactStore(annotation);
        AutoDigestFileSystem fileSystem = new AutoDigestFileSystem(new ArtifactStoreFileSystem(artifactStore));

        FileSystemServer server = new FileSystemServer("mrm-jupiter", port, basePath, fileSystem);
        server.ensureStarted();

        context.getStore(NAMESPACE).put(SERVER_KEY, server);
        getOrCreateServerHandle(context, server);
    }

    @Override
    public void afterAll(ExtensionContext context) throws InterruptedException {
        FileSystemServer server = context.getStore(NAMESPACE).get(SERVER_KEY, FileSystemServer.class);
        MockRepositoryManagerServer serverHandle =
                context.getStore(NAMESPACE).get(SERVER_HANDLE_KEY, MockRepositoryManagerServer.class);
        if (server != null) {
            server.finish();
            server.waitForFinished();
        }
        if (serverHandle != null) {
            serverHandle.cleanup();
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
        return getOrCreateServerHandle(extensionContext, server);
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

        return new MockArtifactStore(
                new DefaultArchiverManager(Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap()),
                root,
                mockRepo.lazyArchiver());
    }

    private MockRepositoryManagerServer getOrCreateServerHandle(ExtensionContext context, FileSystemServer server) {
        MockRepositoryManagerServer serverHandle =
                context.getStore(NAMESPACE).get(SERVER_HANDLE_KEY, MockRepositoryManagerServer.class);
        if (serverHandle == null) {
            serverHandle = new MockRepositoryManagerServer(server.getUrl(), server.getPort());
            context.getStore(NAMESPACE).put(SERVER_HANDLE_KEY, serverHandle);
        }
        return serverHandle;
    }
}
