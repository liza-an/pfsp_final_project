#!/usr/bin/env bash

set -e  # exit immediately if a command exits with a non-zero status
set -x  # print all executed commands on terminal

nohup bash ./staging_compose.sh tesla-stocks-collector up
nohup bash ./staging_compose.sh news-collector up
nohup bash ./staging_compose.sh musk-tweets-collector up
nohup bash ./staging_compose.sh streaming-app up

echo "Ahon'"