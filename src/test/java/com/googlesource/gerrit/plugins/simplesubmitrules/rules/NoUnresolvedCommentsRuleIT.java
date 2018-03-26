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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gerrit.acceptance.NoHttpd;
import com.google.gerrit.acceptance.PushOneCommit;
import com.google.gerrit.common.data.SubmitRecord;
import com.google.gerrit.extensions.api.changes.ReviewInput;
import com.google.gerrit.extensions.api.changes.ReviewInput.CommentInput;
import com.google.gerrit.extensions.client.Side;
import com.google.gerrit.server.project.SubmitRuleOptions;
import com.google.inject.Inject;
import com.googlesource.gerrit.plugins.simplesubmitrules.AbstractDaemonTestWithPlugin;
import com.googlesource.gerrit.plugins.simplesubmitrules.Constants;
import java.util.Collection;
import org.junit.Before;
import org.junit.Test;

@NoHttpd
public class NoUnresolvedCommentsRuleIT extends AbstractDaemonTestWithPlugin {
  private static final String FILENAME = "my.file";
  @Inject private NoUnresolvedCommentsRule rule;

  @Before
  public void enableRuleBeforeTest() throws Exception {
    enableRule(true);
  }

  @Test
  public void blocksWithUnresolvedComments() throws Exception {
    CommentInput comment = newFileComment();
    comment.unresolved = true;
    PushOneCommit.Result r = createChangeWithComment(comment);

    Collection<SubmitRecord> submitRecords =
        rule.evaluate(r.getChange(), SubmitRuleOptions.defaults());

    assertThat(submitRecords).hasSize(1);
    SubmitRecord result = submitRecords.iterator().next();
    assertThat(result.status).isEqualTo(SubmitRecord.Status.NOT_READY);
    assertThat(result.labels).isNull();
    assertThat(result.requirements).hasSize(1);
  }

  @Test
  public void doesNotBlockWithNoComments() throws Exception {
    CommentInput comment = newFileComment();
    comment.unresolved = false;
    PushOneCommit.Result r = createChangeWithComment(comment);

    Collection<SubmitRecord> submitRecords =
        rule.evaluate(r.getChange(), SubmitRuleOptions.defaults());

    assertThat(submitRecords).hasSize(1);
    SubmitRecord result = submitRecords.iterator().next();
    assertThat(result.status).isEqualTo(SubmitRecord.Status.OK);
    assertThat(result.labels).isNull();
    assertThat(result.requirements).hasSize(1);
  }

  @Test
  public void doesNotBlockWithOnlyResolvedComments() throws Exception {
    PushOneCommit.Result change = createChange("refs/for/master");

    Collection<SubmitRecord> submitRecords =
        rule.evaluate(change.getChange(), SubmitRuleOptions.defaults());

    assertThat(submitRecords).hasSize(1);
    SubmitRecord result = submitRecords.iterator().next();
    assertThat(result.status).isEqualTo(SubmitRecord.Status.OK);
    assertThat(result.labels).isNull();
    assertThat(result.requirements).hasSize(1);
  }

  @Test
  public void doesNothingByDefault() throws Exception {
    CommentInput comment = newFileComment();
    comment.unresolved = true;

    PushOneCommit.Result r = createChangeWithComment(comment);

    enableRule(false);

    Collection<SubmitRecord> submitRecords =
        rule.evaluate(r.getChange(), SubmitRuleOptions.defaults());
    assertThat(submitRecords).isEmpty();
  }

  private PushOneCommit.Result createChangeWithComment(CommentInput comment) throws Exception {
    PushOneCommit.Result r = createChange("My change", FILENAME, "new content");
    ReviewInput reviewInput = new ReviewInput();
    reviewInput.comments = ImmutableMap.of(comment.path, ImmutableList.of(comment));
    revision(r).review(reviewInput);

    return r;
  }

  private void enableRule(boolean newState) throws Exception {
    changeProjectConfig(
        config ->
            config.setBoolean(
                "plugin", Constants.PLUGIN_NAME, Constants.BLOCK_IF_UNRESOLVED_COMMENTS, newState));
  }

  private static CommentInput newFileComment() {
    CommentInput c = new CommentInput();
    c.path = FILENAME;
    c.side = Side.REVISION;
    c.message = "nit: double  space.";
    return c;
  }
}
