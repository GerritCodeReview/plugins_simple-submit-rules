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

import './gr-simple-submit-rules-label-config.js';

class SimpleSubmitRulesRepoConfig extends Polymer.Element {
  static get is() {
    return 'gr-simple-submit-rules-repo-config';
  }

  static get properties() {
    return {
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
      _restApi: Object,
      _labels: {
        type: Array,
        value() {
          return [];
        },
        computed: '_computeLabelNames(_repoConfig.*)',
      },
    };
  }

  static get template() {
    return Polymer.html`
<style include="shared-styles"></style>
<style include="gr-form-styles"></style>

<main class="gr-form-styles">
  <h3 id="options">Simple Submit Rules</h3>

  <fieldset id="unresolved_comments">
    <section>
      <span class="title">
        Block submission if change has unresolved comments
      </span>
      <span class="value">
        <gr-select id="blockOnUnresolvedComments"
            bind-value="{{_repoConfig.comments.block_if_unresolved_comments}}">
          <select disabled$="[[_readOnly]]">
            <option value="true">Yes</option>
            <option value="false">No</option>
          </select>
        </gr-select>
      </span>
    </section>

    <template is="dom-repeat"
        id="allLabels"
        items="[[_labels]]"
        initial-count="5"
        target-framerate="60">
      <gr-simple-submit-rules-label-config mutable-data
          label-name="[[item]]"
          repo-config="{{_repoConfig}}"
          read-only="[[_readOnly]]">
      </gr-simple-submit-rules-label-config>
    </template>
  </fieldset>

  <gr-button on-tap="_handleSaveRepoConfig"
      disabled$="[[_computeButtonDisabled(_readOnly, _configChanged)]]">
    Save Changes
  </gr-button>
</main>`;
  }

  static get observers() {
    return ['_handleConfigChanged(_repoConfig.*)'];
  }

  connectedCallback() {
    super.connectedCallback();
    return this._loadRepo();
  }

  _isLoading() {
    return this._loading || this._loading === undefined;
  }

  _handleConfigChanged() {
    if (this._isLoading()) {
      return;
    }
    this._configChanged = true;
  }

  _computeButtonDisabled(readOnly, configChanged) {
    return readOnly || !configChanged;
  }

  _loadRepo() {
    this.repoConfig = {};
    if (!this.repoName) {
      return;
    }
    const promises = [];

    promises.push(
        this._pluginRestApi()
            .getLoggedIn()
            .then(loggedIn => {
              this._loggedIn = loggedIn;
              if (loggedIn) {
                this._getRepoAccess(this.repoName).then(access => {
                  if (!access) {
                    return;
                  }
                  // If the user is not an owner, is_owner is not a property.
                  this._readOnly = !access[this.repoName].is_owner;
                });
              }
            })
    );

    promises.push(
        this._pluginRestApi()
            .get(this._endpointUrl())
            .then(config => {
              if (!config) {
                return;
              }
              this.set('_repoConfig', config);
              this._loading = false;
            })
    );

    return Promise.all(promises);
  }

  _formatRepoConfigForSave(repoConfig) {
    return repoConfig;
  }

  _endpointUrl() {
    return (
      '/projects/' + encodeURIComponent(this.repoName) + '/simple-submit-rules'
    );
  }

  _handleSaveRepoConfig() {
    this._loading = true;
    return this._pluginRestApi()
        .put(this._endpointUrl(), this._repoConfig)
        .then(config => {
          if (!config) {
            return Promise.resolve();
          }

          this.dispatchEvent(
              new CustomEvent('show-alert', {
                detail: {
                  message: 'Simple submit rules: configuration updated.',
                },
                bubbles: true,
              })
          );

          this.set('_repoConfig', config);
          this._loading = false;
          this._configChanged = false;
        });
  }

  _pluginRestApi() {
    if (this._restApi === undefined) {
      this._restApi = this.plugin.restApi();
    }
    return this._restApi;
  }

  _getRepoAccess(repoName) {
    return this._pluginRestApi().get(
        '/access/?project=' + encodeURIComponent(repoName)
    );
  }

  _computeLabelNames() {
    if (this._repoConfig && this._repoConfig.labels) {
      return Object.keys(this._repoConfig.labels);
    }
    return [];
  }
}

customElements.define(
    SimpleSubmitRulesRepoConfig.is,
    SimpleSubmitRulesRepoConfig
);