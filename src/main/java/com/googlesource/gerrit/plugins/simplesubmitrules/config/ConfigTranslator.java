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

import com.google.gerrit.common.data.LabelType;
import com.google.gerrit.common.data.LabelTypes;
import com.google.gerrit.extensions.restapi.BadRequestException;
import com.google.gerrit.server.config.PluginConfig;
import com.google.gerrit.server.config.PluginConfigFactory;
import com.google.gerrit.server.project.ProjectState;
import com.google.inject.Inject;
import com.googlesource.gerrit.plugins.simplesubmitrules.Constants;
import com.googlesource.gerrit.plugins.simplesubmitrules.config.SubmitConfig.CommentsRules;
import com.googlesource.gerrit.plugins.simplesubmitrules.config.SubmitConfig.LabelDefinition;
import java.util.Collection;
import java.util.Map;

public final class ConfigTranslator {
  private final PluginConfigFactory pluginConfigFactory;

  @Inject
  public ConfigTranslator(PluginConfigFactory pluginConfigFactory) {
    this.pluginConfigFactory = pluginConfigFactory;
  }

  static void extractLabelCopyScores(LabelType labelType, LabelDefinition labelDefinition) {
    if (labelType.isCopyMinScore()) {
      labelDefinition.copyScores.add(Constants.COPY_MIN_SCORE);
    }
    if (labelType.isCopyMaxScore()) {
      labelDefinition.copyScores.add(Constants.COPY_MAX_SCORE);
    }
    if (labelType.isCopyAllScoresIfNoChange()) {
      labelDefinition.copyScores.add(Constants.COPY_ALL_SCORES_IF_NO_CHANGE);
    }
    if (labelType.isCopyAllScoresIfNoCodeChange()) {
      labelDefinition.copyScores.add(Constants.COPY_ALL_SCORES_IF_NO_CODE_CHANGE);
    }
    if (labelType.isCopyAllScoresOnMergeFirstParentUpdate()) {
      labelDefinition.copyScores.add(Constants.COPY_ALL_SCORES_ON_MERGE_COMMIT_FIRST_PARENT_UPDATE);
    }
    if (labelType.isCopyAllScoresOnTrivialRebase()) {
      labelDefinition.copyScores.add(Constants.COPY_ALL_SCORES_ON_TRIVIAL_REBASE);
    }
  }

  static void applyCopyScoresTo(Collection<String> copyScores, LabelType labelType) {
    labelType.setCopyMinScore(copyScores.contains(Constants.COPY_MIN_SCORE));

    labelType.setCopyMaxScore(copyScores.contains(Constants.COPY_MAX_SCORE));

    labelType.setCopyAllScoresIfNoChange(
        copyScores.contains(Constants.COPY_ALL_SCORES_IF_NO_CHANGE));

    labelType.setCopyAllScoresIfNoCodeChange(
        copyScores.contains(Constants.COPY_ALL_SCORES_IF_NO_CODE_CHANGE));

    labelType.setCopyAllScoresOnMergeFirstParentUpdate(
        copyScores.contains(Constants.COPY_ALL_SCORES_ON_MERGE_COMMIT_FIRST_PARENT_UPDATE));

    labelType.setCopyAllScoresOnTrivialRebase(
        copyScores.contains(Constants.COPY_ALL_SCORES_ON_TRIVIAL_REBASE));
  }

  SubmitConfig convertFrom(ProjectState projectState) {
    SubmitConfig submitConfig = new SubmitConfig();
    PluginConfig config =
        pluginConfigFactory.getFromProjectConfig(projectState, Constants.PLUGIN_NAME);

    submitConfig.comments.blockIfUnresolvedComments =
        config.getBoolean(Constants.BLOCK_IF_UNRESOLVED_COMMENTS, false);

    projectState
        .getLabelTypes()
        .getLabelTypes()
        .forEach(
            labelType -> {
              extractLabelSettings(labelType, submitConfig);
            });

    return submitConfig;
  }

  void applyTo(SubmitConfig inConfig, ProjectState projectState) throws BadRequestException {
    PluginConfig pluginConfig =
        pluginConfigFactory.getFromProjectConfig(projectState, Constants.PLUGIN_NAME);
    applyCommentRulesTo(inConfig.comments, pluginConfig);
    applyLabelsTo(inConfig.labels, projectState.getLabelTypes());
  }

  private static void applyLabelsTo(Map<String, LabelDefinition> labels, LabelTypes config)
      throws BadRequestException {
    for (Map.Entry<String, LabelDefinition> entry : labels.entrySet()) {
      String label = entry.getKey();
      LabelDefinition definition = entry.getValue();
      LabelType labelType = config.byLabel(label);

      if (labelType == null) {
        throw new BadRequestException(
            "The label " + label + " does not exist." + "You can't change its config.");
      }

      definition.getFunction().ifPresent(labelType::setFunction);

      applyCopyScoresTo(definition.copyScores, labelType);
    }
  }

  private static void extractLabelSettings(LabelType labelType, SubmitConfig config) {
    if (labelType == null) {
      return;
    }

    SubmitConfig.LabelDefinition labelDefinition = new LabelDefinition();
    config.labels.put(labelType.getName(), labelDefinition);

    labelDefinition.function = labelType.getFunction().getFunctionName();
    extractLabelCopyScores(labelType, labelDefinition);
  }

  private static void applyCommentRulesTo(CommentsRules comments, PluginConfig config) {
    config.setBoolean(Constants.BLOCK_IF_UNRESOLVED_COMMENTS, comments.blockIfUnresolvedComments);
  }
}
