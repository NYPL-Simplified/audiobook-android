#!/bin/sh

exec ./gradlew clean assemble test connectedAndroidTest
