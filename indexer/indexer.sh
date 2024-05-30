#!/usr/bin/env bash

set -e -o pipefail

YOUTUBE_URL="$1"
AYA_ID="$2"
AYA_CAT_ID="$3"
AYA_CAT_NAME="$4"

INDEXER_JAR="$5"
YT_DLP="$6"

if [[ -z "$YOUTUBE_URL" || -z "$AYA_ID" || -z "$AYA_CAT_ID" || -z "$AYA_CAT_NAME" ]]; then
  echo "Usage: $0 <youtube-url> <song-id> <category-id> <category-name> [indexer-jar] [yt-dlp]"
  exit 1
fi

if [[ -z "$INDEXER_JAR" ]]; then
  ./gradlew :indexer:fatJar
  INDEXER_JAR="./indexer/build/libs/indexer-*-fat.jar"
fi

if [[ -z "$YT_DLP" ]]; then
  YT_DLP="$(which yt-dlp)"
  if [[ -z "$YT_DLP" ]]; then
    echo "yt-dlp not found. Please install yt-dlp."
    exit 1
  fi
fi

mkdir -p "$AYA_ID"

$YT_DLP --no-check-certificate \
  --no-cache-dir \
  --rm-cache-dir \
  -f "(mp4/best)[height<=?2160][height>=?256][width>=?256]" \
  -o "$AYA_ID/video.mp4" \
  "$YOUTUBE_URL"
MD5SUM=$(md5sum "$AYA_ID/video.mp4" | cut -d' ' -f1)
java -jar "$INDEXER_JAR" "$YOUTUBE_URL" "$AYA_ID" "$AYA_CAT_ID" "$AYA_CAT_NAME" \
  | sed "s/CHECKSUM-PLACEHOLDER/$MD5SUM/g" \
  | tee "$AYA_ID/metadata.json"
