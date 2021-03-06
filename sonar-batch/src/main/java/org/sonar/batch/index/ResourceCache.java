/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.batch.index;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import org.sonar.api.BatchComponent;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.resources.Library;
import org.sonar.api.resources.Resource;

import javax.annotation.CheckForNull;

import java.util.Collection;
import java.util.Map;

public class ResourceCache implements BatchComponent {
  // resource by component key
  private final Map<String, BatchResource> resources = Maps.newHashMap();
  // dedicated cache for libraries
  private final Map<Library, BatchResource> libraries = Maps.newHashMap();

  @CheckForNull
  public BatchResource get(String componentKey) {
    return resources.get(componentKey);
  }

  @CheckForNull
  public BatchResource get(Resource resource) {
    if (!(resource instanceof Library)) {
      return resources.get(resource.getEffectiveKey());
    } else {
      return libraries.get(resource);
    }
  }

  public BatchResource add(Resource resource, Snapshot s) {
    String componentKey = resource.getEffectiveKey();
    Preconditions.checkState(!Strings.isNullOrEmpty(componentKey), "Missing resource effective key");
    Resource parentResource = resource.getParent();
    BatchResource parent = parentResource != null ? get(parentResource.getEffectiveKey()) : null;
    BatchResource batchResource = new BatchResource((long) resources.size() + 1, resource, s, parent);
    if (!(resource instanceof Library)) {
      // Libraries can have the same effective key than a project so we can't cache by effectiveKey
      resources.put(componentKey, batchResource);
    } else {
      libraries.put((Library) resource, batchResource);
    }
    return batchResource;
  }

  public Collection<BatchResource> all() {
    return resources.values();
  }
}
