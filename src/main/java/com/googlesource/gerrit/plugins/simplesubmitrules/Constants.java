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

public final class Constants {
  public static final String PLUGIN_NAME = "simple-submit";

  public static final String REQUIRE_NON_AUTHOR_APPROVAL = "approval-non-author-required";
  public static final String BLOCK_IF_UNRESOLVED_COMMENTS = "block-if-unresolved-comments";

  public static final String COPY_MIN_SCORE = "copy-min-score";
  public static final String COPY_MAX_SCORE = "copy-max-score";
  public static final String COPY_ALL_SCORES_IF_NO_CHANGE = "copy-all-scores-if-no-change";
  public static final String COPY_ALL_SCORES_IF_NO_CODE_CHANGE =
      "copy-all-scores-if-no-code-change";
  public static final String COPY_ALL_SCORES_ON_MERGE_COMMIT_FIRST_PARENT_UPDATE =
      "copy-all-scores-on-merge-commit-first-parent-update";
  public static final String COPY_ALL_SCORES_ON_TRIVIAL_REBASE =
      "copy-all-scores-on-trivial-rebase";

  private Constants() {}
}
