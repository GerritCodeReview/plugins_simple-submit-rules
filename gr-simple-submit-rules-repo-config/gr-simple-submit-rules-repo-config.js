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
      _pluginRestApi: Object,
    },

    observers: [
      '_handleConfigChanged(_repoConfig.*)',
    ],

    attached() {
      return this._loadRepo();
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
      if (!this.repoName) { return; }
      const promises = [];

      promises.push(this._pluginRestApi().getLoggedIn().then(loggedIn => {
        this._loggedIn = loggedIn;
        if (loggedIn) {
          this._getRepoAccess(this.repoName).then(access => {
            if (!access) { return; }
            // If the user is not an owner, is_owner is not a property.
            this._readOnly = !access[this.repoName].is_owner;
          });
        }
      }));

      promises.push(this._pluginRestApi().get(this._endpointUrl())
        .then(config => {
          if (!config) { return; }
          this._repoConfig = config;
          this._loading = false;
        }));

      return Promise.all(promises);
    },

    _formatRepoConfigForSave(repoConfig) {
      return repoConfig;
    },

    _endpointUrl() {
      return 'projects/' + encodeURIComponent(this.repoName) + '/simple-submit-rules';
    },

    _handleSaveRepoConfig() {
      this._loading = true;
      return this._pluginRestApi().put(this._endpointUrl(), this._repoConfig)
        .then(config => {
          if (!config) { return Promise.resolve(); }

          this.dispatchEvent(new CustomEvent('show-alert', {
            detail: {
              message: 'Simple submit rules: configuration updated.'
            },
            bubbles: true
          }));

          this.set('_repoConfig', config);
          this._loading = false;
          this._configChanged = false;
        })
    },

    _pluginRestApi() {
      if (this._pluginRestApi === undefined) {
        this._pluginRestApi = this.plugin.restApi();
      }
      return this._pluginRestApi;
    },

    _getRepoAccess(repoName) {
      return this._pluginRestApi().get('/access/?project=' + encodeURIComponent(repoName));
    },

  });
})();
