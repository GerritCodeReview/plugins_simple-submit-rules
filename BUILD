load("//tools/bzl:plugin.bzl", "gerrit_plugin", "PLUGIN_DEPS", "PLUGIN_TEST_DEPS")
load("//tools/bzl:junit.bzl", "junit_tests")

gerrit_plugin(
    name = "simple-submit-rules",
    srcs = glob(["src/main/java/**/*.java"]),
    manifest_entries = [
        "Gerrit-PluginName: simple-submit-rules",
        "Gerrit-Module: com.googlesource.gerrit.plugins.simplesubmitrules.SimpleSubmitRulesModule",
    ],
    resources = glob(["src/main/resources/**/*"]),
)

ABSTRACT_TEST_BASE = ["src/test/java/com/googlesource/gerrit/plugins/simplesubmitrules/AbstractSimpleSubmitRulesIT.java"]

java_library(
    name = "abstract_test_base",
    testonly = 1,
    srcs = ABSTRACT_TEST_BASE,
    visibility = ["//visibility:public"],
    deps = PLUGIN_TEST_DEPS + PLUGIN_DEPS + [
        ":simple-submit-rules__plugin",
    ],
)

junit_tests(
    name = "tests",
    srcs = glob(
        ["src/test/java/**/*.java"],
        exclude = ABSTRACT_TEST_BASE,
    ),
    visibility = ["//visibility:public"],
    deps = PLUGIN_TEST_DEPS + PLUGIN_DEPS + [
        ":abstract_test_base",
        ":simple-submit-rules__plugin",
    ],
)
