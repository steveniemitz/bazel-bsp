load("//:junit5.bzl", "java_junit5_test", "kt_junit5_test")

java_junit5_test(
    name = "InstallationContextTest",
    size = "small",
    srcs = ["InstallationContextTest.java"],
    test_package = "org.jetbrains.bsp.bazel.installationcontext",
    deps = [
        "//executioncontext/installationcontext/src/main/java/org/jetbrains/bsp/bazel/installationcontext",
    ],
)

kt_junit5_test(
    name = "InstallationContextConstructorTest",
    size = "small",
    srcs = ["InstallationContextConstructorTest.kt"],
    test_package = "org.jetbrains.bsp.bazel.installationcontext",
    deps = [
        "//executioncontext/installationcontext/src/main/java/org/jetbrains/bsp/bazel/installationcontext",
    ],
)
