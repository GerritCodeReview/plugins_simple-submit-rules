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
  public static final String PLUGIN_NAME = "simple-submit-rules";
  public static final String API_ENDPOINT = PLUGIN_NAME;

  public static final String REQUIRE_NON_AUTHOR_APPROVAL = "approval-non-author-required";
  public static final String BLOCK_IF_UNRESOLVED_COMMENTS = "block-if-unresolved-comments";

  private Constants() {}
}
