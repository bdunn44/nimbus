#!/bin/sh

if [ "$(id -u)" != '0' ]; then
	echo "This script must be run as root." 1>&2
	exit 1
fi

SCRIPT_DIR=$(readlink -f "$0")
NIMBUS_HOME=$(dirname "$SCRIPT_DIR")
NIMBUS_JAVA=$(which java)

if [ ! -d "$NIMBUS_HOME" ]; then
	echo "The NIMBUS_HOME variable ($NIMBUS_HOME) is not set properly. Correct this value in /etc/default/$NAME or make sure you are running this script from the Nimbus installation directory."
	exit 1
fi

if [ ! -x "$NIMBUS_JAVA" ]; then
	echo "Java is not installed or the PATH varible for the root user is missing the Java directory"
	echo "Run './install_helper_programs.sh' to install Java and try again."
	exit 1
fi

while true; do
    read -p "Enter the username you want to run Nimbus as: " NIMBUS_USER
    if [ "x$NIMBUS_USER" = "x" ]; then
		echo "Please enter a username."
	elif id -u $NIMBUS_USER > /dev/null 2>&1; then
		break;
	else
		echo "That user does not exist. Please enter a valid username."
	fi
done

if [ "x$NIMBUS_USER" = "x" ]; then
	echo "The \$USER variable is not set. Set the variable to the user that should run Nimbus and re-run this script."
	echo "It is NOT recommended to run Nimbus as root."
	exit 1
fi

printf "NIMBUS_HOME=$NIMBUS_HOME\nNIMBUS_JAVA=$NIMBUS_JAVA\nNIMBUS_USER=$NIMBUS_USER" > /etc/default/nimbus
cp $NIMBUS_HOME/nimbus.sh /etc/init.d/nimbus
update-rc.d nimbus defaults > /dev/null
echo "Done!"
