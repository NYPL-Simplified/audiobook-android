#!/bin/bash

#------------------------------------------------------------------------
# Utility methods

fatal()
{
  echo "build.sh: fatal: $1" 1>&2
  exit 1
}

info()
{
  echo "build.sh: info: $1" 1>&2
}

BUILD_TYPE="$1"
shift

if [ -z "${BUILD_TYPE}" ]
then
  BUILD_TYPE="normal"
fi

#------------------------------------------------------------------------
# Build the project
#

info "Executing build in '${BUILD_TYPE}' mode"

JVM_ARGUMENTS="-Xmx4096m -XX:+PrintGC -XX:+PrintGCDetails -XX:MaxMetaspaceSize=512m -XX:+HeapDumpOnOutOfMemoryError -Dfile.encoding=UTF-8"

info "Gradle JVM arguments: ${JVM_ARGUMENTS}"

case ${BUILD_TYPE} in
  normal)
    ./gradlew \
      -Dorg.gradle.jvmargs="${JVM_ARGUMENTS}" \
      -Dorg.gradle.daemon=false \
      -Dorg.gradle.parallel=false \
      -Dorg.gradle.internal.publish.checksums.insecure=true \
      ktlint assembleDebug test verifySemanticVersioning || fatal "could not build"

    ./gradlew \
      -Dorg.gradle.jvmargs="${JVM_ARGUMENTS}" \
      -Dorg.gradle.daemon=false \
      -Dorg.gradle.parallel=false \
      -Dorg.gradle.internal.publish.checksums.insecure=true \
      assembleRelease || fatal "could not build"
    ;;

  pull-request)
    ./gradlew \
      -Porg.librarysimplified.no_signing=true \
      -Dorg.gradle.jvmargs="${JVM_ARGUMENTS}" \
      -Dorg.gradle.daemon=false \
      -Dorg.gradle.parallel=false \
      -Dorg.gradle.internal.publish.checksums.insecure=true \
      ktlint assembleDebug test verifySemanticVersioning || fatal "could not build"
    ;;
esac
