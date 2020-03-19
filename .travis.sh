#!/bin/sh

cat <<EOF

org.gradle.daemon=true
org.gradle.configureondemand=true
org.gradle.jvmargs=-Xmx4g -XX:MaxPermSize=2048m -XX:+HeapDumpOnOutOfMemoryError

EOF >> gradle.properties

exec ./gradlew clean assemble test
