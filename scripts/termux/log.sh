#!/bin/bash
adb logcat --pid=$(adb shell pidof com.example.app)

