/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.weld.bootstrap.enablement;

import org.jboss.weld.bootstrap.spi.BeanDeploymentArchive;

/**
 * A beans.xml records with global effect. This is either a global enablement definition (enabled == true) or a global priority
 * setter (enabled == false).
 *
 * @author Jozef Hartinger
 *
 */
class GlobalEnablementRecord extends EnablementRecordWithPriority {

    private final boolean enabled;
    private final BeanDeploymentArchive archive;

    public GlobalEnablementRecord(String location, Class<?> enabledClass, int priority, boolean enabled,
            BeanDeploymentArchive archive) {
        super(location, enabledClass, priority);
        this.archive = archive;
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Returns the {@link BeanDeploymentArchive} which contains the beans.xml file in which this record is enabled globally.
     *
     * @return
     */
    public BeanDeploymentArchive getArchive() {
        return archive;
    }
}
