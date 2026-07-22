#!/bin/bash
DEVICE=$1
ADB=~/Android/platform-tools/adb
find ./|grep "app-debug.apk"|while read APK;do
	PACKAGENAME=$(aapt dump badging "$APK"|grep package | awk '{print $2}' | sed s/name=//g | sed s/\'//g)
	echo $PACKAGENAME
done
