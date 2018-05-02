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

import com.google.common.collect.ImmutableList;
import com.google.gerrit.common.data.SubmitRecord;
import com.google.gerrit.common.data.SubmitRecord.Status;
import com.google.gerrit.common.data.SubmitRequirement;
import com.google.gerrit.server.config.PluginConfig;
import com.google.gerrit.server.project.SubmitRuleOptions;
import com.google.gerrit.server.query.change.ChangeData;
import com.google.gerrit.server.rules.SubmitRule;
import com.google.gwtorm.server.OrmException;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.googlesource.gerrit.plugins.simplesubmitrules.SimpleSubmitRulesConfig;
import com.googlesource.gerrit.plugins.simplesubmitrules.SimpleSubmitRulesModule;
import java.util.Collection;
import java.util.Collections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Simple rule: block submission when unresolved comments are present. */
@Singleton
public class NoUnresolvedCommentsRule implements SubmitRule {
  private static final Logger log = LoggerFactory.getLogger(NoUnresolvedCommentsRule.class);
  private static final SubmitRequirement REQUIREMENT =
      SubmitRequirement.builder()
          .setType("unresolved_comments")
          .setFallbackText("Resolve all comments")
          .build();
  private final SimpleSubmitRulesModule plugin;

  @Inject
  public NoUnresolvedCommentsRule(SimpleSubmitRulesModule plugin) {
    this.plugin = plugin;
  }

  @Override
  public Collection<SubmitRecord> evaluate(ChangeData cd, SubmitRuleOptions options) {
    PluginConfig config = plugin.getConfig(cd);
    boolean blockIfUnresolvedComments =
        config.getBoolean(SimpleSubmitRulesConfig.KEY_BLOCK_IF_UNRESOLVED_COMMENTS, false);

    if (!blockIfUnresolvedComments) {
      return Collections.emptyList();
    }

    Integer unresolvedComments;
    try {
      unresolvedComments = cd.unresolvedCommentCount();
    } catch (OrmException e) {
      log.error("Error when counting unresolved comments for change {}", cd.getId(), e);

      SubmitRecord sr = new SubmitRecord();
      sr.status = Status.RULE_ERROR;
      sr.errorMessage = "Error when counting unresolved comments";
      return ImmutableList.of(sr);
    }

    SubmitRecord sr = new SubmitRecord();
    sr.requirements = Collections.singletonList(REQUIREMENT);
    if (unresolvedComments == null || unresolvedComments > 0) {
      sr.status = Status.NOT_READY;
    } else {
      sr.status = Status.OK;
    }

    return ImmutableList.of(sr);
  }
}
