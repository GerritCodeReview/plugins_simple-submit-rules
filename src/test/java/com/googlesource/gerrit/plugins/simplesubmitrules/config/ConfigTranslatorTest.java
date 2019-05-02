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

import static com.google.common.truth.Truth.assertWithMessage;

import com.google.common.collect.ImmutableSet;
import com.google.gerrit.common.data.LabelType;
import com.google.gerrit.extensions.restapi.BadRequestException;
import com.googlesource.gerrit.plugins.simplesubmitrules.api.LabelDefinition;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class ConfigTranslatorTest {
  @Rule public ExpectedException exception = ExpectedException.none();

  @Test
  public void checkLabelTranslation() throws Exception {
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

  @Test
  public void checkDisallowedCopyScoreThrowsBadRequest() throws Exception {
    exception.expect(BadRequestException.class);
    exception.expectMessage("copy score rules [copyAllScoresIfNoChange] are forbidden");
    ConfigTranslator.applyCopyScoreRulesTo(
        ImmutableSet.of("copyAllScoresIfNoChange", "copyAllScoresOnMergeFirstParentUpdate"),
        ImmutableSet.of("copyAllScoresIfNoChange"),
        LabelType.withDefaultValues("Verified"));
  }

  /** Helper method to check that conversion from/to Gerrit works for both true and false values */
  private static void checkTranslation(
      String copyScoreName,
      Predicate<LabelType> functionToCheck,
      BiConsumer<LabelType, Boolean> functionToSet)
      throws Exception {
    checkLabelToGerritPresent(copyScoreName, functionToCheck);
    checkLabelToGerritAbsent(copyScoreName, functionToCheck);

    checkLabelFromGerritPresent(copyScoreName, functionToSet);
    checkLabelFromGerritAbsent(copyScoreName, functionToSet);
  }

  private static void checkLabelFromGerritPresent(
      String copyScoreName, BiConsumer<LabelType, Boolean> functionToSet) {

    LabelType label = LabelType.withDefaultValues("Verified");
    LabelDefinition labelDefinition = new LabelDefinition();

    functionToSet.accept(label, false);
    ConfigTranslator.extractLabelCopyScoreRules(label, labelDefinition);

    assertWithMessage("[case %s:false]", copyScoreName)
        .that(labelDefinition.copyScoreRules)
        .doesNotContain(copyScoreName);
  }

  private static void checkLabelFromGerritAbsent(
      String copyScoreName, BiConsumer<LabelType, Boolean> functionToSet) {

    LabelType label = LabelType.withDefaultValues("Verified");
    LabelDefinition labelDefinition = new LabelDefinition();

    functionToSet.accept(label, true);
    ConfigTranslator.extractLabelCopyScoreRules(label, labelDefinition);

    assertWithMessage("[case %s:true]", copyScoreName)
        .that(labelDefinition.copyScoreRules)
        .contains(copyScoreName);
  }

  private static void checkLabelToGerritPresent(
      String copyScoreName, Predicate<LabelType> functionToCheck) throws Exception {
    LabelType label = LabelType.withDefaultValues("Verified");

    ConfigTranslator.applyCopyScoreRulesTo(
        ImmutableSet.of(copyScoreName), ImmutableSet.of(), label);
    assertWithMessage("[case %s:true]", copyScoreName).that(functionToCheck.test(label)).isTrue();
  }

  private static void checkLabelToGerritAbsent(
      String copyScoreName, Predicate<LabelType> functionToCheck) throws Exception {
    LabelType label = LabelType.withDefaultValues("Verified");

    ConfigTranslator.applyCopyScoreRulesTo(ImmutableSet.of(), ImmutableSet.of(), label);
    assertWithMessage("[case %s:false]", copyScoreName).that(functionToCheck.test(label)).isFalse();
  }
}
