#!/bin/sh

TS=`date +'%Y%m%d%M%S'`
SCRIPT_DIR=$(readlink -f "$0")
NIMBUS_HOME=$(dirname "$SCRIPT_DIR")
NIMBUS_JAR=$NIMBUS_HOME/lib/`ls $NIMBUS_HOME/lib | egrep nimbus-server-.*\.jar | cut -f 1`
NIMBUS_VER=`echo $NIMBUS_JAR | cut -d '-' -f 3`
# Get from version.txt if it exists
if [ -f $NIMBUS_HOME/logs/version.txt ]; then
	NIMBUS_VER=`cat $NIMBUS_HOME/logs/version.txt`
fi

# Exit if Nimbus JAR wasn't located
if [ ! -f "$NIMBUS_JAR" ]; then
	echo "Could not find the Nimbus installation. The Nimbus home directory ('$NIMBUS_HOME') may be incorrect."
	exit 1
fi

# Check if upgrade is supported (older than 0.6.1)
MAJOR=`echo $NIMBUS_VER | cut -d '.' -f 1`
MINOR=`echo $NIMBUS_VER | cut -d '.' -f 2`
DOT=`echo $NIMBUS_VER | cut -d '.' -f 3`
if [ $MAJOR -eq 0 -a $MINOR -lt 6 ] || [ $MAJOR -eq 0 -a $MINOR -eq 6 -a $DOT -eq 0 ]; then
	echo "Upgrading Nimbus installations older than release 0.6.1 is not supported. "
	exit 1
fi

# Check if Nimbus is running
if [ -f $NIMBUS_HOME/logs/nimbus.pid ]; then
	echo "Nimbus cannot be upgraded while running. Please stop Nimbus first."
	exit 1
fi

# The upgrade script is intended to be run from the existing installation directory.
# The user can either supply the newer version's distribution file (.tar.gz) as the first argument
# or enter it using the prompt

echo "Welcome to the Nimbus upgrade utility"

SRC_NIMBUS_DIST="$1"
SRC_NIMBUS_HOME=''
SRC_NIMBUS_JAR=''
SRC_NIMBUS_VER=''

do_dist_check() 
{
	# Not null and is valid file
	if [ "x$SRC_NIMBUS_DIST" = "x" ]; then
		echo "Please enter the path to the Nimbus distribution."
		return 1
	elif [ ! -f $SRC_NIMBUS_DIST ]; then 
		echo "That's not a valid file."
		return 1
	fi
	return 0
}

do_dist_extract()
{
	# Extract dist to tmp
	SRC_NIMBUS_HOME=/tmp/nimbus-$TS
	[ ! -d $SRC_NIMBUS_HOME ] && mkdir $SRC_NIMBUS_HOME
	tar -zxf $SRC_NIMBUS_DIST -C $SRC_NIMBUS_HOME #&>/dev/null
	if [ "$?" != "0" ]; then
		echo "Unable to extract Nimbus distribution!"
		return 1
	fi
	return 0
}

do_src_version_detect()
{
	SRC_NIMBUS_JAR=$SRC_NIMBUS_HOME/lib/`ls $NIMBUS_HOME/lib | egrep nimbus-server-.*\.jar | cut -f 1`
	if [ ! -f "$SRC_NIMBUS_JAR" ]; then
		echo "Nimbus distribution is corrupt!"
		return 1
	else
		SRC_NIMBUS_VER=`echo $SRC_NIMBUS_JAR | cut -d '-' -f 3`
		# Quick fix for the 0.6.1.1118 release - create the version file
		if [ "$NIMBUS_VER" = "0.6.1" ] && [ ! -f $NIMBUS_HOME/logs/version.txt ]; then
			echo "0.6.1.1118" > $NIMBUS_HOME/logs/version.txt
		fi
		# Get from version.txt if it exists
		if [ -f $SRC_NIMBUS_HOME/logs/version.txt ]; then
			SRC_NIMBUS_VER=`cat $NIMBUS_HOME/logs/version.txt`
		fi
		return 0
	fi
}

# Get the path to the new Nimbus distribution and extract it
if [ "x" = "x$SRC_NIMBUS_DIST" ]; then
	read -p "Enter the path to the new Nimbus distribution file: " SRC_NIMBUS_DIST
fi

echo -n "Checking the target Nimbus distribution file... "
do_dist_check
[ "$?" != "0" ] && exit 1
do_dist_extract
[ "$?" != "0" ] && exit 1
do_src_version_detect
[ "$?" != "0" ] && exit 1
echo "Done"

# Backup old installation directory
echo -n "Backing up the current installation... "
BKP="nimbus-$NIMBUS_VER-$TS.tar.gz"
tar -czf /tmp/$BKP --exclude='logs/*.tar.gz' -C `dirname $NIMBUS_HOME` `basename $NIMBUS_HOME`
mv /tmp/$BKP $NIMBUS_HOME/logs/$BKP
echo "`date +'%Y-%m-%dT%H:%M:%S.000'`: Nimbus installation backed up to $NIMBUS_HOME/logs/$BKP" >> $NIMBUS_HOME/logs/upgrades.log
echo "Done"
echo "Your current Nimbus installation at '$NIMBUS_HOME' has been archived to $NIMBUS_HOME/logs/$BKP"

# Do the upgrade
java -cp $SRC_NIMBUS_JAR com.kbdunn.nimbus.server.upgrade.UpgradeRunner "$SRC_NIMBUS_HOME" "$NIMBUS_HOME"

echo -n "Cleaning up... "
# Make scripts executable again
chmod +x $NIMBUS_HOME/*.sh
# Clean up tmp
rm -rf $SRC_NIMBUS_HOME
echo "Done"
