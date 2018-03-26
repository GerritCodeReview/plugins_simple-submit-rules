load("//tools/bzl:plugin.bzl", "gerrit_plugin", "PLUGIN_DEPS", "PLUGIN_TEST_DEPS")
load("//tools/bzl:junit.bzl", "junit_tests")

gerrit_plugin(
    name = "simple-submit-rules",
    srcs = glob(["src/main/java/**/*.java"]),
    manifest_entries = [
        "Gerrit-PluginName: simple-submit-rules",
        "Gerrit-Module: com.googlesource.gerrit.plugins.simplesubmitrules.EasyPreSubmitModule",
    ],
    resources = glob(["src/main/resources/**/*"]),
)
