/*
 * Copyright 2011 the original author or authors.
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

package org.gradle.api.internal.artifacts.publish.maven.deploy.mvnsettings;

import org.apache.maven.artifact.ant.InstallDeployTaskSupport;

import java.io.File;

/**
 * @author Szczepan Faber, created at: 3/29/11
 */
public class MaybeUserMavenSettingsSupplier implements MavenSettingsSupplier {

    EmptyMavenSettingsSupplier emptySettingsSupplier = new EmptyMavenSettingsSupplier();
    MavenSettingsProvider mavenSettingsProvider = new MavenSettingsProvider();

    public void supply(InstallDeployTaskSupport installDeployTaskSupport) {
        File userSettings = mavenSettingsProvider.getUserSettingsFile();
        if (userSettings.exists()) {
            installDeployTaskSupport.setSettingsFile(userSettings);
            return;
        }

        emptySettingsSupplier.supply(installDeployTaskSupport);
    }

    public void done() {
        emptySettingsSupplier.done();
    }

}
