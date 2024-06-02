#!/usr/bin/env bash

set -e

ssh_ip=$REMOTE_IP
ssh_user=$REMOTE_USER
ssh_path=$REMOTE_PATH

if [[ -z "$ssh_ip" || -z "$ssh_user" || -z "$ssh_path" ]]; then
  echo "Missing required environment variables: REMOTE_IP, REMOTE_USER, REMOTE_PATH"
  exit 1
fi

last_id=$(
  ssh \
    -o StrictHostKeyChecking=no \
    -o UserKnownHostsFile=/dev/null \
    -o LogLevel=ERROR \
    "$ssh_user@$ssh_ip" "ls -1 $ssh_path" | \
  grep -oP '^[0-9]+$' | \
  sort -u -n -r | \
  head -n1
)

echo $((last_id + 1))
