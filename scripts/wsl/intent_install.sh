#!/bin/bash
ADB=/mnt/c/usr/bin/adb.exe
$ADB shell am start -a android.intent.action.VIEW -d "ahm-app://deploy?package_url=https://github.com/otvnvs/ahm-asset-delivery-scanner/archive/refs/heads/main.zip" com.example.app
