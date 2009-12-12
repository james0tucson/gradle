/*
 * Copyright 2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gradle.api.plugins;

/**
 * <p>An {@code ObjectConfigurationAction} allows you to apply {@link org.gradle.api.Plugin}s and scripts to an object
 * or objects.</p>
 */
public interface ObjectConfigurationAction {
    /**
     * Specifies the target objects to be configured.
     *
     * @param targets The target objects.
     * @return this
     */
    ObjectConfigurationAction to(Object... targets);

    /**
     * Configures the target objects using the given script.
     *
     * @param script The script. Evaluated as for {@link org.gradle.api.Project#file(Object)}.
     * @return this
     */
    ObjectConfigurationAction script(Object script);
}