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

package com.googlesource.gerrit.plugins.simplesubmitrules.rules;

import com.google.gerrit.entities.LegacySubmitRequirement;
import com.google.gerrit.entities.SubmitRecord;
import com.google.gerrit.exceptions.StorageException;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.server.config.PluginConfig;
import com.google.gerrit.server.config.PluginConfigFactory;
import com.google.gerrit.server.project.NoSuchProjectException;
import com.google.gerrit.server.query.change.ChangeData;
import com.google.gerrit.server.rules.SubmitRule;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.googlesource.gerrit.plugins.simplesubmitrules.SimpleSubmitRulesConfig;
import java.util.Collections;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Simple rule: block submission when unresolved comments are present. */
@Singleton
public class NoUnresolvedCommentsRule implements SubmitRule {
  private static final Logger log = LoggerFactory.getLogger(NoUnresolvedCommentsRule.class);
  public static final String RULE_NAME = "No-Unresolved-Comments";
  private static final LegacySubmitRequirement REQUIREMENT =
      LegacySubmitRequirement.builder()
          .setType("unresolved_comments")
          .setFallbackText("Resolve all comments")
          .build();
  private final PluginConfigFactory pluginConfigFactory;
  private final String pluginName;

  @Inject
  public NoUnresolvedCommentsRule(
      PluginConfigFactory pluginConfigFactory, @PluginName String pluginName) {
    this.pluginConfigFactory = pluginConfigFactory;
    this.pluginName = pluginName;
  }

  @Override
  public Optional<SubmitRecord> evaluate(ChangeData cd) {
    PluginConfig config;
    try {
      config = pluginConfigFactory.getFromProjectConfig(cd.project(), pluginName);
    } catch (NoSuchProjectException e) {
      log.error("Error when fetching config of change {}'s project", cd.getId(), e);

      return error("Error when fetching configuration");
    }

    boolean ruleEnabled =
        config.getBoolean(SimpleSubmitRulesConfig.KEY_BLOCK_IF_UNRESOLVED_COMMENTS, false);

    if (!ruleEnabled) {
      return Optional.empty();
    }

    Integer unresolvedComments;
    try {
      unresolvedComments = cd.unresolvedCommentCount();
    } catch (StorageException e) {
      log.error("Error when counting unresolved comments for change {}", cd.getId(), e);

      return error("Error when counting unresolved comments");
    }

    SubmitRecord sr = new SubmitRecord();
    sr.ruleName = RULE_NAME;
    sr.requirements = Collections.singletonList(REQUIREMENT);
    sr.status =
        unresolvedComments == null || unresolvedComments > 0
            ? SubmitRecord.Status.NOT_READY
            : SubmitRecord.Status.OK;

    return Optional.of(sr);
  }

  private static Optional<SubmitRecord> error(String errorMessage) {
    SubmitRecord sr = new SubmitRecord();
    sr.status = SubmitRecord.Status.RULE_ERROR;
    sr.errorMessage = errorMessage;
    sr.ruleName = RULE_NAME;
    return Optional.of(sr);
  }
}
