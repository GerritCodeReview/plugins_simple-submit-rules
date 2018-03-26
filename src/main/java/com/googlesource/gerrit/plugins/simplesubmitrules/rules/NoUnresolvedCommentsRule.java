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

import com.google.gerrit.common.data.SubmitRecord;
import com.google.gerrit.common.data.SubmitRecord.Status;
import com.google.gerrit.common.data.SubmitRequirement;
import com.google.gerrit.server.config.PluginConfig;
import com.google.gerrit.server.project.SubmitRuleOptions;
import com.google.gerrit.server.query.change.ChangeData;
import com.google.gerrit.server.rules.SubmitRule;
import com.google.gwtorm.server.OrmException;
import com.google.inject.Inject;
import com.googlesource.gerrit.plugins.simplesubmitrules.Constants;
import com.googlesource.gerrit.plugins.simplesubmitrules.EasyPreSubmitModule;
import java.util.Collection;
import java.util.Collections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NoUnresolvedCommentsRule implements SubmitRule {
  private static final Logger log = LoggerFactory.getLogger(NoUnresolvedCommentsRule.class);
  private final EasyPreSubmitModule plugin;

  @Inject
  public NoUnresolvedCommentsRule(EasyPreSubmitModule plugin) {
    this.plugin = plugin;
  }

  @Override
  public Collection<SubmitRecord> evaluate(ChangeData changeData, SubmitRuleOptions options) {
    PluginConfig config = plugin.getConfig(changeData);
    boolean blockIfUnresolvedComments =
        config.getBoolean(Constants.BLOCK_IF_UNRESOLVED_COMMENTS, false);
    if (!blockIfUnresolvedComments) {
      return Collections.emptyList();
    }

    Integer unresolvedComments = null;
    try {
      unresolvedComments = changeData.unresolvedCommentCount();
    } catch (OrmException e) {
      log.error("Error when counting unresolved comments", e);
    }

    if (unresolvedComments == null || unresolvedComments <= 0) {
      return Collections.emptyList();
    }

    SubmitRecord sr = new SubmitRecord();
    sr.status = Status.NOT_READY;
    SubmitRequirement req =
        new SubmitRequirement(
            "Unresolved comments", "Resolve all comments before submitting.", null);
    sr.requirements = Collections.singletonList(req);
    return Collections.singletonList(sr);
  }
}
