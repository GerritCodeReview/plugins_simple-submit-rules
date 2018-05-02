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

import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.server.config.PluginConfig;
import com.google.gerrit.server.config.PluginConfigFactory;
import com.google.gerrit.server.project.NoSuchProjectException;
import com.google.gerrit.server.query.change.ChangeData;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SimpleSubmitRulesConfig {
  private static final Logger log = LoggerFactory.getLogger(SimpleSubmitRulesConfig.class);
  public static final String KEY_REQUIRE_NON_AUTHOR_APPROVAL = "nonAuthorApprovalRequired";
  public static final String KEY_BLOCK_IF_UNRESOLVED_COMMENTS = "blockIfUnresolvedComments";

  private final PluginConfigFactory pluginConfigFactory;
  private final String pluginName;

  @Inject
  public SimpleSubmitRulesConfig(
      PluginConfigFactory pluginConfigFactory, @PluginName String pluginName) {
    this.pluginConfigFactory = pluginConfigFactory;
    this.pluginName = pluginName;
  }

  public PluginConfig getConfig(ChangeData changeData) {
    try {
      return pluginConfigFactory.getFromProjectConfig(changeData.project(), pluginName);
    } catch (NoSuchProjectException e) {
      log.error("Could not load plugin configuration", e);
      return null;
    }
  }
}
