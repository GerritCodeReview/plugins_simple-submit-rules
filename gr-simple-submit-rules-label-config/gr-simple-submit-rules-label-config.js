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

(function () {
  'use strict';

  const COPY_SCORES = [
    'copyMinScore',
    'copyMaxScore',
    'copyAllScoresOnTrivialRebase',
    'copyAllScoresIfNoCodeChange',
    'copyAllScoresIfNoChange',
    'copyAllScoresOnMergeFirstParentUpdate'
  ];

  Polymer({
    is: 'gr-simple-submit-rules-label-config',

    properties: {
      labelName: String,
      /** @type {?} */
      repoConfig: {
        type: Object,
        notify: true,
      },
      readOnly: Boolean,
      _updatingFunction: {
        type: Boolean,
        value: false
      },
      _updatingCopyScores: {
        type: Boolean,
        value: false
      },
      _labelConfig: {
        type: Object,
        computed: 'computeLabelConfig(repoConfig.labels, labelName)'
      },
      _copyScores: Object,
      _negativeBlocks: Boolean,
      _maxVoteRequired: Boolean,
    },

    observers: [
      'observeFunctionChange(_labelConfig.function)',
      'observeFunctionDescriptorChange(_negativeBlocks, _maxVoteRequired)',

      'observeCopyScoresChange(_labelConfig.copy_scores)',
      'observeCopyScoresChangeInUi(_copyScores.*)',
    ],

    ready() {
      this._copyScores = {};
      this.observeCopyScoresChange(this._labelConfig.copy_scores);
    },

    computeNegativeBlocks(_function) {
      return _function.indexOf('WithBlock') !== -1;
    },

    observeFunctionDescriptorChange(negativeBlocks, maxVoteRequired) {
      if (this._labelConfig === undefined) { return; }
      if (this._updatingFunction) { return; }
      this._updatingFunction = true;
      let fName = '';

      maxVoteRequired = maxVoteRequired === 'true' || maxVoteRequired === true;
      negativeBlocks = negativeBlocks === 'true' || negativeBlocks === true;
      if (maxVoteRequired) {
        fName = negativeBlocks ? 'MaxWithBlock' : 'MaxNoBlock';
      } else {
        fName = negativeBlocks ? 'AnyWithBlock' : 'NoBlock';
      }
      this.set('_labelConfig.function', fName);

      this._updatingFunction = false;
    },

    observeFunctionChange(_function) {
      if (this._updatingFunction) { return; }
      this._updatingFunction = true;

      this._negativeBlocks = _function.indexOf('WithBlock') !== -1;
      this._maxVoteRequired = _function.indexOf('Max') !== -1;

      this._updatingFunction = false;
    },

    computeLabelConfig(labels, labelName) {
      this.linkPaths(['repoConfig.labels', labelName], '_labelConfig');
      this.linkPaths('_labelConfig', ['repoConfig.labels', labelName]);
      return labels[this.labelName] || {};
    },

    observeCopyScoresChange() {
      if (this._updatingCopyScores) { return; }
      this._updatingCopyScores = true;

      for (let key of COPY_SCORES) {
        this.set(['_copyScores', key], 'false');
      }
      for (let value of this._labelConfig.copy_scores) {
        this.set(['_copyScores', value], 'true');
      }

      this._updatingCopyScores = false;
    },

    observeCopyScoresChangeInUi() {
      if (this._updatingCopyScores) { return; }
      this._updatingCopyScores = true;

      let newCopyScores = [];
      for (let key in this._copyScores) {
        if (this._copyScores[key] == 'true') {
          newCopyScores.push(key);
        }
      }
      this.set('_labelConfig.copy_scores', newCopyScores);

      this._updatingCopyScores = false;
    }
  });
})();
