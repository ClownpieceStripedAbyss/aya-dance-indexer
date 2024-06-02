#!/usr/bin/env bash

set -e -o pipefail

YOUTUBE_URL="$1"
AYA_CAT_ID="$2"
AYA_CAT_NAME="$3"

if [[ -z "$YOUTUBE_URL" || -z "$AYA_CAT_ID" || -z "$AYA_CAT_NAME" ]]; then
  echo "Usage: $0 <youtube-url> <category-id> <category-name>"
  exit 1
fi

cd "$(dirname "$(readlink -f "$0")")"

# Download indexer from GitHub release
echo ":: Downloading indexer fat jar..."
INDEXER_URL="https://github.com/ClownpieceStripedAbyss/aya-dance-indexer/releases/latest/download/aya-dance-indexer-fatjar.jar"
wget -O indexer.jar "$INDEXER_URL"

# Download yt-dlp
echo ":: Downloading yt-dlp..."
YT_DLP_URL="https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp_linux"
wget -O yt-dlp "$YT_DLP_URL"
chmod +x yt-dlp

echo ":: Computing available video ID..."
AYA_ID=$(bash ./find-available-id.sh)
echo "Available ID: $AYA_ID"

echo ":: Fetching video with ID $AYA_ID..."
rm -f "$AYA_ID"
bash ./fetch-video.sh "$YOUTUBE_URL" "$AYA_ID" "$AYA_CAT_ID" "$AYA_CAT_NAME" "./indexer.jar" "./yt-dlp"

echo ":: Uploading to staging area..."
bash ./upload-video.sh "staging_$AYA_ID" "$AYA_ID"
