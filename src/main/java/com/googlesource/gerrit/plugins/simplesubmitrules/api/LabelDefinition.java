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

package com.googlesource.gerrit.plugins.simplesubmitrules.api;

import com.google.common.base.MoreObjects;
import com.google.gerrit.entities.LabelFunction;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class LabelDefinition {
  public String function;
  public Boolean ignoreSelfApproval;
  public Set<String> copyScoreRules;

  public LabelDefinition() {}

  public LabelDefinition(String function, Boolean ignoreSelfApproval, Set<String> copyScoreRules) {
    this.function = function;
    this.ignoreSelfApproval = ignoreSelfApproval;
    this.copyScoreRules = copyScoreRules;
  }

  public Optional<LabelFunction> getFunction() {
    return LabelFunction.parse(function);
  }

  @Override
  public int hashCode() {
    return Objects.hash(function, ignoreSelfApproval, copyScoreRules);
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof LabelDefinition)) {
      return false;
    }
    LabelDefinition other = (LabelDefinition) o;
    return Objects.equals(function, other.function)
        && Objects.equals(ignoreSelfApproval, other.ignoreSelfApproval)
        && Objects.equals(copyScoreRules, other.copyScoreRules);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("function", function)
        .add("ignoreSelfApproval", ignoreSelfApproval)
        .add("copyScoreRules", copyScoreRules)
        .toString();
  }
}
