#!/usr/bin/env bash
# run the EPD pipeline
#
. /etc/profile
APPNAME=EPD

APPDIR=/home/rgddata/pipelines/$APPNAME
cd $APPDIR
pwd
DB_OPTS="-Dspring.config=$APPDIR/../properties/default_db.xml"
LOG4J_OPTS="-Dlog4j.configuration=file://$APPDIR/properties/log4j.properties"
declare -x "${APPNAME}_OPTS=$DB_OPTS $LOG4J_OPTS"
bin/$APPNAME "$@"