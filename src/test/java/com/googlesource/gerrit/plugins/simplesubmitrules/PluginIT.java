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

import static com.google.common.truth.Truth.assertThat;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gerrit.acceptance.LightweightPluginDaemonTest;
import com.google.gerrit.acceptance.PushOneCommit;
import com.google.gerrit.acceptance.RestResponse;
import com.google.gerrit.acceptance.TestPlugin;
import com.google.gerrit.common.RawInputUtil;
import com.google.gerrit.extensions.api.changes.ReviewInput;
import com.google.gerrit.extensions.client.Side;
import com.google.gerrit.extensions.common.ChangeInfo;
import com.google.gerrit.extensions.common.SubmitRequirementInfo;
import com.google.gerrit.extensions.restapi.RawInput;
import com.google.gerrit.reviewdb.client.Project;
import org.junit.Test;

@TestPlugin(
    name = "my-plugin",
    sysModule = "com.googlesource.gerrit.plugins.simplesubmitrules.Module")
/** Overall end-to-end integration test for configuring labels and comments and merging changes. */
public class PluginIT extends LightweightPluginDaemonTest {
  @Test
  public void singleApprovalIsSufficientByDefault() throws Exception {
    PushOneCommit.Result r = createChange();
    approve(r.getChangeId());
    ChangeInfo changeInfo = gApi.changes().id(r.getChangeId()).get();
    assertThat(changeInfo.submittable).isTrue();
    assertThat(changeInfo.requirements).isEmpty();
  }

  @Test
  public void unresolvedCommentsBlockSubmission() throws Exception {
    RawInput rawInput = configWithBlockingUnresolvedComments();
    RestResponse configResult = adminRestSession.putRaw(endpointUrl(project), rawInput);
    configResult.assertOK();

    // Create change and approve it
    PushOneCommit.Result r = createChange();
    approve(r.getChangeId());

    ReviewInput reviewInput = new ReviewInput();
    ReviewInput.CommentInput c = new ReviewInput.CommentInput();
    c.path = "a.txt";
    c.side = Side.REVISION;
    c.unresolved = true;
    c.message = "nit: double  space.";
    reviewInput.comments = ImmutableMap.of(c.path, ImmutableList.of(c));
    revision(r).review(reviewInput);

    ChangeInfo changeInfo = gApi.changes().id(r.getChangeId()).get();
    assertThat(changeInfo.submittable).isFalse();
    SubmitRequirementInfo noUnresolveComments =
        new SubmitRequirementInfo(
            "NOT_READY", "Resolve all comments", "unresolved_comments", ImmutableMap.of());
    assertThat(changeInfo.requirements).containsExactly(noUnresolveComments);
  }

  @Test
  public void authorApprovalDoesNotGrantSubmission() throws Exception {
    RawInput rawInput = configWithNoSelfApproval();
    RestResponse configResult = adminRestSession.putRaw(endpointUrl(project), rawInput);
    configResult.assertOK();

    // Create change, put an unresolved comment on it and approve it.
    PushOneCommit.Result r = createChange();
    approve(r.getChangeId());

    ChangeInfo changeInfo = gApi.changes().id(r.getChangeId()).get();
    assertThat(changeInfo.submittable).isFalse();
    assertThat(changeInfo.requirements).isEmpty();
  }

  private static RawInput configWithNoSelfApproval() {
    return RawInputUtil.create(
        ("{\n"
                + "    \"labels\": {\n"
                + "        \"Code-Review\": {\n"
                + "            \"function\": \"MaxWithBlock\",\n"
                + "            \"ignore_self_approval\": true,\n"
                + "            \"copy_scores\": [\n"
                + "                \"copyAllScoresIfNoChange\",\n"
                + "                \"copyMinScore\",\n"
                + "                \"copyAllScoresOnTrivialRebase\"\n"
                + "            ]\n"
                + "        }\n"
                + "    }\n"
                + "}")
            .getBytes(Charsets.UTF_8),
        "application/json");
  }

  private static RawInput configWithBlockingUnresolvedComments() {
    return RawInputUtil.create(
        ("{\n"
                + "    \"comments\": {\n"
                + "        \"block_if_unresolved_comments\": true\n"
                + "    }\n"
                + "}")
            .getBytes(Charsets.UTF_8),
        "application/json");
  }

  private static String endpointUrl(Project.NameKey project) {
    return "/projects/" + project.get() + "/simple-submit-rules";
  }
}
