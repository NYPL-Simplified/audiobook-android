#!/bin/bash

exec &> >(tee -a ".ci/main.log")

fatal()
{
  echo "main.sh: fatal: $1" 1>&2
  exit 1
}

info()
{
  echo "main.sh: info: $1" 1>&2
}

if [ $# -ne 1 ]
then
  fatal "usage: build-type
  Where: build-type is one of 'pull-request' or 'normal'
"
fi

BUILD_TYPE="$1"
shift

case ${BUILD_TYPE} in
  pull-request)
    info "Building in pull-request mode"
    info "Credentials will not be used"
    info "Builds will not be deployed"

    .ci/build.sh pull-request || fatal "Could not build"
    ;;

  normal)
    info "Building in normal mode"
    info "Credentials will be used"
    info "Builds will be deployed"

    .ci/credentials.sh || fatal "Could not set up credentials"
    .ci/build.sh normal || fatal "Could not build"
    .ci/deploy.sh || fatal "Could not deploy"
    ;;
esac