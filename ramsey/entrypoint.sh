#!/bin/sh
set -e

if [ -z "$TESSDATA_PREFIX" ]; then
  DETECTED=$(find /usr/share -maxdepth 4 -type d -name tessdata 2>/dev/null | head -n1)
  if [ -n "$DETECTED" ]; then
    export TESSDATA_PREFIX="$DETECTED"
    echo "Auto-detected TESSDATA_PREFIX=$TESSDATA_PREFIX"
  else
    echo "WARNING: could not auto-detect a tessdata directory under /usr/share." >&2
  fi
fi

exec java -jar /app/app.jar
