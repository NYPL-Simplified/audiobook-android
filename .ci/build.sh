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

#------------------------------------------------------------------------
# Build the project
#

info "Executing build"
./gradlew \
  -Dorg.gradle.internal.publish.checksums.insecure=true \
  clean assemble ktlint test verifySemanticVersioning || fatal "could not build"
