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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.codehaus.mojo.mrm.plugin.ArtifactStoreFactory;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Annotation to start a Mock Repository Manager server for a JUnit Jupiter test class.
 * <p>
 * When applied to a test class, the server is started before all tests in the class and stopped after all tests.
 * Test methods may declare a {@link MockRepositoryManagerServer} parameter to receive a handle to the running server.
 * </p>
 * <p>
 * The {@link #repositories()} attribute accepts {@link ArtifactStoreFactory} implementation classes that will be
 * instantiated (using their no-arg constructor) and used to populate the mock repository. When no repositories are
 * specified the server starts with an empty store.
 * </p>
 *
 * <pre>
 * {@code
 * @MockRepositoryManager(repositories = MyArtifactStoreFactory.class)
 * class MyTest {
 *     @Test
 *     void test(MockRepositoryManagerServer server) {
 *         String url = server.getUrl();
 *         // ...
 *     }
 * }
 * }
 * </pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(MockRepositoryManagerExtension.class)
public @interface MockRepositoryManager {

    /**
     * The port to start the server on. Defaults to {@code 0} which means a random available port will be chosen.
     *
     * @return the port number
     */
    int port() default 0;

    /**
     * The base context path for the server. Defaults to {@code "/"}.
     *
     * @return the context path
     */
    String basePath() default "/";

    /**
     * The {@link ArtifactStoreFactory} classes that define the mock artifacts served by the repository.
     * Each class must have a public no-arg constructor. When no repositories are specified the server starts
     * with an empty artifact store.
     *
     * @return the artifact store factory classes
     */
    Class<? extends ArtifactStoreFactory>[] repositories() default {};
}
