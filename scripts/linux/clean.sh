#!/bin/bash
JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64 ANDROID_SDK_ROOT=/home/skullquake/Android/ ./gradlew -x lint clean
EXITCODE=$?
test $EXITCODE -eq 0 && echo "cleaning complete" || echo "cleaning failed"
exit $EXITCODE
