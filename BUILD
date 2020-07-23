load("//tools/bzl:genrule2.bzl", "genrule2")
load("//tools/bzl:js.bzl", "polygerrit_plugin")
load("//tools/bzl:junit.bzl", "junit_tests")
load("//tools/bzl:plugin.bzl", "PLUGIN_DEPS", "PLUGIN_TEST_DEPS", "gerrit_plugin")
load("@npm//@bazel/rollup:index.bzl", "rollup_bundle")

gerrit_plugin(
    name = "simple-submit-rules",
    srcs = glob(["src/main/java/**/*.java"]),
    manifest_entries = [
        "Gerrit-PluginName: simple-submit-rules",
        "Gerrit-Module: com.googlesource.gerrit.plugins.simplesubmitrules.Module",
        "Gerrit-BatchModule: com.googlesource.gerrit.plugins.simplesubmitrules.BatchModule",
    ],
    resource_jars = [":ssr-static"],
    resources = glob(["src/main/resources/**/*"]),
)

junit_tests(
    name = "tests",
    srcs = glob(
        ["src/test/java/**/*.java"],
    ),
    visibility = ["//visibility:public"],
    deps = PLUGIN_TEST_DEPS + PLUGIN_DEPS + [
        ":simple-submit-rules__plugin",
    ],
)

genrule2(
    name = "ssr-static",
    srcs = [":simple-submit-rules_ui"],
    outs = ["ssr-static.jar"],
    cmd = " && ".join([
        "mkdir $$TMP/static",
        "cp -rp $(locations :simple-submit-rules_ui) $$TMP/static",
        "cd $$TMP",
        "zip -Drq $$ROOT/$@ -g .",
    ]),
)

polygerrit_plugin(
    name = "simple-submit-rules_ui",
    app = "plugin-bundle.js",
    plugin_name = "simple-submit-rules",
)

rollup_bundle(
    name = "plugin-bundle",
    srcs = glob([
        "submit-rules/*.js",
    ]),
    entry_point = "plugin.js",
    format = "iife",
    rollup_bin = "//tools/node_tools:rollup-bin",
    sourcemap = "hidden",
    deps = [
        "@tools_npm//rollup-plugin-node-resolve",
    ],
)
