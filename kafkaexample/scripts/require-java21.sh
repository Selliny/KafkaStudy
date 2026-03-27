#!/bin/sh
set -eu

if [ -n "${JAVA_HOME-}" ]; then
  java_cmd="$JAVA_HOME/bin/java"
  javac_cmd="$JAVA_HOME/bin/javac"
  if [ ! -x "$java_cmd" ] || [ ! -x "$javac_cmd" ]; then
    echo "This project requires JDK 21+. JAVA_HOME does not point to a complete JDK." >&2
    echo "Set JAVA_HOME to a JDK 21 installation and rerun ./mvnw." >&2
    exit 1
  fi
else
  java_cmd="$(command -v java || true)"
  javac_cmd="$(command -v javac || true)"
  if [ -z "$java_cmd" ] || [ -z "$javac_cmd" ]; then
    echo "This project requires JDK 21+. Set JAVA_HOME to a JDK 21 installation and rerun ./mvnw." >&2
    exit 1
  fi
fi

version_line="$("$java_cmd" -version 2>&1 | awk 'NR==1 { print; exit }')"
raw_version="$(printf '%s\n' "$version_line" | sed -n 's/.*"\([^"]*\)".*/\1/p')"

if [ -z "$raw_version" ]; then
  echo "Unable to determine Java version from: $version_line" >&2
  exit 1
fi

case "$raw_version" in
  1.*) major_version="${raw_version#1.}" ; major_version="${major_version%%.*}" ;;
  *) major_version="${raw_version%%.*}" ;;
esac

case "$major_version" in
  ''|*[!0-9]*)
    echo "Unable to determine Java version from: $version_line" >&2
    exit 1
    ;;
esac

if [ "$major_version" -lt 21 ]; then
  echo "This project requires JDK 21+. mvnw resolved: $version_line" >&2
  echo "Set JAVA_HOME to a JDK 21 installation and rerun ./mvnw." >&2
  exit 1
fi
