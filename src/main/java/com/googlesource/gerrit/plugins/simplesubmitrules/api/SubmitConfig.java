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
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class SubmitConfig {
  public Map<String, LabelDefinition> labels;
  public CommentsRules comments;

  public SubmitConfig() {
    labels = new HashMap<>();
  }

  public SubmitConfig(Map<String, LabelDefinition> labels, CommentsRules comments) {
    this.labels = labels;
    this.comments = comments;
  }

  @Override
  public int hashCode() {
    return Objects.hash(labels, comments);
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof SubmitConfig)) {
      return false;
    }
    SubmitConfig other = (SubmitConfig) o;
    return Objects.equals(labels, other.labels) && Objects.equals(comments, other.comments);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("labels", labels)
        .add("comments", comments)
        .toString();
  }
}
