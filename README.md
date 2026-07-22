# Android Hybrid Mobile Application

Basic Android Hybrid Mobile application.

## Prerequisites - Windows

Todo...

## Prerequisites - WSL

Todo...

## Prerequisites - Linux

```bash
sudo apt install openjdk-11-jdk -y
sudo apt install openjdk-17-jdk -y
mkdir -p $HOME/Android/cmdline-tools
cd $HOME/Android/
wget https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
unzip commandlinetools-linux-11076708_latest.zip -d cmdline-tools/
mv cmdline-tools/cmdline-tools cmdline-tools/latest
rm commandlinetools-linux-11076708_latest.zip
yes | JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 $HOME/Android/cmdline-tools/latest/bin/sdkmanager --licenses

# install emulator
yes | JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 $HOME/Android/cmdline-tools/latest/bin/sdkmanager --install "emulator"
sudo apt install qemu-kvm libvirt-daemon-system libvirt-clients bridge-utils -y
sudo adduser $USER kvm
sudo apt install util-linux-extra -y
newgrp kvm
yes | JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 $HOME/Android/cmdline-tools/latest/bin/sdkmanager --install "system-images;android-35;google_apis;x86_64"
yes | JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 $HOME/Android/cmdline-tools/latest/bin/sdkmanager --install "platform-tools"
# select no for custom profile
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 $HOME/Android/cmdline-tools/latest/bin/avdmanager create avd -n "Medium_Phone_API_35" -k "system-images;android-35;google_apis;x86_64"
```

Remote emulator connection

```bash
# ubuntu
~/Android/platform-tools/adb kill-server
./scripts/linux/emulator.sh --headless --cold
~/Android/platform-tools/adb devices

# wsl
ssh -CN -L 5556:127.0.0.1:5555 skullquake@192.168.0.44
/mnt/c/opt/scrcpy/adb.exe kill-server
/mnt/c/opt/scrcpy/adb.exe connect 127.0.0.1:5556
/mnt/c/opt/scrcpy/scrcpy.exe -s 127.0.0.1:5556
```

## Prerequisites - Termux

Todo...

# Building

```bash
./gradlew assemble
```

Or commit with message `release[:title[:description]]` and `./.github/workflows/build.yml` will build a new Release.
