// Copyright (C) 2018 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.googlesource.gerrit.plugins.simplesubmitrules.config;

import com.google.gerrit.extensions.restapi.AuthException;
import com.google.gerrit.extensions.restapi.BadRequestException;
import com.google.gerrit.extensions.restapi.RestModifyView;
import com.google.gerrit.extensions.restapi.RestReadView;
import com.google.gerrit.server.IdentifiedUser;
import com.google.gerrit.server.git.meta.MetaDataUpdate;
import com.google.gerrit.server.permissions.PermissionBackend;
import com.google.gerrit.server.permissions.PermissionBackendException;
import com.google.gerrit.server.permissions.ProjectPermission;
import com.google.gerrit.server.project.ProjectCache;
import com.google.gerrit.server.project.ProjectResource;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.googlesource.gerrit.plugins.simplesubmitrules.api.SubmitConfig;
import java.io.IOException;

/** REST Endpoint to configure labels and our simple submit rules */
@Singleton
public class ConfigServlet
    implements RestReadView<ProjectResource>, RestModifyView<ProjectResource, SubmitConfig> {
  private final ProjectCache projectCache;
  private final PermissionBackend permissionBackend;
  private final MetaDataUpdate.User metaDataUpdateFactory;
  private final ConfigTranslator configTranslator;

  @Inject
  ConfigServlet(
      ProjectCache projectCache,
      PermissionBackend permissionBackend,
      MetaDataUpdate.User metaDataUpdateFactory,
      ConfigTranslator configTranslator) {
    this.projectCache = projectCache;
    this.permissionBackend = permissionBackend;
    this.metaDataUpdateFactory = metaDataUpdateFactory;
    this.configTranslator = configTranslator;
  }

  @Override
  public Object apply(ProjectResource resource) throws AuthException, PermissionBackendException {
    permissionBackend
        .user(resource.getUser())
        .project(resource.getNameKey())
        .check(ProjectPermission.READ_CONFIG);

    return configTranslator.convertFrom(resource.getProjectState());
  }

  @Override
  public Object apply(ProjectResource resource, SubmitConfig inConfig)
      throws PermissionBackendException, AuthException, BadRequestException, IOException {

    permissionBackend
        .user(resource.getUser())
        .project(resource.getNameKey())
        .check(ProjectPermission.WRITE_CONFIG);

    IdentifiedUser user = resource.getUser().asIdentifiedUser();
    try (MetaDataUpdate md = metaDataUpdateFactory.create(resource.getNameKey(), user)) {
      configTranslator.applyTo(inConfig, resource.getProjectState());
      resource.getProjectState().getConfig().commit(md);
      projectCache.evict(resource.getNameKey());
    }

    return configTranslator.convertFrom(resource.getProjectState());
  }
}
