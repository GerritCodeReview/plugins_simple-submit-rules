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

import com.google.gerrit.acceptance.GitUtil;
import com.google.gerrit.acceptance.LightweightPluginDaemonTest;
import com.google.gerrit.acceptance.PushOneCommit;
import com.google.gerrit.acceptance.TestPlugin;
import com.google.gerrit.reviewdb.client.RefNames;
import com.google.gerrit.server.project.ProjectConfig;
import java.util.function.Consumer;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.junit.TestRepository;
import org.eclipse.jgit.lib.Config;
import org.junit.Ignore;

/** Base class used by IT tests, loads the Simple Submit Rules plugin. */
@TestPlugin(
    name = "my-plugin",
    sysModule = "com.googlesource.gerrit.plugins.simplesubmitrules.Module")
@Ignore
public abstract class AbstractSimpleSubmitRulesIT extends LightweightPluginDaemonTest {

  /** Helper method to change the project.config file using a provided consumer. */
  protected void changeProjectConfig(Consumer<Config> callback) throws Exception {
    TestRepository<InMemoryRepository> projectRepo = cloneProject(project, admin);
    // Fetch permission ref
    GitUtil.fetch(projectRepo, "refs/meta/config:cfg");
    projectRepo.reset("cfg");

    String rawConfig =
        gApi.projects()
            .name(project.get())
            .branch(RefNames.REFS_CONFIG)
            .file(ProjectConfig.PROJECT_CONFIG)
            .asString();

    Config config = new Config();
    config.fromText(rawConfig);

    // Apply our custom function to the config
    callback.accept(config);

    rawConfig = config.toText();

    PushOneCommit push =
        pushFactory.create(
            db, admin.getIdent(), projectRepo, "Subject", ProjectConfig.PROJECT_CONFIG, rawConfig);
    push.to(RefNames.REFS_CONFIG).assertOkStatus();
  }
}
