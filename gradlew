#!/bin/bash
# Gradle wrapper startup script for POSIX

# Determine the Java command to use
if [ -n "$JAVA_HOME" ] ; then
    JAVACMD="$JAVA_HOME/bin/java"
else
    JAVACMD="java"
fi

# Resolve the script directory
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

# Set Java options
DEFAULT_JVM_OPTS="-Xmx64m -Xms64m"

# Execute Gradle
exec "$JAVACMD" $DEFAULT_JVM_OPTS -classpath "$SCRIPT_DIR/gradle/wrapper/gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain "$@"
