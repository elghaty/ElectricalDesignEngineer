#!/usr/bin/env sh

# Minimal Gradle wrapper launcher. The wrapper JAR selects the configured
# Gradle distribution from gradle/wrapper/gradle-wrapper.properties.
APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
exec java -classpath "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain "$@"
