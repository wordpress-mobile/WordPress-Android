#!/bin/sh

if [ x"$1" == x ]; then
	checkstyle -c cq-configs/checkstyle/checkstyle.xml  -r src/
else
	checkstyle -c cq-configs/checkstyle/checkstyle.xml $@
fi