/**
 * @license
 * Copyright (C) 2018 The Android Open Source Project
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

import {htmlTemplate} from './gr-simple-submit-rules-label-config_html.js';

const COPY_SCORES = [
  'copyMinScore',
  'copyMaxScore',
  'copyAllScoresOnTrivialRebase',
  'copyAllScoresIfNoCodeChange',
  'copyAllScoresIfNoChange',
  'copyAllScoresOnMergeFirstParentUpdate',
];

class SimpleSubmitRulesLableConfig extends Polymer.Element {
  static get is() {
    return 'gr-simple-submit-rules-label-config';
  }

  static get properties() {
    return {
      labelName: String,
      /** @type {?} */
      repoConfig: {
        type: Object,
        notify: true,
      },
      readOnly: Boolean,
      // The two "_updating" booleans are there to prevent an infinite loop:
      // when the user changes a value, we update another value and this
      // update in turn triggers the function again.
      _updatingFunction: {
        type: Boolean,
        value: false,
      },
      _updatingCopyScoreRules: {
        type: Boolean,
        value: false,
      },
      _labelConfig: {
        type: Object,
        computed: '_computeLabelConfig(repoConfig.labels, labelName)',
      },
      _copyScoreRules: {
        type: Object,
        value: {},
      },
      _negativeBlocks: Boolean,
      _maxVoteRequired: Boolean,
    };
  }

  static get template() {
    return htmlTemplate;
  }

  static get observers() {
    return [
      '_observeFunctionChange(_labelConfig.function)',
      '_observeFunctionDescriptorChange(_negativeBlocks, _maxVoteRequired)',
      '_observeCopyScoreRulesChange(_labelConfig.copy_score_rules)',
      '_observeCopyScoreRulesChangeInUi(_copyScoreRules.*)',
    ];
  }

  _observeFunctionDescriptorChange(negativeBlocks, maxVoteRequired) {
    if (this._labelConfig === undefined) {
      return;
    }
    if (this._updatingFunction) {
      return;
    }
    this._updatingFunction = true;
    let fName = '';

    if (maxVoteRequired) {
      fName = negativeBlocks ? 'MaxWithBlock' : 'MaxNoBlock';
    } else {
      fName = negativeBlocks ? 'AnyWithBlock' : 'NoBlock';
    }
    this.set('_labelConfig.function', fName);

    this._updatingFunction = false;
  }

  _observeFunctionChange(_function) {
    if (this._updatingFunction) {
      return;
    }
    this._updatingFunction = true;

    this._negativeBlocks = _function.indexOf('WithBlock') !== -1;
    this._maxVoteRequired = _function.indexOf('Max') !== -1;

    this._updatingFunction = false;
  }

  _computeLabelConfig(labels, labelName) {
    this.linkPaths(['repoConfig.labels', labelName], '_labelConfig');
    this.linkPaths('_labelConfig', ['repoConfig.labels', labelName]);
    return labels[this.labelName] || {};
  }

  _observeCopyScoreRulesChange() {
    if (this._updatingCopyScoreRules) {
      return;
    }
    this._updatingCopyScoreRules = true;

    for (const key of COPY_SCORES) {
      this.set(['_copyScoreRules', key], false);
    }
    for (const value of this._labelConfig.copy_score_rules) {
      this.set(['_copyScoreRules', value], true);
    }

    this._updatingCopyScoreRules = false;
  }

  _observeCopyScoreRulesChangeInUi() {
    if (this._updatingCopyScoreRules) {
      return;
    }
    this._updatingCopyScoreRules = true;

    const newCopyScoreRules = [];
    for (const key in this._copyScoreRules) {
      if (this._copyScoreRules[key]) {
        newCopyScoreRules.push(key);
      }
    }
    this.set('_labelConfig.copy_score_rules', newCopyScoreRules);

    this._updatingCopyScoreRules = false;
  }
}

customElements.define(
    SimpleSubmitRulesLableConfig.is,
    SimpleSubmitRulesLableConfig
);