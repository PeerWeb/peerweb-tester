#!/bin/sh

. selenium_config.sh

hub="http://${HUB_HOST}:${HUB_PORT}"
visit="${VISIT_URL}"

java -jar target/peerweb-1.0-SNAPSHOT.jar -hub ${hub} -visit ${visit} ${@}
