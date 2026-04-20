#!/bin/sh
# Gradle wrapper script for POSIX systems
APP_NAME="Gradle"
APP_BASE_NAME=$(basename "$0")
APP_HOME=$(cd "$(dirname "$0")" && pwd)
MAX_FD="maximum"
GRADLE_OPTS="${GRADLE_OPTS:-}"
DEFAULT_JVM_OPTS=""

warn() { echo "$*"; }
die() { echo; echo "ERROR: $*"; echo; exit 1; }

if [ "$APP_HOME" = "" ]; then APP_HOME="."; fi
CLASSPATH="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"
JAVACMD="${JAVA_HOME:+$JAVA_HOME/bin/java}"
JAVACMD="${JAVACMD:-java}"

set -- \
  -classpath "$CLASSPATH" \
  org.gradle.wrapper.GradleWrapperMain \
  "$@"

exec "$JAVACMD" $DEFAULT_JVM_OPTS $JAVA_OPTS $GRADLE_OPTS "$@"
