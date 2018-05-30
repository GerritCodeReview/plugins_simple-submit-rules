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
  Polymer({
    is: 'gr-simple-submit-rules-repo-config',

    properties: {
      repoName: String,
      /** @type {?} */
      _repoConfig: Object,
      _configChanged: {
        type: Boolean,
        value: false,
      },
      _readOnly: {
        type: Boolean,
        value: true,
      },
      _labels: {
        type: Array,
        value() { return []; },
        computed: '_computeLabelNames(_repoConfig.*)'
      },
    },

    observers: [
      '_handleConfigChanged(_repoConfig.*)',
    ],

    attached() {
      this._loadRepo();
    },

    _isLoading() {
      return this._loading || this._loading === undefined;
    },

    _handleConfigChanged() {
      if (this._isLoading()) { return; }
      this._configChanged = true;
    },

    _computeButtonDisabled(readOnly, configChanged) {
      return readOnly || !configChanged;
    },

    _loadRepo() {
      this.repoConfig = {};
      if (!this.repoName) { return Promise.resolve(); }
      const promises = [];

      promises.push(this.$.restAPI.getLoggedIn().then(loggedIn => {
        this._loggedIn = loggedIn;
        if (loggedIn) {
          this.$.restAPI.getRepoAccess(this.repoName).then(access => {
            if (!access) { return Promise.resolve(); }

            // If the user is not an owner, is_owner is not a property.
            this._readOnly = !access[this.repoName].is_owner;
          });
        }
      }));

      promises.push(new GrPluginRestApi().get(this._endpointurl())
        .then(config => {
          if (!config) { return Promise.resolve(); }
          this._repoConfig = config;
          this._loading = false;
        }));

      return Promise.all(promises);
    },

    _formatRepoConfigForSave(repoConfig) {
      return repoConfig;
    },

    _endpointurl() {
      return "/projects/" + encodeURIComponent(this.repoName) + "/simple-submit-rules";
    },

    _handleSaveRepoConfig() {
      this._loading = true;
      return new GrPluginRestApi().put(this._endpointurl(), this._repoConfig)
        .then(config => {
          if (!config) { return Promise.resolve(); }

          this.dispatchEvent(new CustomEvent('show-alert', {
            detail: {
              message: "Simple submit rules: configuration updated."
            },
            bubbles: true
          }));

          this._repoConfig = config;
          this._loading = false;
          this._configChanged = false;
        })
    },

    _computeLabelNames() {
      return Object.keys(this._repoConfig.labels);
    }
  });
})();
