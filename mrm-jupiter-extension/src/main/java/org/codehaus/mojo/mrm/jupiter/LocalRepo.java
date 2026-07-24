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

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Configures a locally stored Maven repository to be served by the Mock Repository Manager.
 * <p>
 * This annotation is used as an element of {@link MockRepositoryManager#localRepos()}.
 * The {@link #source()} path must point to a directory that follows standard Maven repository layout
 * (e.g. {@code groupId/artifactId/version/artifactId-version.jar}).
 * </p>
 *
 * @since 2.0
 * @see MockRepositoryManager#localRepos()
 */
@Documented
@Target({})
@Retention(RetentionPolicy.RUNTIME)
public @interface LocalRepo {

    /**
     * Path to the directory containing the local Maven repository.
     * Relative paths are resolved against the working directory.
     *
     * @return path to the repository source directory
     */
    String source();
}
