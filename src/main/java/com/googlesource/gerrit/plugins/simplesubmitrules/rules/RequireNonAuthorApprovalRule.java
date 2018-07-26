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

import com.google.common.annotations.VisibleForTesting;
import com.google.gerrit.common.data.LabelFunction;
import com.google.gerrit.common.data.LabelType;
import com.google.gerrit.common.data.SubmitRecord;
import com.google.gerrit.reviewdb.client.Account;
import com.google.gerrit.reviewdb.client.PatchSetApproval;
import com.google.gerrit.server.project.ProjectCache;
import com.google.gerrit.server.project.ProjectConfig;
import com.google.gerrit.server.project.ProjectState;
import com.google.gerrit.server.project.SubmitRuleOptions;
import com.google.gerrit.server.query.change.ChangeData;
import com.google.gerrit.server.rules.SubmitRule;
import com.google.gwtorm.server.OrmException;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.googlesource.gerrit.plugins.simplesubmitrules.SimpleSubmitRulesConfig;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.eclipse.jgit.lib.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Simple rule: require a non-author approval or block submission */
@Singleton
public class RequireNonAuthorApprovalRule implements SubmitRule {
  private static final Logger log = LoggerFactory.getLogger(RequireNonAuthorApprovalRule.class);
  private static final String E_UNABLE_TO_FETCH_CHANGE_OWNER = "Unable to fetch the change owner";
  private static final String E_UNABLE_TO_FETCH_LABELS =
      "Unable to fetch labels and approvals for the change";
  private final ProjectCache projectCache;

  @Inject
  public RequireNonAuthorApprovalRule(ProjectCache projectCache) {
    this.projectCache = projectCache;
  }

  @Override
  public Collection<SubmitRecord> evaluate(ChangeData cd, SubmitRuleOptions options) {
    ProjectState projectState = projectCache.get(cd.project());
    Config config = projectState.getConfig(ProjectConfig.PROJECT_CONFIG).getWithInheritance();

    Account.Id owner;
    try {
      owner = cd.change().getOwner();
    } catch (OrmException e) {
      log.error(E_UNABLE_TO_FETCH_CHANGE_OWNER, e);

      SubmitRecord submitRecord = new SubmitRecord();
      submitRecord.errorMessage = E_UNABLE_TO_FETCH_CHANGE_OWNER;
      submitRecord.status = SubmitRecord.Status.RULE_ERROR;
      return Collections.singletonList(submitRecord);
    }

    List<LabelType> labelTypes;
    List<PatchSetApproval> approvals;
    try {
      labelTypes = cd.getLabelTypes().getLabelTypes();
      approvals = cd.currentApprovals();
    } catch (OrmException e) {
      log.error(E_UNABLE_TO_FETCH_LABELS, e);

      SubmitRecord submitRecord = new SubmitRecord();
      submitRecord.errorMessage = E_UNABLE_TO_FETCH_LABELS;
      submitRecord.status = SubmitRecord.Status.RULE_ERROR;

      return Collections.singletonList(submitRecord);
    }

    SubmitRecord submitRecord = new SubmitRecord();
    submitRecord.status = SubmitRecord.Status.OK;
    submitRecord.labels = new ArrayList<>(labelTypes.size());

    for (LabelType t : labelTypes) {
      if (!config.getBoolean(
          t.getName(), SimpleSubmitRulesConfig.KEY_REQUIRE_NON_AUTHOR_APPROVAL, false)) {
        // The default rules are enough in this case.
        continue;
      }

      LabelFunction labelFunction = t.getFunction();
      if (labelFunction == null) {
        continue;
      }

      Collection<PatchSetApproval> approvalsForLabel = getApprovalsForLabel(approvals, t, owner);
      SubmitRecord.Label label = labelFunction.check(t, approvalsForLabel);

      switch (label.status) {
        case OK:
        case MAY:
          break;

        case NEED:
        case REJECT:
        case IMPOSSIBLE:
          submitRecord.labels.add(label);
          submitRecord.status = SubmitRecord.Status.NOT_READY;
          break;
      }
    }

    if (submitRecord.labels.isEmpty()) {
      return Collections.emptyList();
    }

    return Collections.singletonList(submitRecord);
  }

  /**
   * Returns the approvals for a given label, for everyone except from `user`, except if the vote is
   * negative.
   */
  @VisibleForTesting
  static Collection<PatchSetApproval> getApprovalsForLabel(
      Collection<PatchSetApproval> approvals, LabelType t, Account.Id user) {
    return approvals
        .stream()
        .filter(input -> input.getValue() < 0 || !input.getAccountId().equals(user))
        .filter(input -> input.getLabel().equals(t.getLabelId().get()))
        .collect(Collectors.toList());
  }
}