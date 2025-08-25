#!/usr/bin/env sh

#
# Copyright 2015 the original author or authors.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# Add default JVM options here. You can also use JAVA_OPTS and GRADLE_OPTS to pass any JVM options to Gradle and Java processes.
# For more information, see https://docs.gradle.org/current/userguide/build_environment.html
DEFAULT_JVM_OPTS=""

APP_NAME="Gradle"
APP_BASE_NAME=`basename "$0"`

# Use the maximum available, or set MAX_FD != -1 to use that value.
MAX_FD="maximum"

warn () {
    echo "$*"
}

die () {
    echo
    echo "ERROR: $*"
    echo
    exit 1
}

# OS specific support (must be 'true' or 'false').
cygwin=false
msys=false
darwin=false
nonstop=false
case "`uname`" in
  CYGWIN* )
    cygwin=true
    ;;
  Darwin* )
    darwin=true
    ;;
  MINGW* )
    msys=true
    ;;
  NONSTOP* )
    nonstop=true
    ;;
esac

# Attempt to set APP_HOME
# Resolve links: $0 may be a link
PRG="$0"
# Need this for relative symlinks.
while [ -h "$PRG" ] ; do
    ls=`ls -ld "$PRG"`
    link=`expr "$ls" : '.*-> \(.*\)$'`
    if expr "$link" : '/.*' > /dev/null; then
        PRG="$link"
    else
        PRG=`dirname "$PRG"`"/$link"
    fi
done
APP_HOME=`dirname "$PRG"`

# For Cygwin, ensure paths are in UNIX format before anything is touched
if $cygwin ; then
    [ -n "$APP_HOME" ] &&
        APP_HOME=`cygpath --unix "$APP_HOME"`
    [ -n "$JAVA_HOME" ] &&
        JAVA_HOME=`cygpath --unix "$JAVA_HOME"`
    [ -n "$CLASSPATH" ] &&
        CLASSPATH=`cygpath --path --unix "$CLASSPATH"`
fi

# Attempt to find Java
if [ -n "$JAVA_HOME" ] ; then
    if [ -x "$JAVA_HOME/jre/sh/java" ] ; then
        # IBM's JDK on AIX uses strange locations for the executables
        JAVACMD="$JAVA_HOME/jre/sh/java"
    else
        JAVACMD="$JAVA_HOME/bin/java"
    fi
    if [ ! -x "$JAVACMD" ] ; then
        die "ERROR: JAVA_HOME is set to an invalid directory: $JAVA_HOME

Please set the JAVA_HOME variable in your environment to match the
location of your Java installation."
    fi
else
    JAVACMD="java"
    which java >/dev/null 2>&1 || die "ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.

Please set the JAVA_HOME variable in your environment to match the
location of your Java installation."
fi

# Increase the maximum file descriptors if we can
if ! $cygwin && ! $darwin && ! $nonstop; then
    MAX_FD_LIMIT=`ulimit -H -n`
    if [ $? -eq 0 ] ; then
        if [ "$MAX_FD" = "maximum" -o "$MAX_FD" = "max" ] ; then
            # Use the system limit
            MAX_FD="$MAX_FD_LIMIT"
        fi
        ulimit -n $MAX_FD
        if [ $? -ne 0 ] ; then
            warn "Could not set maximum file descriptor limit: $MAX_FD"
        fi
    else
        warn "Could not query system maximum file descriptor limit: $MAX_FD_LIMIT"
    fi
fi

# Add the jar to the classpath
GRADLE_WRAPPER_JAR="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"

# Split up the JVM options string into an array, following the shell quoting and substitution rules
function jvm_options_to_array {
    touch "$APP_HOME/gradle/wrapper/gradle-wrapper.properties"
    jvm_options_property_file="$APP_HOME/gradle/wrapper/gradle-wrapper.properties"
    default_jvm_opts_property="org.gradle.jvmargs"
    eval set -- $(
        "$JAVACMD" \
            -Dorg.gradle.appname="$APP_BASE_NAME" \
            -Xmx64m -Xms64m \
            -cp "$GRADLE_WRAPPER_JAR" \
            org.gradle.wrapper.Main \
            "jvmargs" \
            --property-file "$jvm_options_property_file" \
            --default-jvm-opts "$DEFAULT_JVM_OPTS" \
            --property "$default_jvm_opts_property"
    )
    DEFAULT_JVM_OPTS=("$@")
}
jvm_options_to_array

# Collect all arguments for the java command, following the shell quoting and substitution rules
eval set -- $(
    "$JAVACMD" \
        -Dorg.gradle.appname="$APP_BASE_NAME" \
        -Xmx64m -Xms64m \
        -cp "$GRADLE_WRAPPER_JAR" \
        org.gradle.wrapper.Main \
        "args" \
        "${DEFAULT_JVM_OPTS[@]}" \
        "$@"
)

# Execute Gradle
exec "$JAVACMD" "${DEFAULT_JVM_OPTS[@]}" -Dorg.gradle.appname="$APP_BASE_NAME" -cp "$GRADLE_WRAPPER_JAR" org.gradle.wrapper.GradleWrapperMain "$@"
