#!/usr/bin/env sh

APP_HOME=$(cd "$(dirname "$0")" >/dev/null 2>&1 && pwd)
exec java -cp "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain "$@"
