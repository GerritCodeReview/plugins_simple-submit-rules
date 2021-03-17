/**
 * @license
 * Copyright (C) 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

export const htmlTemplate = Polymer.html`
    <style include="shared-styles"></style>
    <style include="gr-form-styles"></style>
    <style>
      :host {
        border: 1px solid var(--border-color);
        display: block;
        margin-bottom: 1em;
        padding: 1em 1em;
      }

      fieldset {
        border: 1px solid var(--border-color);
      }
    </style>

    <main class="gr-form-styles">
      <h3 id="options">Label [[labelName]]</h3>

      <fieldset id="simple-submit-rules">

        <section>
          <span class="title">Should a vote with the maximum value be required?</span>
          <span class="value">
            <input id="maxVoteRequired"
              type="checkbox"
              checked="{{_maxVoteRequired::change}}"
              disabled$="[[readOnly]]">
          </span>
        </section>

        <section>
          <span class="title">Should votes with the lowest value block submission?</span>
          <span class="value">
            <input id="negativeBlocks"
                type="checkbox"
                checked="{{_negativeBlocks::change}}"
                disabled$="[[readOnly]]">
          </span>
        </section>

        <section>
          <span class="title">(Expert users) Function name</span>
          <span class="value">
            <gr-select id="functionName"
                bind-value="{{_labelConfig.function}}">
              <select disabled$="[[readOnly]]">
                <option value="MaxNoBlock">MaxNoBlock</option>
                <option value="MaxWithBlock">MaxWithBlock</option>
                <option value="AnyWithBlock">AnyWithBlock</option>
                <option value="NoBlock">NoBlock</option>
                <option value="NoOp">NoOp</option>
              </select>
            </gr-select>
          </span>
        </section>

        <section>
          <span class="title">Disallow approval by the change owner</span>
          <span class="value">
            <input id="allowUnresolvedComments"
                type="checkbox"
                checked="{{_labelConfig.ignore_self_approval::change}}"
                disabled$="[[readOnly]]">
          </span>
        </section>
      </fieldset>

      <fieldset>
        <section>
          <span class="title">
            When a new patchset is uploaded, Gerrit should copy votes ...
          </span>
        </section>

        <!-- copyMinScore -->
        <section>
          <span class="title">
            <gr-tooltip-content class="draftTooltip"
                has-tooltip
                title="Should votes of the minimal value be kept?"
                max-width="20em"
                show-icon>
              with minimal value
            </gr-tooltip-content>
          </span>
          <span class="value">
          <input id="copyMinScore"
              type="checkbox"
              checked="{{_copyScoreRules.copyMinScore::change}}"
              disabled$="[[readOnly]]">
          </span>
        </section>

        <!-- copyMaxScore -->
        <section>
          <span class="title">
            <gr-tooltip-content class="draftTooltip"
                has-tooltip
                title="Should votes of the maximal value be kept?"
                max-width="20em"
                show-icon>
              with maximal value
            </gr-tooltip-content>
          </span>
          <span class="value">
            <input id="copyMaxScore"
                type="checkbox"
                checked="{{_copyScoreRules.copyMaxScore::change}}"
                disabled$="[[readOnly]]">
          </span>
        </section>

        <!-- copyAllScoresOnTrivialRebase -->
        <section>
          <span class="title">
            <gr-tooltip-content class="draftTooltip"
                has-tooltip
                title="Should votes be kept when a trivial rebase
                         is done (same commit message and content, different parent)?"
                max-width="20em"
                show-icon>
              on trivial rebase
            </gr-tooltip-content>
          </span>
          <span class="value">
            <input id="copyAllScoresOnTrivialRebase"
                type="checkbox"
                checked="{{_copyScoreRules.copyAllScoresOnTrivialRebase::change}}"
                disabled$="[[readOnly]]">
          </span>
        </section>

        <!-- copyAllScoresIfNoCodeChange -->
        <section>
          <span class="title">
            <gr-tooltip-content class="draftTooltip"
                has-tooltip
                title="Should votes be kept when the commit message is modified?
                         Changing the parent or changing files invalidates this."
                max-width="20em"
                show-icon>
              when only the commit message is modified
            </gr-tooltip-content>
          </span>
          <span class="value">
            <input id="copyAllScoresIfNoCodeChange"
                type="checkbox"
                checked="{{_copyScoreRules.copyAllScoresIfNoCodeChange::change}}"
                disabled$="[[readOnly]]">
          </span>
        </section>

        <!-- copyAllScoresIfNoChange -->
        <section>
          <span class="title">
            <gr-tooltip-content class="draftTooltip"
                has-tooltip
                title="Should votes be kept when the commit metadata (author, commit
                        date) are modified? Changing anything else from the commit
                        (message, content, parent) invalidates this."
                max-width="20em"
                show-icon>
              when only commit metatada are modified
            </gr-tooltip-content>
          </span>
          <span class="value">
            <input id="copyAllScoresIfNoChange"
                type="checkbox"
                checked="{{_copyScoreRules.copyAllScoresIfNoChange::change}}"
                disabled$="[[readOnly]]">
          </span>
        </section>

        <!-- copyAllScoresOnMergeFirstParentUpdate -->
        <section>
          <span class="title">
            <gr-tooltip-content class="draftTooltip"
                has-tooltip
                title="Only applies to Merge commits. Should votes be kept when the
                        destination commit of a merge commit is changed?"
                max-width="20em"
                show-icon>
              on rebased merge commits
            </gr-tooltip-content>
          </span>
          <span class="value">
            <input id="copyAllScoresOnMergeFirstParentUpdate"
                type="checkbox"
                checked="{{_copyScoreRules.copyAllScoresOnMergeFirstParentUpdate::change}}"
                disabled$="[[readOnly]]">
          </span>
        </section>

      </fieldset>
    </main>
    `;
