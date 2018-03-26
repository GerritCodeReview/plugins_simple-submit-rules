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

import com.google.common.base.MoreObjects;
import com.google.gerrit.common.data.LabelFunction;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class SubmitConfig {
  public Map<String, LabelDefinition> labels = new HashMap<>();
  public CommentsRules comments = new CommentsRules();

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("labels", labels)
        .add("comments", comments)
        .toString();
  }

  static class LabelDefinition {
    String function = null;
    boolean ignoreSelfApproval = false;
    Set<String> copyScores = new HashSet<>();

    Optional<LabelFunction> getFunction() {
      return LabelFunction.parse(function);
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
          .add("function", function)
          .add("ignoreSelfApproval", ignoreSelfApproval)
          .add("copyScores", copyScores)
          .toString();
    }
  }

  static class CommentsRules {
    boolean blockIfUnresolvedComments = false;

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
          .add("blockIfUnresolvedComments", blockIfUnresolvedComments)
          .toString();
    }
  }
}
