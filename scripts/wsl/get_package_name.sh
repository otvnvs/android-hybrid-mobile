#!/bin/bash
DEVICE=$1
ADB=/mnt/c/usr/bin/adb.exe 
find ./|grep "app-debug.apk"|while read APK;do
	PACKAGENAME=$(aapt dump badging "$APK"|grep package | awk '{print $2}' | sed s/name=//g | sed s/\'//g)
	echo $PACKAGENAME
done
