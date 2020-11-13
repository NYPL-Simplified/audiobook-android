#!/bin/bash

exec &> >(tee -a ".ci/build.log")

#------------------------------------------------------------------------
# Utility methods

fatal()
{
  echo "build.sh: fatal: $1" 1>&2
  echo
  echo "build.sh: dumping log: " 1>&2
  echo
  cat .ci/build.log
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

case ${BUILD_TYPE} in
  normal)
    ./gradlew \
      -Dorg.gradle.internal.publish.checksums.insecure=true \
      clean assembleRelease ktlint test verifySemanticVersioning || fatal "could not build"
    ;;

  pull-request)
    ./gradlew \
      -Porg.librarysimplified.no_signing=true \
      -Dorg.gradle.internal.publish.checksums.insecure=true \
      clean assembleDebug ktlint test verifySemanticVersioning || fatal "could not build"
    ;;
esac
