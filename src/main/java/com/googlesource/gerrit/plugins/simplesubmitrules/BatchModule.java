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

import com.google.gerrit.extensions.registration.DynamicSet;
import com.google.gerrit.server.rules.SubmitRule;
import com.google.inject.AbstractModule;
import com.googlesource.gerrit.plugins.simplesubmitrules.config.ConfigTranslator;
import com.googlesource.gerrit.plugins.simplesubmitrules.rules.NoUnresolvedCommentsRule;

/** Rules for the batch programs (offline reindexer) */
public class BatchModule extends AbstractModule {
  @Override
  protected void configure() {
    bind(ConfigTranslator.class);
    DynamicSet.bind(binder(), SubmitRule.class).to(NoUnresolvedCommentsRule.class);
  }
}
