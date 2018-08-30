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
import java.util.Objects;

public class CommentsRules {
  public boolean blockIfUnresolvedComments;

  public CommentsRules(boolean blockIfUnresolvedComments) {
    this.blockIfUnresolvedComments = blockIfUnresolvedComments;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(blockIfUnresolvedComments);
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof CommentsRules)) {
      return false;
    }
    CommentsRules other = (CommentsRules) o;
    return blockIfUnresolvedComments == other.blockIfUnresolvedComments;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("blockIfUnresolvedComments", blockIfUnresolvedComments)
        .toString();
  }
}
