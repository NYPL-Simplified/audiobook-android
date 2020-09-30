#!/bin/sh

grep "VERSION_NAME=" gradle.properties | grep -v PREVIOUS | awk -F= '{print $NF}'