#!/bin/sh
# https://stackoverflow.com/questions/9567682/how-to-set-up-selenium-with-chromedriver-on-jenkins-hosted-grid

. selenium_config.sh

echo "Starting Selenium hub on port ${HUB_PORT}..."

java -jar selenium-server-standalone-2.42.2.jar \
     -role hub \
     -port ${HUB_PORT}

