#!/bin/sh
if [ "${1}" = "-debug" ] ; then
	export ANT_OPTS="${ANT_OPTS} -Xrunjdwp:transport=dt_socket,address=8787,server=y,suspend=y"
fi
ant -f src/test/ant/build.xml
