#!/bin/bash

SOURCE_DIR="../build/distributions"
CUSTOM_PLUGIN_DIR="./attachment"

if [ -d "$CUSTOM_PLUGIN_DIR" ]; then
  rm "$CUSTOM_PLUGIN_DIR"/*.zip
  echo "Directory already exists. delete and remake directory: $CUSTOM_PLUGIN_DIR"
else
  echo "Make directory: $CUSTOM_PLUGIN_DIR"
fi

mkdir -p "$CUSTOM_PLUGIN_DIR"

cp "$SOURCE_DIR"/* "$CUSTOM_PLUGIN_DIR"/

if [ $? -eq 0 ]; then
  echo "Custom plugin moved to docker volume successfully."
  echo "docker run"
  docker-compose -f ./docker-compose.yml up -d
else
  echo "Fail to move custom plugin, try gradle assemble to make custom plugin zip file"
fi

