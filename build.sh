#!/usr/bin/env bash
# Convenience wrapper: ForgeGradle 2.1 requires Java 8.
export JAVA_HOME=/usr/lib/jvm/java-8-openjdk
exec ./gradlew "$@" --no-daemon
