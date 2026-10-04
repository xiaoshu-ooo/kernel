unset JAVA_TOOL_OPTIONS
unset _JAVA_OPTIONS
unset JDK_JAVA_OPTIONS

export ANDROID_HOME="/data/data/com.termux/files/usr/opt/android-sdk"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export JAVA_HOME="/data/data/com.termux/files/usr/lib/jvm/java-21-openjdk"

gradle --stop

gradle :app:assembleDebug \
  --no-daemon \
  --stacktrace