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

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.gerrit.common.data.LabelType;
import com.googlesource.gerrit.plugins.simplesubmitrules.config.SubmitConfig.LabelDefinition;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import org.junit.Test;

public class ConfigTranslatorTest {
  @Test
  public void checkLabelTranslation() {
    checkTranslation(
        ConfigTranslator.COPY_MIN_SCORE, LabelType::isCopyMinScore, LabelType::setCopyMinScore);
    checkTranslation(
        ConfigTranslator.COPY_MAX_SCORE, LabelType::isCopyMaxScore, LabelType::setCopyMaxScore);

    checkTranslation(
        ConfigTranslator.COPY_ALL_SCORES_IF_NO_CHANGE,
        LabelType::isCopyAllScoresIfNoChange,
        LabelType::setCopyAllScoresIfNoChange);

    checkTranslation(
        ConfigTranslator.COPY_ALL_SCORES_IF_NO_CODE_CHANGE,
        LabelType::isCopyAllScoresIfNoCodeChange,
        LabelType::setCopyAllScoresIfNoCodeChange);

    checkTranslation(
        ConfigTranslator.COPY_ALL_SCORES_ON_MERGE_COMMIT_FIRST_PARENT_UPDATE,
        LabelType::isCopyAllScoresOnMergeFirstParentUpdate,
        LabelType::setCopyAllScoresOnMergeFirstParentUpdate);

    checkTranslation(
        ConfigTranslator.COPY_ALL_SCORES_ON_TRIVIAL_REBASE,
        LabelType::isCopyAllScoresOnTrivialRebase,
        LabelType::setCopyAllScoresOnTrivialRebase);
  }

  private static void checkTranslation(
      String copyScoreName,
      Predicate<LabelType> functionToCheck,
      BiConsumer<LabelType, Boolean> functionToSet) {
    checkLabelToGerritPresent(copyScoreName, functionToCheck);
    checkLabelToGerritAbsent(copyScoreName, functionToCheck);

    checkLabelFromGerritPresent(copyScoreName, functionToSet);
    checkLabelFromGerritAbsent(copyScoreName, functionToSet);
  }

  private static void checkLabelFromGerritPresent(
      String copyScoreName, BiConsumer<LabelType, Boolean> functionToSet) {

    LabelType label = LabelType.withDefaultValues("Verified");
    LabelDefinition labelDefinition = new SubmitConfig.LabelDefinition();

    functionToSet.accept(label, false);
    ConfigTranslator.extractLabelCopyScores(label, labelDefinition);

    assertThat(labelDefinition.copyScores)
        .named("[case %s:false]", copyScoreName)
        .doesNotContain(copyScoreName);
  }

  private static void checkLabelFromGerritAbsent(
      String copyScoreName, BiConsumer<LabelType, Boolean> functionToSet) {

    LabelType label = LabelType.withDefaultValues("Verified");
    LabelDefinition labelDefinition = new SubmitConfig.LabelDefinition();

    functionToSet.accept(label, true);
    ConfigTranslator.extractLabelCopyScores(label, labelDefinition);

    assertThat(labelDefinition.copyScores)
        .named("[case %s:true]", copyScoreName)
        .contains(copyScoreName);
  }

  private static void checkLabelToGerritPresent(
      String copyScoreName, Predicate<LabelType> functionToCheck) {
    LabelType label = LabelType.withDefaultValues("Verified");

    ConfigTranslator.applyCopyScoresTo(ImmutableList.of(copyScoreName), label);
    assertThat(functionToCheck.test(label)).named("[case %s:true]", copyScoreName).isTrue();
  }

  private static void checkLabelToGerritAbsent(
      String copyScoreName, Predicate<LabelType> functionToCheck) {
    LabelType label = LabelType.withDefaultValues("Verified");

    ConfigTranslator.applyCopyScoresTo(ImmutableList.of(), label);
    assertThat(functionToCheck.test(label)).named("[case %s:false]", copyScoreName).isFalse();
  }
}
