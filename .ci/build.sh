#!/bin/bash

exec &> >(tee -a ".ci/build.log")

#------------------------------------------------------------------------
# Utility methods

fatal()
{
  echo "fatal: $1" 1>&2
  echo
  echo "dumping log: " 1>&2
  echo
  cat .ci/build.log
  exit 1
}

info()
{
  echo "info: $1" 1>&2
}

#------------------------------------------------------------------------
# Build the project
#

info "Executing build"
./gradlew \
  -Dorg.gradle.internal.publish.checksums.insecure=true \
  clean assemble ktlint test || fatal "could not build"
