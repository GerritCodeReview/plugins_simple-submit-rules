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
import static com.google.common.truth.Truth8.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gerrit.acceptance.LightweightPluginDaemonTest;
import com.google.gerrit.acceptance.NoHttpd;
import com.google.gerrit.acceptance.PushOneCommit;
import com.google.gerrit.acceptance.TestPlugin;
import com.google.gerrit.entities.SubmitRecord;
import com.google.gerrit.extensions.api.changes.ReviewInput;
import com.google.gerrit.extensions.client.Side;
import com.google.gerrit.server.query.change.ChangeData;
import com.googlesource.gerrit.plugins.simplesubmitrules.SimpleSubmitRulesConfig;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;

@TestPlugin(
    name = "my-plugin",
    sysModule = "com.googlesource.gerrit.plugins.simplesubmitrules.Module")
@NoHttpd
public class NoUnresolvedCommentsRuleIT extends LightweightPluginDaemonTest {
  private static final String FILENAME = "my.file";

  @Before
  public void enableRuleBeforeTest() throws Exception {
    enableRule(true);
  }

  @Test
  public void blocksWithUnresolvedComments() throws Exception {
    ReviewInput.CommentInput comment = newFileComment();
    comment.unresolved = true;
    PushOneCommit.Result r = createChangeWithComment(comment);

    Optional<SubmitRecord> submitRecords = evaluate(r.getChange());

    assertThat(submitRecords).isPresent();
    SubmitRecord result = submitRecords.get();
    assertThat(result.status).isEqualTo(SubmitRecord.Status.NOT_READY);
    assertThat(result.labels).isNull();
    assertThat(result.requirements).hasSize(1);
  }

  @Test
  public void doesNotBlockWithNoComments() throws Exception {
    ReviewInput.CommentInput comment = newFileComment();
    comment.unresolved = false;
    PushOneCommit.Result r = createChangeWithComment(comment);

    Optional<SubmitRecord> submitRecords = evaluate(r.getChange());

    assertThat(submitRecords).isPresent();
    SubmitRecord result = submitRecords.get();
    assertThat(result.status).isEqualTo(SubmitRecord.Status.OK);
    assertThat(result.labels).isNull();
    assertThat(result.requirements).hasSize(1);
  }

  @Test
  public void doesNotBlockWithOnlyResolvedComments() throws Exception {
    PushOneCommit.Result change = createChange("refs/for/master");

    Optional<SubmitRecord> submitRecords = evaluate(change.getChange());

    assertThat(submitRecords).isPresent();
    SubmitRecord result = submitRecords.get();
    assertThat(result.status).isEqualTo(SubmitRecord.Status.OK);
    assertThat(result.labels).isNull();
    assertThat(result.requirements).hasSize(1);
  }

  @Test
  public void doesNothingByDefault() throws Exception {
    ReviewInput.CommentInput comment = newFileComment();
    comment.unresolved = true;

    PushOneCommit.Result r = createChangeWithComment(comment);

    enableRule(false);

    Optional<SubmitRecord> submitRecords = evaluate(r.getChange());
    assertThat(submitRecords).isEmpty();
  }

  private PushOneCommit.Result createChangeWithComment(ReviewInput.CommentInput comment)
      throws Exception {
    PushOneCommit.Result r = createChange("My change", FILENAME, "new content");
    ReviewInput reviewInput = new ReviewInput();
    reviewInput.comments = ImmutableMap.of(comment.path, ImmutableList.of(comment));
    revision(r).review(reviewInput);

    return r;
  }

  private void enableRule(boolean newState) throws Exception {
    try (ProjectConfigUpdate u = updateProject(project)) {
      u.getConfig()
          .updatePluginConfig(
              plugin.getName(),
              cfg ->
                  cfg.setBoolean(
                      SimpleSubmitRulesConfig.KEY_BLOCK_IF_UNRESOLVED_COMMENTS, newState));
      u.save();
    }
  }

  private Optional<SubmitRecord> evaluate(ChangeData cd) {
    NoUnresolvedCommentsRule rule =
        plugin.getSysInjector().getInstance(NoUnresolvedCommentsRule.class);

    return rule.evaluate(cd);
  }

  private static ReviewInput.CommentInput newFileComment() {
    ReviewInput.CommentInput c = new ReviewInput.CommentInput();
    c.path = FILENAME;
    c.side = Side.REVISION;
    c.message = "nit: double  space.";
    return c;
  }
}
