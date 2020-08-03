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
import com.google.common.collect.ImmutableSet;
import com.google.gerrit.acceptance.LightweightPluginDaemonTest;
import com.google.gerrit.acceptance.PushOneCommit;
import com.google.gerrit.acceptance.RestResponse;
import com.google.gerrit.acceptance.TestPlugin;
import com.google.gerrit.common.RawInputUtil;
import com.google.gerrit.entities.CachedProjectConfig;
import com.google.gerrit.entities.LabelFunction;
import com.google.gerrit.entities.LabelType;
import com.google.gerrit.entities.Project;
import com.google.gerrit.extensions.api.changes.ReviewInput;
import com.google.gerrit.extensions.client.Side;
import com.google.gerrit.extensions.common.ChangeInfo;
import com.google.gerrit.extensions.common.SubmitRequirementInfo;
import com.google.gerrit.extensions.restapi.RawInput;
import com.googlesource.gerrit.plugins.simplesubmitrules.api.CommentsRules;
import com.googlesource.gerrit.plugins.simplesubmitrules.api.LabelDefinition;
import com.googlesource.gerrit.plugins.simplesubmitrules.api.SubmitConfig;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.junit.TestRepository;
import org.junit.Test;

@TestPlugin(
    name = "my-plugin",
    sysModule = "com.googlesource.gerrit.plugins.simplesubmitrules.Module")
/** Overall end-to-end integration test for configuring labels and comments and merging changes. */
public class PluginIT extends LightweightPluginDaemonTest {
  private static final String JSON_TYPE = "application/json";

  @Test
  public void singleApprovalIsSufficientByDefault() throws Exception {
    PushOneCommit.Result r = createChange();
    approve(r.getChangeId());
    ChangeInfo changeInfo = gApi.changes().id(r.getChangeId()).get();
    assertThat(changeInfo.submittable).isTrue();
    assertThat(changeInfo.requirements).isEmpty();
  }

  @Test
  public void unresolvedCommentsBlockSubmissionIfConfigured() throws Exception {
    SubmitConfig config = new SubmitConfig(null, new CommentsRules(true));
    postConfig(project, config);

    // Create change as user
    TestRepository<InMemoryRepository> userTestRepo = cloneProject(project, user);
    PushOneCommit push = pushFactory.create(user.newIdent(), userTestRepo);
    PushOneCommit.Result r = push.to("refs/for/master");

    // Approve as admin
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
        new SubmitRequirementInfo("NOT_READY", "Resolve all comments", "unresolved_comments");
    assertThat(changeInfo.requirements).containsExactly(noUnresolveComments);
  }

  @Test
  public void uploaderApprovalDoesNotGrantSubmissionIfConfigured() throws Exception {
    LabelDefinition codeReviewNoSelfApproval = new LabelDefinition("MaxWithBlock", true, null);
    SubmitConfig config =
        new SubmitConfig(ImmutableMap.of("Code-Review", codeReviewNoSelfApproval), null);
    postConfig(project, config);

    // Create change, put an unresolved comment on it and approve it.
    PushOneCommit.Result r = createChange();
    approve(r.getChangeId());

    ChangeInfo changeInfo = gApi.changes().id(r.getChangeId()).get();
    assertThat(changeInfo.submittable).isFalse();
    SubmitRequirementInfo noSelfApproval =
        new SubmitRequirementInfo(
            "NOT_READY", "Approval from non-uploader required", "non_uploader_approval");
    assertThat(changeInfo.requirements).containsExactly(noSelfApproval);
  }

  @Test
  public void labelConfigsGetPersisted() throws Exception {
    LabelDefinition codeReviewNoSelfApproval =
        new LabelDefinition("MaxNoBlock", true, ImmutableSet.of("copyAllScoresIfNoChange"));
    SubmitConfig config =
        new SubmitConfig(ImmutableMap.of("Code-Review", codeReviewNoSelfApproval), null);
    postConfig(project, config);

    String currentConfig = adminRestSession.get(endpointUrl(project)).getEntityContent();
    SubmitConfig parsedConfig = newGson().fromJson(currentConfig, SubmitConfig.class);
    assertThat(parsedConfig.labels)
        .isEqualTo(ImmutableMap.of("Code-Review", codeReviewNoSelfApproval));
  }

  @Test
  public void commentsConfigGetPersisted() throws Exception {
    SubmitConfig config = new SubmitConfig(null, new CommentsRules(true));
    postConfig(project, config);

    String currentConfig = adminRestSession.get(endpointUrl(project)).getEntityContent();
    SubmitConfig parsedConfig = newGson().fromJson(currentConfig, SubmitConfig.class);
    assertThat(parsedConfig.comments).isEqualTo(new CommentsRules(true));
  }

  @Test
  public void pluginPersistsLabelInCurrentProjectWhenOverrideIsNeeded() throws Exception {
    LabelDefinition codeReview = new LabelDefinition("MaxNoBlock", false, null);
    SubmitConfig config = new SubmitConfig(ImmutableMap.of("Code-Review", codeReview), null);
    postConfig(project, config);

    LabelType localCR =
        projectCache.get(project).get().getConfig().getLabelSections().get("Code-Review");
    assertThat(localCR.getFunction()).isEqualTo(LabelFunction.MAX_NO_BLOCK);

    // Check that the label has the same configs besides the function, which we changed
    LabelType allProjectsCR = projectCache.getAllProjects().getLabelTypes().byLabel("Code-Review");
    assertThat(localCR.toBuilder().setFunction(allProjectsCR.getFunction()).build()).isEqualTo(allProjectsCR);
  }

  @Test
  public void replyContainsUpdatedConfig() throws Exception {
    LabelDefinition codeReviewNoSelfApproval = new LabelDefinition(null, true, null);
    SubmitConfig config =
        new SubmitConfig(ImmutableMap.of("Code-Review", codeReviewNoSelfApproval), null);
    SubmitConfig response = postConfig(project, config);
    assertThat(response.labels.keySet()).containsExactly("Code-Review");
    assertThat(response.labels.get("Code-Review").ignoreSelfApproval).isTrue();
  }

  private SubmitConfig postConfig(Project.NameKey project, SubmitConfig config) throws Exception {
    RawInput rawInput =
        RawInputUtil.create(newGson().toJson(config).getBytes(Charsets.UTF_8), JSON_TYPE);
    RestResponse configResult = adminRestSession.putRaw(endpointUrl(project), rawInput);
    configResult.assertOK();
    return newGson().fromJson(configResult.getEntityContent(), SubmitConfig.class);
  }

  private static String endpointUrl(Project.NameKey project) {
    return "/projects/" + project.get() + "/simple-submit-rules";
  }
}
