#!/bin/bash

exec &> >(tee -a ".ci/credentials.log")

#------------------------------------------------------------------------
# Utility methods
#

fatal()
{
  echo "credentials.sh: fatal: $1" 1>&2
  echo
  echo "credentials.sh: Dumping log: " 1>&2
  echo
  cat .ci/credentials.log
  exit 1
}

info()
{
  echo "credentials.sh: info: $1" 1>&2
}

#------------------------------------------------------------------------
# Check environment
#

if [ -z "${MAVEN_CENTRAL_USERNAME}" ]
then
  fatal "MAVEN_CENTRAL_USERNAME is not defined"
fi
if [ -z "${MAVEN_CENTRAL_PASSWORD}" ]
then
  fatal "MAVEN_CENTRAL_PASSWORD is not defined"
fi
if [ -z "${MAVEN_CENTRAL_STAGING_PROFILE_ID}" ]
then
  fatal "MAVEN_CENTRAL_STAGING_PROFILE_ID is not defined"
fi
if [ -z "${MAVEN_CENTRAL_SIGNING_KEY_ID}" ]
then
  fatal "MAVEN_CENTRAL_SIGNING_KEY_ID is not defined"
fi
if [ -z "${NYPL_GITHUB_ACCESS_TOKEN}" ]
then
  fatal "NYPL_GITHUB_ACCESS_TOKEN is not defined"
fi

#------------------------------------------------------------------------
# Clone credentials repos
#

info "Cloning credentials"

git clone \
  --depth 1 \
  "https://${NYPL_GITHUB_ACCESS_TOKEN}@github.com/NYPL-Simplified/Certificates" \
  ".ci/credentials" || fatal "Could not clone credentials"

#------------------------------------------------------------------------
# Import the PGP key for signing Central releases, and try to sign a test
# file to check that the key hasn't expired.
#

info "Importing GPG key"
gpg --import ".ci/credentials/APK Signing/librarySimplified.asc" || fatal "Could not import GPG key"

info "Signing test file"
echo "Test" > hello.txt || fatal "Could not create test file"
gpg --sign -a hello.txt || fatal "Could not produce test signature"

#------------------------------------------------------------------------
# Download Brooklime if necessary.
#

BROOKLIME_URL="https://repo1.maven.org/maven2/com/io7m/brooklime/com.io7m.brooklime.cmdline/0.1.0/com.io7m.brooklime.cmdline-0.1.0-main.jar"
BROOKLIME_SHA256_EXPECTED="d706dee5ce6be4992d35b3d61094872e194b7f8f3ad798a845ceb692a8ac8fcd"

wget -O "brooklime.jar.tmp" "${BROOKLIME_URL}" || fatal "Could not download brooklime"
mv "brooklime.jar.tmp" "brooklime.jar" || fatal "Could not rename brooklime"

BROOKLIME_SHA256_RECEIVED=$(openssl sha256 "brooklime.jar" | awk '{print $NF}') || fatal "Could not checksum brooklime.jar"

if [ "${BROOKLIME_SHA256_EXPECTED}" != "${BROOKLIME_SHA256_RECEIVED}" ]
then
  fatal "brooklime.jar checksum does not match.
  Expected: ${BROOKLIME_SHA256_EXPECTED}
  Received: ${BROOKLIME_SHA256_RECEIVED}"
fi
