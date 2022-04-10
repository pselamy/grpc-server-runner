workspace(name = "grpc_server_runner")

load("@bazel_tools//tools/build_defs/repo:git.bzl", "git_repository")

git_repository(
    name = "rules_jvm_external",
    remote = "https://github.com/bazelbuild/rules_jvm_external",
    tag = "4.2",
)

load("@rules_jvm_external//:defs.bzl", "maven_install")

maven_install(
    artifacts = [
        "com.google.guava:guava:31.1-jre",
        "com.google.inject:guice:5.1.0",
        "io.grpc:grpc-api:1.45.1",
    ],
    repositories = [
        "https://repo1.maven.org/maven2",
    ],
)
