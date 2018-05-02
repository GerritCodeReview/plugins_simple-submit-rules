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
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ConfigTranslatorTest {
  /** Test the translations for each config key */
  @Test
  public void checkLabelTranslation() {
    checkTranslation("copyMinScore", LabelType::isCopyMinScore, LabelType::setCopyMinScore);
    checkTranslation("copyMaxScore", LabelType::isCopyMaxScore, LabelType::setCopyMaxScore);

    checkTranslation(
        "copyAllScoresIfNoChange",
        LabelType::isCopyAllScoresIfNoChange,
        LabelType::setCopyAllScoresIfNoChange);

    checkTranslation(
        "copyAllScoresIfNoCodeChange",
        LabelType::isCopyAllScoresIfNoCodeChange,
        LabelType::setCopyAllScoresIfNoCodeChange);

    checkTranslation(
        "copyAllScoresOnMergeFirstParentUpdate",
        LabelType::isCopyAllScoresOnMergeFirstParentUpdate,
        LabelType::setCopyAllScoresOnMergeFirstParentUpdate);

    checkTranslation(
        "copyAllScoresOnTrivialRebase",
        LabelType::isCopyAllScoresOnTrivialRebase,
        LabelType::setCopyAllScoresOnTrivialRebase);
  }

  /** Helper method to check that conversion from/to Gerrit works for both true and false values */
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
