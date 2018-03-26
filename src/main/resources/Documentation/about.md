This plugin provides simple-to-use submit rules, hence the name.

## Features
- REST API to configure the options and labels.
- Ability to prevent submission if there are unresolved comments.
- Ability to require approval, and to consider approval from the change author or not.
- (soon!) a simple PolyGerrit UI to configure the labels and how they work.

### Inheritance
This plugin supports configuration inheritance, following a worst case scenario when
this is possible> That is, children projects can't use weaker options that their
parents.

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

The GET request serves a JSON response with the current settings. The scheme used in this
response is the same used for the PUT request body, so it will only be described once.

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

It is reasonable to consider that a label missing from the labels section won't be reset,
but consumers should not rely upon it.

### CommentsRules

```
{
  "block-if-unresolved-comments": boolean
}
```

When block-if-unresolved-comments is set to true, a Change with unresolved comments
CAN'T be submitted.

### LabelDefinition

```
{
  "function": LabelFunction,
  "ignore-self-approval": boolean,
  "copy-scores": CopyScoreRule[]
}
```

The LabelFunction specifies the behavior to use for this label.
It allows marking a label as mandatory, and defines if negative votes are blocking or not.
The list of functions is defined under.

When ignore-self-approval is set, the change author can't approve his own change.

The copy-scores list defines under what circumstances the votes (for this specific label)
should be kept. The list of rules is defined below.


### CopyScoreRule
CopyScoreRule value is an enum value, encoded as a string.

Each rule defines a behavior when a change is updated.

The possible options are: `copy-min-score`, `copy-max-score`, `copy-all-scores-if-no-change`
`copy-all-scores-if-no-code-change`, `copy-all-scores-on-merge-commit-first-parent-update`, and
`copy-all-scores-on-trivial-rebase`

See the Labels documentation page for more information.

### LabelFunction
The LabelFunction is an enum value, encoded as a string.

Example: `MaxWithBlock`, `AnyWithBlock`...

See the Labels documentation page for more information.
