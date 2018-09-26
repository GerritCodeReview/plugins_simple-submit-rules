This plugin provides simple-to-use submit rules, hence the name.

## Features
- REST API to configure the options and labels.
- Ability to prevent submission if there are unresolved comments.
- Ability to require approval, and to consider approval from the uploader of the
  latest patch set or not.
- (soon!) a simple PolyGerrit UI to configure the labels and how they work.

### Inheritance
This plugin supports configuration inheritance, following a worst case scenario when this is
possible. That is, children projects can't use weaker options that their parents.

The REST API provides a view of the configuration including inheritance.
The PUT method only changes the current project's configuration.

The Copy Scores are handled by Core Gerrit.

## The REST API
This plugin exposes a REST API to configure the labels and simple rules. It is available via the
following endpoints:

Anonymously: `@URL@projects/ProjectName/simple-submit-rules`

Authenticated: `@URL@a/projects/ProjectName/simple-submit-rules`

The authenticated endpoint uses the default Gerrit authentication and permissions systems.

The `READ_CONFIG` permission is required to access the configuration.

The `WRITE_CONFIG` permission is required to make changes to the configuration.

The project name in the URI must be urlencoded if it contains special characters like a slash.
This follows the current conventions used in Gerrit.

This endpoint is available using two HTTP methods: `GET` and `PUT`.

The GET request serves a JSON response with the current settings. The scheme used in this response
is the same used for the PUT request body, so it will only be described once.

## Schema

```
{
  "comments": CommentsRules,
  "labels": {
    "Verified": LabelDefinition,
    "Code-Review": LabelDefinition
  }
}
```

The comments section defines the rules to apply to comments (can the change be submitted with
unresolved comments, …). The labels section defines each label.

When reading labels on the API, the result includes both local and inherited labels.
When the configuration is modified through the API, the plugin will check if there are
local label configurations. If the request modifies an inherited label, it will be copied
down so that it can be modified locally.

### CommentsRules

```
{
  "block_if_unresolved_comments": boolean
}
```

When block_if_unresolved_comments is set to true, a Change with unresolved comments CAN'T be
submitted.

### LabelDefinition

```
{
  "function": LabelFunction,
  "ignore_self_approval": boolean,
  "copy_score_rules": CopyScoreRule[]
}
```

The LabelFunction specifies the behavior to use for this label.
It allows marking a label as mandatory, and defines if negative votes are blocking or not.
The list of functions is defined under.

When ignore_self_approval is set, the change author can't vote to approve their own change.

The copy_scores list defines under what circumstances the votes (for this specific label) should be
kept. The list of rules is defined below.


### CopyScoreRule
CopyScoreRule value is an enum value, encoded as a string.

Each rule defines a behavior when a change is updated.

The possible options are: `copyMinScore`, `copyMaxScore`, `copyAllScoresIfNoChange`
`copyAllScoresIfNoCodeChange`, `copyAllScoresOnMergeCommitFirstParentUpdate`, and
`copyAllScoresOnTrivialRebase`

See the Labels documentation page for more information.

### LabelFunction
The LabelFunction is an enum value, encoded as a string.

Example: `MaxWithBlock`, `AnyWithBlock`...

See the Labels documentation page for more information.

### Configuration in gerrit.config

The following is a list of configuration options that can be changed
in gerrit.config. All configs have to be nested under plugin.@PLUGIN@:

#### disallowedLabelFunctions-<label-name>

This config will prevent users from changing the function in the
label configuration that is referenced in the name to a matching value.
However, the config does not effect existing labels that already have
the forbidden value.

Example:
```
[plugin "simple-submit"]
  disallowedLabelFunctions-Code-Review = MaxNoBlock
```
