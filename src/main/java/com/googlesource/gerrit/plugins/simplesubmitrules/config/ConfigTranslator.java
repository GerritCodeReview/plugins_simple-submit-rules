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
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.extensions.restapi.BadRequestException;
import com.google.gerrit.server.config.PluginConfig;
import com.google.gerrit.server.config.PluginConfigFactory;
import com.google.gerrit.server.project.ProjectConfig;
import com.google.gerrit.server.project.ProjectState;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.googlesource.gerrit.plugins.simplesubmitrules.SimpleSubmitRulesConfig;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

/** Codec class used to convert {@link SubmitConfig} from/to a Gerrit config */
@Singleton
public final class ConfigTranslator {
  private final PluginConfigFactory pluginConfigFactory;
  private final String pluginName;

  @Inject
  public ConfigTranslator(PluginConfigFactory pluginConfigFactory, @PluginName String pluginName) {
    this.pluginConfigFactory = pluginConfigFactory;
    this.pluginName = pluginName;
  }

  static void extractLabelCopyScores(
      LabelType labelType, SubmitConfig.LabelDefinition labelDefinition) {
    labelDefinition.copyScores = new HashSet<>();
    if (labelType.isCopyMinScore()) {
      labelDefinition.copyScores.add(ProjectConfig.KEY_COPY_MIN_SCORE);
    }
    if (labelType.isCopyMaxScore()) {
      labelDefinition.copyScores.add(ProjectConfig.KEY_COPY_MAX_SCORE);
    }
    if (labelType.isCopyAllScoresIfNoChange()) {
      labelDefinition.copyScores.add(ProjectConfig.KEY_COPY_ALL_SCORES_IF_NO_CHANGE);
    }
    if (labelType.isCopyAllScoresIfNoCodeChange()) {
      labelDefinition.copyScores.add(ProjectConfig.KEY_COPY_ALL_SCORES_IF_NO_CODE_CHANGE);
    }
    if (labelType.isCopyAllScoresOnMergeFirstParentUpdate()) {
      labelDefinition.copyScores.add(
          ProjectConfig.KEY_COPY_ALL_SCORES_ON_MERGE_FIRST_PARENT_UPDATE);
    }
    if (labelType.isCopyAllScoresOnTrivialRebase()) {
      labelDefinition.copyScores.add(ProjectConfig.KEY_COPY_ALL_SCORES_ON_TRIVIAL_REBASE);
    }
  }

  static void applyCopyScoresTo(Collection<String> copyScores, LabelType labelType) {
    labelType.setCopyMinScore(copyScores.contains(ProjectConfig.KEY_COPY_MIN_SCORE));

    labelType.setCopyMaxScore(copyScores.contains(ProjectConfig.KEY_COPY_MAX_SCORE));

    labelType.setCopyAllScoresIfNoChange(
        copyScores.contains(ProjectConfig.KEY_COPY_ALL_SCORES_IF_NO_CHANGE));

    labelType.setCopyAllScoresIfNoCodeChange(
        copyScores.contains(ProjectConfig.KEY_COPY_ALL_SCORES_IF_NO_CODE_CHANGE));

    labelType.setCopyAllScoresOnMergeFirstParentUpdate(
        copyScores.contains(ProjectConfig.KEY_COPY_ALL_SCORES_ON_MERGE_FIRST_PARENT_UPDATE));

    labelType.setCopyAllScoresOnTrivialRebase(
        copyScores.contains(ProjectConfig.KEY_COPY_ALL_SCORES_ON_TRIVIAL_REBASE));
  }

  SubmitConfig convertFrom(ProjectState projectState) {
    SubmitConfig submitConfig = new SubmitConfig();
    PluginConfig config =
        pluginConfigFactory.getFromProjectConfigWithInheritance(projectState, pluginName);

    submitConfig.comments.blockIfUnresolvedComments =
        config.getBoolean(SimpleSubmitRulesConfig.KEY_BLOCK_IF_UNRESOLVED_COMMENTS, false);

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
    PluginConfig pluginConfig = pluginConfigFactory.getFromProjectConfig(projectState, pluginName);
    applyCommentRulesTo(inConfig.comments, pluginConfig);
    applyLabelsTo(inConfig.labels, projectState.getLabelTypes());
  }

  private static void applyLabelsTo(
      Map<String, SubmitConfig.LabelDefinition> labels, LabelTypes labelTypes)
      throws BadRequestException {
    for (Map.Entry<String, SubmitConfig.LabelDefinition> entry : labels.entrySet()) {
      String label = entry.getKey();
      SubmitConfig.LabelDefinition definition = entry.getValue();
      LabelType labelType = labelTypes.byLabel(label);

      if (labelType == null) {
        throw new BadRequestException(
            "The label " + label + " does not exist. You can't change its config.");
      }

      definition.getFunction().ifPresent(labelType::setFunction);
      labelType.setIgnoreUploaderSelfApproval(definition.ignoreSelfApproval);
    }
  }

  private static void extractLabelSettings(LabelType labelType, SubmitConfig config) {
    if (labelType == null) {
      return;
    }

    SubmitConfig.LabelDefinition labelDefinition = new SubmitConfig.LabelDefinition();
    config.labels.put(labelType.getName(), labelDefinition);

    labelDefinition.function = labelType.getFunction().getFunctionName();
    extractLabelCopyScores(labelType, labelDefinition);
    labelDefinition.ignoreSelfApproval = labelType.ignoreUploaderSelfApproval();
  }

  private static void applyCommentRulesTo(
      SubmitConfig.CommentsRules comments, PluginConfig config) {
    config.setBoolean(
        SimpleSubmitRulesConfig.KEY_BLOCK_IF_UNRESOLVED_COMMENTS,
        comments.blockIfUnresolvedComments);
  }
}
