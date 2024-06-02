#!/usr/bin/env bash

set -e

DIR="$1"
REMOTE_DIR_NAME="$2"

ssh_ip=$REMOTE_IP
ssh_user=$REMOTE_USER
ssh_path=$REMOTE_PATH

if [[ -z "$ssh_ip" || -z "$ssh_user" || -z "$ssh_path" ]]; then
  echo "Missing required environment variables: REMOTE_IP, REMOTE_USER, REMOTE_PATH"
  exit 1
fi

if [[ -z "$DIR" ]]; then
  echo "Usage: $0 <directory> [remote-directory-name]"
  exit 1
fi

if [[ ! -d "$DIR" ]]; then
  echo "Directory not found: $DIR"
  exit 1
fi

scp \
  -o StrictHostKeyChecking=no \
  -o UserKnownHostsFile=/dev/null \
  -o LogLevel=ERROR \
  -r "$DIR" "$ssh_user@$ssh_ip:$ssh_path/${REMOTE_DIR_NAME:-$(basename "$DIR")}"
