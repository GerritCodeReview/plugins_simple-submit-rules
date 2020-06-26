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

import static com.google.gerrit.server.project.ProjectCache.illegalState;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.gerrit.common.Nullable;
import com.google.gerrit.common.data.LabelFunction;
import com.google.gerrit.common.data.LabelType;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.extensions.restapi.BadRequestException;
import com.google.gerrit.server.config.PluginConfig;
import com.google.gerrit.server.config.PluginConfigFactory;
import com.google.gerrit.server.project.ProjectCache;
import com.google.gerrit.server.project.ProjectConfig;
import com.google.gerrit.server.project.ProjectState;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.googlesource.gerrit.plugins.simplesubmitrules.SimpleSubmitRulesConfig;
import com.googlesource.gerrit.plugins.simplesubmitrules.api.CommentsRules;
import com.googlesource.gerrit.plugins.simplesubmitrules.api.LabelDefinition;
import com.googlesource.gerrit.plugins.simplesubmitrules.api.SubmitConfig;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Codec class used to convert {@link SubmitConfig} from/to a Gerrit config */
@Singleton
public final class ConfigTranslator {
  private final ProjectCache projectCache;
  private final PluginConfigFactory pluginConfigFactory;
  private final String pluginName;

  @Inject
  public ConfigTranslator(
      ProjectCache projectCache,
      PluginConfigFactory pluginConfigFactory,
      @PluginName String pluginName) {
    this.projectCache = projectCache;
    this.pluginConfigFactory = pluginConfigFactory;
    this.pluginName = pluginName;
  }

  static void extractLabelCopyScoreRules(LabelType labelType, LabelDefinition labelDefinition) {
    labelDefinition.copyScoreRules = new HashSet<>();
    if (labelType.isCopyMinScore()) {
      labelDefinition.copyScoreRules.add(ProjectConfig.KEY_COPY_MIN_SCORE);
    }
    if (labelType.isCopyMaxScore()) {
      labelDefinition.copyScoreRules.add(ProjectConfig.KEY_COPY_MAX_SCORE);
    }
    if (labelType.isCopyAllScoresIfNoChange()) {
      labelDefinition.copyScoreRules.add(ProjectConfig.KEY_COPY_ALL_SCORES_IF_NO_CHANGE);
    }
    if (labelType.isCopyAllScoresIfNoCodeChange()) {
      labelDefinition.copyScoreRules.add(ProjectConfig.KEY_COPY_ALL_SCORES_IF_NO_CODE_CHANGE);
    }
    if (labelType.isCopyAllScoresOnMergeFirstParentUpdate()) {
      labelDefinition.copyScoreRules.add(
          ProjectConfig.KEY_COPY_ALL_SCORES_ON_MERGE_FIRST_PARENT_UPDATE);
    }
    if (labelType.isCopyAllScoresOnTrivialRebase()) {
      labelDefinition.copyScoreRules.add(ProjectConfig.KEY_COPY_ALL_SCORES_ON_TRIVIAL_REBASE);
    }
  }

  static void applyCopyScoreRulesTo(
      Set<String> copyScoreRules, Set<String> disallowedCopyScoreRules, LabelType.Builder labelType)
      throws BadRequestException {
    Set<String> disallowed =
        Sets.intersection(ImmutableSet.copyOf(copyScoreRules), disallowedCopyScoreRules);
    if (!disallowed.isEmpty()) {
      throw new BadRequestException("copy score rules " + disallowed + " are forbidden");
    }

    labelType.setCopyMinScore(copyScoreRules.contains(ProjectConfig.KEY_COPY_MIN_SCORE));
    labelType.setCopyMaxScore(copyScoreRules.contains(ProjectConfig.KEY_COPY_MAX_SCORE));
    labelType.setCopyAllScoresIfNoChange(
        copyScoreRules.contains(ProjectConfig.KEY_COPY_ALL_SCORES_IF_NO_CHANGE));
    labelType.setCopyAllScoresIfNoCodeChange(
        copyScoreRules.contains(ProjectConfig.KEY_COPY_ALL_SCORES_IF_NO_CODE_CHANGE));
    labelType.setCopyAllScoresOnMergeFirstParentUpdate(
        copyScoreRules.contains(ProjectConfig.KEY_COPY_ALL_SCORES_ON_MERGE_FIRST_PARENT_UPDATE));
    labelType.setCopyAllScoresOnTrivialRebase(
        copyScoreRules.contains(ProjectConfig.KEY_COPY_ALL_SCORES_ON_TRIVIAL_REBASE));
  }

  SubmitConfig convertFrom(ProjectState projectState) {
    SubmitConfig submitConfig = new SubmitConfig();
    PluginConfig config =
        pluginConfigFactory.getFromProjectConfigWithInheritance(projectState, pluginName);

    submitConfig.comments =
        new CommentsRules(
            config.getBoolean(SimpleSubmitRulesConfig.KEY_BLOCK_IF_UNRESOLVED_COMMENTS, false));

    projectState
        .getLabelTypes()
        .getLabelTypes()
        .forEach(
            labelType -> {
              extractLabelSettings(labelType, submitConfig);
            });

    return submitConfig;
  }

  void applyTo(SubmitConfig inConfig, ProjectConfig projectConfig)
      throws BadRequestException, IOException {
    PluginConfig hostPluginConfig = pluginConfigFactory.getFromGerritConfig(pluginName);
    PluginConfig projectPluginConfig = projectConfig.getPluginConfig(pluginName);

    applyCommentRulesTo(inConfig.comments, projectPluginConfig);
    applyLabelsTo(inConfig.labels, projectConfig, hostPluginConfig);
  }

  private void applyLabelsTo(
      Map<String, LabelDefinition> labels,
      ProjectConfig projectConfig,
      PluginConfig hostPluginConfig)
      throws BadRequestException, IOException {
    if (labels.isEmpty()) {
      return;
    }

    for (Map.Entry<String, LabelDefinition> entry : labels.entrySet()) {
      if (!projectConfig.getLabelSections().containsKey(entry.getKey())) {
        // The current project does not have this label. Try to copy it down from the inherited
        // labels to be able to modify it locally.
        Map<String, LabelType> copiedLabelTypes = projectConfig.getLabelSections();
        ProjectState projectState =
            projectCache
                .get(projectConfig.getName())
                .orElseThrow(illegalState(projectConfig.getName()));
        projectState.getLabelTypes().getLabelTypes().stream()
            .filter(l -> l.getName().equals(entry.getKey()))
            .filter(l -> l.isCanOverride())
            .forEach(l -> copiedLabelTypes.put(l.getName(), copyLabelType(l)));
      }

      String label = entry.getKey();
      LabelDefinition definition = entry.getValue();
      if (projectConfig.getLabelSections().get(label) == null) {
        throw new BadRequestException(
            "The label " + label + " does not exist. You can't change its config.");
      }
      if (definition.getFunction().isPresent()) {
        Set<String> disallowedLabelFunctions =
            ImmutableSet.copyOf(
                hostPluginConfig.getStringList("disallowedLabelFunctions-" + label));
        LabelFunction function = definition.getFunction().get();
        if (disallowedLabelFunctions.contains(function.getFunctionName())) {
          throw new BadRequestException(function.getFunctionName() + " disallowed");
        }
      }
      projectConfig.updateLabelType(
          label,
          labelType -> {
            if (definition.ignoreSelfApproval != null) {
              labelType.setIgnoreSelfApproval(definition.ignoreSelfApproval);
            }
            if (definition.getFunction().isPresent()) {
              labelType.setFunction(definition.getFunction().get());
            }
          });

      if (definition.copyScoreRules != null) {
        Set<String> disallowedCopyScoreRules =
            ImmutableSet.copyOf(
                hostPluginConfig.getStringList("disallowedCopyScoreRules-" + label));
        applyCopyScoreRulesTo(
            definition.copyScoreRules,
            disallowedCopyScoreRules,
            projectConfig.getLabelSections().get(label).toBuilder());
      }
    }
  }

  private static void extractLabelSettings(LabelType labelType, SubmitConfig config) {
    if (labelType == null) {
      return;
    }

    LabelDefinition labelDefinition = new LabelDefinition();
    config.labels.put(labelType.getName(), labelDefinition);

    labelDefinition.function = labelType.getFunction().getFunctionName();
    extractLabelCopyScoreRules(labelType, labelDefinition);
    labelDefinition.ignoreSelfApproval = labelType.isIgnoreSelfApproval();
  }

  private static void applyCommentRulesTo(@Nullable CommentsRules comments, PluginConfig config) {
    if (comments == null) {
      return;
    }
    config.setBoolean(
        SimpleSubmitRulesConfig.KEY_BLOCK_IF_UNRESOLVED_COMMENTS,
        comments.blockIfUnresolvedComments);
  }

  private static LabelType copyLabelType(LabelType label) {
    // TODO(hiesel) Move this to core
    LabelType.Builder copy =
        LabelType.create(label.getName(), ImmutableList.copyOf(label.getValues())).toBuilder();
    if (label.getRefPatterns() != null) {
      copy.setRefPatterns(ImmutableList.copyOf(label.getRefPatterns()));
    }
    copy.setAllowPostSubmit(label.isAllowPostSubmit());
    copy.setCanOverride(label.isCanOverride());
    copy.setCopyAllScoresIfNoChange(label.isCopyAllScoresIfNoChange());
    copy.setCopyAllScoresIfNoCodeChange(label.isCopyAllScoresIfNoCodeChange());
    copy.setCopyAllScoresOnMergeFirstParentUpdate(label.isCopyAllScoresOnMergeFirstParentUpdate());
    copy.setCopyAllScoresOnTrivialRebase(label.isCopyAllScoresOnTrivialRebase());
    copy.setIgnoreSelfApproval(label.isIgnoreSelfApproval());
    copy.setCopyMaxScore(label.isCopyMaxScore());
    copy.setCopyMinScore(label.isCopyMinScore());
    copy.setFunction(label.getFunction());
    return copy.build();
  }
}
