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

import static com.google.common.truth.Truth.assertThat;

import com.google.gerrit.acceptance.NoHttpd;
import com.google.gerrit.acceptance.PushOneCommit;
import com.google.gerrit.common.data.SubmitRecord;
import com.google.gerrit.extensions.api.changes.ReviewInput;
import com.google.gerrit.server.project.SubmitRuleOptions;
import com.google.inject.Inject;
import com.googlesource.gerrit.plugins.simplesubmitrules.AbstractSimpleSubmitRulesIT;
import com.googlesource.gerrit.plugins.simplesubmitrules.Constants;
import java.util.Collection;
import org.junit.Before;
import org.junit.Test;

@NoHttpd
public class RequireNonAuthorApprovalRuleIT extends AbstractSimpleSubmitRulesIT {
  private static final String FILENAME = "my.file";
  @Inject private RequireNonAuthorApprovalRule rule;

  @Before
  public void enableRuleBeforeTest() throws Exception {
    enableRule("Code-Review", true);
  }

  @Test
  public void blocksWhenAutorIsOnlyApprover() throws Exception {
    PushOneCommit.Result r = createChangeWithVote(2);

    Collection<SubmitRecord> submitRecords =
        rule.evaluate(r.getChange(), SubmitRuleOptions.defaults());

    assertThat(submitRecords).hasSize(1);
    SubmitRecord result = submitRecords.iterator().next();
    assertThat(result.status).isEqualTo(SubmitRecord.Status.NOT_READY);
    assertThat(result.labels).isNotEmpty();
  }

  @Test
  public void doesNothingByDefault() throws Exception {
    PushOneCommit.Result r = createChangeWithVote(+2);

    enableRule("Code-Review", false);

    Collection<SubmitRecord> submitRecords =
        rule.evaluate(r.getChange(), SubmitRuleOptions.defaults());
    assertThat(submitRecords).isEmpty();
  }

  private PushOneCommit.Result createChangeWithVote(int value) throws Exception {
    PushOneCommit.Result r = createChange("My change", FILENAME, "new content");
    ReviewInput reviewInput = new ReviewInput();
    reviewInput.label("Code-Review", value);
    revision(r).review(reviewInput);

    return r;
  }

  private void enableRule(String labelName, boolean newState) throws Exception {
    changeProjectConfig(
        config ->
            config.setBoolean(labelName, null, Constants.REQUIRE_NON_AUTHOR_APPROVAL, newState));
  }
}
