#!/bin/sh

grep "VERSION_NAME=" gradle.properties | awk -F= '{print $NF}'