#!/bin/sh
### BEGIN INIT INFO
# Provides:          nimbus
# Required-Start:    $remote_fs $syslog
# Required-Stop:     $remote_fs $syslog
# Default-Start:     2 3 4 5
# Default-Stop:      0 1 6
# Short-Description: Nimbus
# Description:       A personal cloud web application
### END INIT INFO

# Author: Bryson Dunn <bryson@cloudnimbus.org>

DESC="Nimbus"
NAME=nimbus

# Read configuration variable file if it is present
if [ -r /etc/default/$NAME ]
then 
	. /etc/default/$NAME
else
	SCRIPT_DIR=$(readlink -f "$0")
	NIMBUS_HOME=$(dirname "$SCRIPT_DIR")
	NIMBUS_JAVA=$(which java)
	NIMBUS_USER=$USER
fi

if [ ! -d "$NIMBUS_HOME" ]
then
	echo "The \$NIMBUS_HOME variable ($NIMBUS_HOME) is not set properly. Correct this value in /etc/default/$NAME or make sure you are running this script from the Nimbus installation directory."
	exit 0
fi

PIDFILE=$NIMBUS_HOME/logs/nimbus.pid
SCRIPTNAME=/etc/init.d/$NAME

#
# Function that starts the daemon
#
do_start()
{
	# Return
	#   0 if daemon has been started
	#   1 if daemon was already running
	#   2 if daemon could not be started
	
	do_status_check
	[ "0" = "$?" ] && return 1
	
	# Exit if Java isn't installed or Nimbus Home isn't set correctly
	if [ ! -x "$NIMBUS_JAVA" ]
	then 
		echo "Java is not installed!" 
		return 2
	fi

	# Exit if Nimbus User isn't set
	if [ "x$NIMBUS_USER" = "x" ]
	then
		echo "The \$NIMBUS_USER variable is not set. Correct this value in /etc/default/$NAME or set the \$USER variable if you are running this script from the installation directory."
		echo "It is NOT recommended to run Nimbus as root."
		return 2
	fi
	
	# Find the Nimbus JAR file
	NIMBUS_JAR=$NIMBUS_HOME/lib/`ls $NIMBUS_HOME/lib | egrep nimbus-server-.*\.jar | cut -f 1`

	# Exit if Nimbus JAR wasn't located
	if [ ! -f "$NIMBUS_JAR" ]
	then
		echo "Could not find the Nimbus installation. The Nimbus home directory ('$NIMBUS_HOME') may be incorrect."
		return 2
	fi
	
	# Calculate max memory allocation as 75% of total
	MEM=`cat /proc/meminfo | grep MemTotal | awk '{printf "%d",$2*.75}'`

	# Define the startup command and arguments
	DAEMON="sudo -u $NIMBUS_USER $NIMBUS_JAVA"
	DAEMON_ARGS="-ea -Djava.security.egd=file:/dev/urandom -Dprogram.name=Nimbus -Dnimbus.home=$NIMBUS_HOME -Xms64m -Xmx${MEM}k -Xss500k -jar $NIMBUS_JAR"
	[ "$2" = '-port' ] && [ "x$3" != 'x' ] && DAEMON_ARGS="$DAEMON_ARGS --httpPort=$3"
	
	# Print startup information
	echo "+++++++++++++++++++++++++++++++++++++++++++"
	echo "+  NIMBUS CONFIGURATION"
	if [ -r /etc/default/$NAME ]
	then 
		echo "+  Configuration file: /etc/default/$NAME"
	fi
	echo "+  Nimbus Home: $NIMBUS_HOME"
	echo "+  Startup command: $DAEMON $DAEMON_ARGS"
	echo "+++++++++++++++++++++++++++++++++++++++++++"
	
	# Run Nimbus daemon in the background
	nohup $DAEMON $DAEMON_ARGS >/dev/null 2>&1 &
	if [ "0" = "$?" ]
	then
		sleep 2 # Wait for nohup child process to start
		ps --ppid $! -o pid= | tr -d ' ' | tr -d '\n' > $PIDFILE # Get PID of child process
		echo "Nimbus startup initiated. Checking status of PID `cat $PIDFILE`..."
		sleep 5
		do_status_check
		[ "$?" = "0" ] && return 0 # Success
		[ -f "$PIDFILE" ] && rm "$PIDFILE" # Failure, delete pid file 
	fi
	return 2 # Failure
}

#
# Function that stops the daemon
#
do_stop()
{
	# Return
	#   0 if daemon has been stopped
	#   1 if daemon could not be stopped
	#	2 if daemon is not running
	
	do_status_check
	[ "0" = "$?" ] || return 2
	PID=$(cat "$PIDFILE")
	[ "x$PID" != 'x' ] && kill $PID >/dev/null 2>&1
	if [ "$?" = "0" ]
	then
		rm -f "$PIDFILE"
		return 0
	else
		return 1
	fi
}

#
# Function that checks the status of the daemon
#
do_status_check()
{
	# Return
	#   0 if daemon is running
	#	1 if daemon is not running
	
	if [ -f "$PIDFILE" ]
	then 
		if ps -p `cat $NIMBUS_HOME/logs/nimbus.pid` > /dev/null 2>&1
		then 
			return 0
		else
			# Delete the invalid PID file
			rm -f $PIDFILE
		fi
	fi
	return 1
}

RET=0
case "$1" in
	start)
		echo "Starting $DESC..."
		do_start
		RET=$?
		case $RET in
			0) echo "$DESC was started successfully." ;;
			1) echo "$DESC is already running." ;;
			2) echo "$DESC could not be started." ;;
		esac
	;;
	stop)
		echo "Stopping $DESC..."
		do_stop
		RET=$?
		case $RET in
			0) echo "$DESC stopped." ;;
			1) echo "Unable to stop $DESC." ;;
			2) echo "$DESC is not running." ;;
		esac
	;;
	status)
		do_status_check
		case "$?" in 
			0) echo "$DESC is running." ;;
			1) echo "$DESC is stopped." ;;
		esac
		RET=0
	;;
	restart|force-reload)
		echo "Restarting $DESC..."
		do_stop
		RET=$?
		case $RET in
			0) echo "$DESC stopped." ;;
			1) 
				echo "Unable to stop $DESC."
				return 
				;;
			# Ignore 2 - we dont' care if it's not running
		esac
		do_start
		RET=$?
		case $RET in
			0) echo "$DESC was started successfully." ;;
			1) echo "$DESC is already running." ;;
			2) echo "$DESC could not be started." ;;
		esac
	;;
	*)
		echo "Usage: $SCRIPTNAME [start|stop|status|restart]" >&2
		exit 3
	;;
esac
exit $RET