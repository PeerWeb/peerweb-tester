#!/bin/sh
# https://stackoverflow.com/questions/9567682/how-to-set-up-selenium-with-chromedriver-on-jenkins-hosted-grid

. selenium_config.sh

port=$(seq ${NODE_MIN_PORT} ${NODE_MAX_PORT} | gsort -R | head -n1)

echo "Starting Selenium node on port ${port}..."

java -jar selenium-server-standalone-2.42.2.jar \
     -Dwebdriver.chrome.driver="chromedriver" \
     -role node \
     -hub "http://${HUB_HOST}:${HUB_PORT}" \
     -port ${port}

