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

import com.google.common.collect.ImmutableList;
import com.google.gerrit.common.data.LabelFunction;
import com.google.gerrit.common.data.LabelType;
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
import java.util.List;
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

    PluginConfig pluginConfig =
        pluginConfigFactory.getFromProjectConfigWithInheritance(projectState, pluginName);
    projectState
        .getLabelTypes()
        .getLabelTypes()
        .forEach(
            labelType -> {
              extractLabelSettings(labelType, submitConfig, pluginConfig);
            });

    return submitConfig;
  }

  void applyTo(SubmitConfig inConfig, ProjectState projectState) throws BadRequestException {
    PluginConfig hostPluginConfig = pluginConfigFactory.getFromGerritConfig(pluginName);
    PluginConfig perProjectPluginConfig =
        pluginConfigFactory.getFromProjectConfig(projectState, pluginName);
    applyCommentRulesTo(inConfig.comments, perProjectPluginConfig);
    applyLabelsTo(inConfig.labels, projectState, hostPluginConfig, perProjectPluginConfig);
  }

  private static void applyLabelsTo(
      Map<String, SubmitConfig.LabelDefinition> labels,
      ProjectState projectState,
      PluginConfig hostPluginConfig,
      PluginConfig perProjectPluginConfig)
      throws BadRequestException {
    if (labels.isEmpty()) {
      return;
    }

    for (Map.Entry<String, SubmitConfig.LabelDefinition> entry : labels.entrySet()) {
      if (!projectState.getConfig().getLabelSections().containsKey(entry.getKey())) {
        // The current project does not have this label. Try to copy it down from the inherited
        // labels to be able to modify it locally.
        Map<String, LabelType> copiedLabelTypes = projectState.getConfig().getLabelSections();
        projectState
            .getLabelTypes()
            .getLabelTypes()
            .stream()
            .filter(l -> l.getName().equals(entry.getKey()))
            .forEach(l -> copiedLabelTypes.put(l.getName(), l));
      }

      String label = entry.getKey();
      SubmitConfig.LabelDefinition definition = entry.getValue();
      LabelType labelType = projectState.getLabelTypes().byLabel(label);

      if (labelType == null) {
        throw new BadRequestException(
            "The label " + label + " does not exist. You can't change its config.");
      }

      if (definition.getFunction().isPresent()) {
        List<String> disallowedLabelFunctions =
            ImmutableList.copyOf(
                hostPluginConfig.getStringList("disallowedLabelFunctions-" + label));
        LabelFunction function = definition.getFunction().get();
        if (disallowedLabelFunctions.contains(function.getFunctionName())) {
          throw new BadRequestException(function.getFunctionName() + " disallowed");
        }
        labelType.setFunction(function);
      }
      applyCopyScoresTo(definition.copyScores, labelType);
      if (definition.ignoreSelfApproval != null) {
        // Core Gerrit doesn't know about the self approval bit. Write it to the plugin config to
        // keep it save from core Gerrit's loading and saving the project config, which can in some
        // cases overwrite attributes it doesn't know (e.g. when the order of labels changes).
        perProjectPluginConfig.setBoolean(
            SimpleSubmitRulesConfig.requireNonAuthorApprovalKey(label),
            definition.ignoreSelfApproval);
      }
    }
  }

  private static void extractLabelSettings(
      LabelType labelType, SubmitConfig config, PluginConfig pluginConfig) {
    if (labelType == null) {
      return;
    }

    SubmitConfig.LabelDefinition labelDefinition = new SubmitConfig.LabelDefinition();
    config.labels.put(labelType.getName(), labelDefinition);

    labelDefinition.function = labelType.getFunction().getFunctionName();
    extractLabelCopyScores(labelType, labelDefinition);
    labelDefinition.ignoreSelfApproval =
        pluginConfig.getBoolean(
            SimpleSubmitRulesConfig.requireNonAuthorApprovalKey(labelType.getName()), false);
  }

  private static void applyCommentRulesTo(
      SubmitConfig.CommentsRules comments, PluginConfig config) {
    config.setBoolean(
        SimpleSubmitRulesConfig.KEY_BLOCK_IF_UNRESOLVED_COMMENTS,
        comments.blockIfUnresolvedComments);
  }
}
