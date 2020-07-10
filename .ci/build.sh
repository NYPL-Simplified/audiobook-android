#!/bin/bash

#------------------------------------------------------------------------
# Utility methods

fatal()
{
  echo "fatal: $1" 1>&2
  exit 1
}

info()
{
  echo "info: $1" 1>&2
}

#------------------------------------------------------------------------
# Build the project
#

(cat <<EOF

org.gradle.daemon=true
org.gradle.configureondemand=true
org.gradle.jvmargs=-Xmx4g -XX:MaxPermSize=2048m -XX:+HeapDumpOnOutOfMemoryError
EOF
) >> gradle.properties

info "Executing build"
./gradlew \
  -Dorg.gradle.internal.publish.checksums.insecure=true \
  clean assemble ktlint test || fatal "could not build"
