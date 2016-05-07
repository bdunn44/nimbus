#!/bin/sh

SCRIPT_DIR=$(readlink -f "$0")
NIMBUS_HOME=$(dirname "$SCRIPT_DIR")
NIMBUS_JAR=$NIMBUS_HOME/lib/`ls $NIMBUS_HOME/lib | egrep nimbus-server-.*\.jar | cut -f 1`

if [ -f $NIMBUS_HOME/data/nimbusdb.lck ]; then
	echo "The password reset utility can't be used while Nimbus is running. Please stop Nimbus first."
	exit 1
fi

java -Dnimbus.home=$NIMBUS_HOME -cp $NIMBUS_JAR com.kbdunn.nimbus.server.security.ResetPassword