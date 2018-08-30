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

package com.googlesource.gerrit.plugins.simplesubmitrules.config;

import static com.google.gerrit.server.project.testing.Util.value;

import com.google.common.base.Charsets;
import com.google.gerrit.acceptance.LightweightPluginDaemonTest;
import com.google.gerrit.acceptance.RestResponse;
import com.google.gerrit.acceptance.TestPlugin;
import com.google.gerrit.common.RawInputUtil;
import com.google.gerrit.common.data.LabelFunction;
import com.google.gerrit.extensions.restapi.RawInput;
import com.google.gerrit.reviewdb.client.Project;
import org.junit.Before;
import org.junit.Test;

@TestPlugin(
    name = "my-plugin",
    sysModule = "com.googlesource.gerrit.plugins.simplesubmitrules.Module")
public class ConfigServletIT extends LightweightPluginDaemonTest {
  @Before
  public void setUp() throws Exception {
    configLabel(
        project,
        "Code-Review",
        LabelFunction.MAX_WITH_BLOCK,
        value(1, "Passes"),
        value(0, "No score"),
        value(-1, "Failed"));
  }

  @Test
  public void adminCanFetchConfig() throws Exception {
    RestResponse r = adminRestSession.getJsonAccept(endpointUrl(project));
    r.assertOK();
  }

  @Test
  public void adminCanModifyConfig() throws Exception {
    RawInput rawInput = createConfig();
    RestResponse r = adminRestSession.putRaw(endpointUrl(project), rawInput);
    r.assertOK();
  }

  @Test
  public void userCanNotFetchConfig() throws Exception {
    RestResponse r = userRestSession.getJsonAccept(endpointUrl(project));
    r.assertForbidden();
  }

  @Test
  public void userCanNotModifyConfig() throws Exception {
    RawInput rawInput = createConfig();
    RestResponse r = userRestSession.putRaw(endpointUrl(project), rawInput);
    r.assertForbidden();
  }

  private static RawInput createConfig() {
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
                + "    },\n"
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
