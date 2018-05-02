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

package com.googlesource.gerrit.plugins.simplesubmitrules;

import com.google.gerrit.extensions.registration.DynamicSet;
import com.google.gerrit.extensions.restapi.RestApiModule;
import com.google.gerrit.server.project.ProjectResource;
import com.google.gerrit.server.rules.SubmitRule;
import com.google.inject.AbstractModule;
import com.googlesource.gerrit.plugins.simplesubmitrules.config.ConfigServlet;
import com.googlesource.gerrit.plugins.simplesubmitrules.config.ConfigTranslator;
import com.googlesource.gerrit.plugins.simplesubmitrules.rules.NoUnresolvedCommentsRule;
import com.googlesource.gerrit.plugins.simplesubmitrules.rules.RequireNonAuthorApprovalRule;

/** Bootstraps the Simple Submit Rules plugin */
public class SimpleSubmitRulesModule extends AbstractModule {
  private static final String API_ENDPOINT = "simple-submit-rules";

  @Override
  protected void configure() {
    bind(ConfigTranslator.class);
    bind(SimpleSubmitRulesConfig.class);

    install(
        new RestApiModule() {
          @Override
          protected void configure() {
            get(ProjectResource.PROJECT_KIND, API_ENDPOINT).to(ConfigServlet.class);
            put(ProjectResource.PROJECT_KIND, API_ENDPOINT).to(ConfigServlet.class);
          }
        });

    DynamicSet.bind(binder(), SubmitRule.class).to(RequireNonAuthorApprovalRule.class);
    DynamicSet.bind(binder(), SubmitRule.class).to(NoUnresolvedCommentsRule.class);
  }
}
