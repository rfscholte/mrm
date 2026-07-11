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

import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Annotation to start a Mock Repository Manager server for a JUnit Jupiter test class.
 * <p>
 * When applied to a test class, the server is started before all tests in the class and stopped after all tests.
 * Test methods may declare a {@link MockRepositoryManagerServer} parameter to receive a handle to the running server.
 * </p>
 * <p>
 * Repositories are configured via the typed annotation attributes {@link #mockRepos()},
 * {@link #localRepos()}, and {@link #hostedRepos()}. When no repositories are specified the server
 * starts with an empty store.
 * </p>
 *
 * <pre>
 * {@code
 * @MockRepositoryManager(localRepos = @LocalRepo(source = "src/test/mrm"))
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
     * Mock repositories whose artifact content is derived from POM files in the source directory.
     *
     * @return the mock repository configurations
     * @see MockRepo
     */
    MockRepo[] mockRepos() default {};

    /**
     * Locally stored Maven repositories served read-only from a directory on disk.
     *
     * @return the local repository configurations
     * @see LocalRepo
     */
    LocalRepo[] localRepos() default {};

    /**
     * Hosted repositories that accept artifact uploads (distribution management).
     *
     * @return the hosted repository configurations
     * @see HostedRepo
     */
    HostedRepo[] hostedRepos() default {};
}
