#!/bin/sh

PACKAGES="ntfs-3g pmount avahi-daemon avahi-discover libnss-mdns hwinfo openssl"

if [ "$(id -u)" != '0' ]; then
	echo "This script must be run as root." 1>&2
	exit 1
fi

echo "Updating package repository...."
apt-get update --quiet

if [ "x$(which java)" = 'x' ]; then
	echo "Installing Java 8....."
	apt-get install oracle-java8-jdk --quiet --yes
fi

echo "Installing packages...."
apt-get install $PACKAGES --quiet --yes
