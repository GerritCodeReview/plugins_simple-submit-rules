load("//tools/bzl:junit.bzl", "junit_tests")
load("//tools/bzl:plugin.bzl", "gerrit_plugin", "PLUGIN_DEPS", "PLUGIN_TEST_DEPS")

junit_tests(
    name = "simple_submit_rules_tests",
    srcs = glob(["**/*.java"]),
    deps = PLUGIN_TEST_DEPS + PLUGIN_DEPS + [
        "//plugins/simple-submit-rules",
    ],
)
