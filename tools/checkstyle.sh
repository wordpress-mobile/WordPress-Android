#!/bin/sh

DEFAULT_SRC_SOURCES=WordPress/src/main/java

if [ x"$1" == x ]; then
	checkstyle -c cq-configs/checkstyle/checkstyle.xml -r $DEFAULT_SRC_SOURCES
else
	checkstyle -c cq-configs/checkstyle/checkstyle.xml $@
fi
