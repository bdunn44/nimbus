#!/bin/sh

require_root()
{
	# Require root
	if [ "$(id -u)" != '0' ]
	then
		echo "This script must be run as root." 1>&2
		exit 1
	fi
}

check_nimbus_home()
{
	SCRIPT_DIR=$(readlink -f "$0")
	NIMBUS_HOME=$(dirname "$SCRIPT_DIR")
	NIMBUS_JAR=$NIMBUS_HOME/lib/`ls $NIMBUS_HOME/lib | egrep nimbus-server-.*\.jar | cut -f 1`
	# Exit if Nimbus JAR wasn't located
	if [ ! -f "$NIMBUS_JAR" ]
	then
		echo "Could not find the Nimbus installation. The Nimbus home directory ('$NIMBUS_HOME') may be incorrect."
		exit 1
	fi
}

require_nimbus_stopped()
{
	# Check if Nimbus is running
	if [ -f $NIMBUS_HOME/logs/nimbus.pid ]
	then
		echo "Can't configure SSL while Nimbus is running. Please stop Nimbus first."
		exit 1
	fi
}

read_port_config()
{
	# Get HTTP/S ports
	PORT=`grep 'nimbus\.http\.port=' $NIMBUS_HOME/conf/nimbus.properties | cut -d '=' -f 2`
	SSLPORT=`grep 'nimbus\.ssl\.https\.port=' $NIMBUS_HOME/conf/nimbus.properties | cut -d '=' -f 2`
	[ "x$PORT" = "x" ] && PORT=8080
	[ "x$SSLPORT" = "x" ] && SSLPORT=8443
}

prompt_continue()
{
	CONFIRM=
	echo -n "Would you like to continue? [Y/N]: "
	read CONFIRM
	[ "$CONFIRM" = "y" ] || [ "$CONFIRM" = "Y" ] || exit 0
}

prompt_email()
{
	EMAIL=
	while true
	do
		echo -n "Enter an email address used for Let's Encrypt registration and recovery: "
		read EMAIL
		[ "x$EMAIL" = "x" ] || break
	done
}

prompt_domains()
{
	DOMAINS=
	while true
	do
		echo -n "Enter a comma or space delimited list of domain names to create a certificate for: "
		read DOMAINS
		[ "x$DOMAINS" = "x" ] || break
	done
	DOMAINS=`echo $DOMAINS | tr ' ' ',' | tr -s ','`
}

check_certbot()
{
	if [ ! -f "$NIMBUS_HOME/ssl/certbot/certbot-auto" ]
	then
		echo -n "Downloading the Let's Encrypt certbot... "
		mkdir $NIMBUS_HOME/ssl/certbot/
		wget -q -P $NIMBUS_HOME/ssl/certbot/ https://dl.eff.org/certbot-auto
		chmod a+x $NIMBUS_HOME/ssl/certbot/certbot-auto
		echo "Done"
	fi
}

run_certbot()
{
	check_certbot
	echo "Running certbot to generate SSL certificate(s)... "
	$NIMBUS_HOME/ssl/certbot/certbot-auto certonly --standalone -t -m $EMAIL -d "$DOMAINS" --http-01-port $PORT --tls-sni-01-port $SSLPORT
	if [ $? -ne 0 ]
	then
		echo "Certbot finished in error. Exiting the utility."
		exit 1
	fi
	echo
	echo "Certbot process finished."
}

run_certbot_renew()
{
	check_certbot
	$NIMBUS_HOME/ssl/certbot/certbot-auto renew --standalone --http-01-port $PORT --tls-sni-01-port $SSLPORT --pre-hook "sh $NIMBUS_HOME/nimbus.sh stop >/dev/null 2>&1" --noninteractive --quiet
}

prompt_keystore_password()
{
	KS_PW=
	KS_PWC=
	KS_PWOBF=
	while true
	do
		stty -echo
		printf "Enter a keystore password: "
		read KS_PW
		printf "\n"
		if [ "x$KS_PW" = "x" ]
		then
			printf "Password cannot be empty\n"
			continue
		fi
		printf "Confirm the keystore password: "
		read KS_PWC
		printf "\n"
		stty echo
		if [ "$KS_PW" != "$KS_PWC" ]
		then
			echo "Those passwords don't match."
			KS_PW=
			KS_PWC=
		else
			break
		fi
	done
	# Not obfuscating at the moment... This complicates renewal
	#JETTY_UTIL_JAR=$NIMBUS_HOME/lib/`ls $NIMBUS_HOME/lib | egrep jetty-util.*\.jar | cut -f 1`
	#KS_PWOBF=`java -cp $JETTY_UTIL_JAR org.eclipse.jetty.util.security.Password $KS_PW | grep OBF`
}

convert_jks()
{
	CERTPATH=/etc/letsencrypt/live
	CERTSFILE=$NIMBUS_HOME/ssl/certs.pem
	[ -f "$CERTSFILE" ] && rm $CERTSFILE
	touch $CERTSFILE
	for domain in $(ls $CERTPATH)
	do
		# Build pem file
		if [ -d "$CERTPATH/$domain" ]
		then
			cat $CERTPATH/$domain/privkey.pem >> $CERTSFILE
			cat $CERTPATH/$domain/cert.pem >> $CERTSFILE
		fi
	done

	[ -f "$NIMBUS_HOME/ssl/keystore.pkcs12" ] && rm $NIMBUS_HOME/ssl/keystore.pkcs12
	echo $KS_PW | openssl pkcs12 -export -out $NIMBUS_HOME/ssl/keystore.pkcs12 -in $CERTSFILE -passout stdin
	[ -f "$NIMBUS_HOME/ssl/keystore.jks" ] && rm $NIMBUS_HOME/ssl/keystore.jks
	keytool -importkeystore -srckeystore $NIMBUS_HOME/ssl/keystore.pkcs12 -srcstoretype PKCS12 -destkeystore $NIMBUS_HOME/ssl/keystore.jks -srcstorepass $KS_PW -storepass $KS_PW -noprompt >/dev/null 2>&1
	rm $NIMBUS_HOME/ssl/keystore.pkcs12
	rm $CERTSFILE
}

update_nimbus_config()
{
	echo -n "Updating Nimbus configuration file... "
	sed -i 's:^[ \t]*nimbus.ssl.enabled[ \t]*=\([ \t]*.*\)$:nimbus.ssl.enabled=true:' $NIMBUS_HOME/conf/nimbus.properties
	sed -i 's:^[ \t]*nimbus.ssl.keystore.path[ \t]*=\([ \t]*.*\)$:nimbus.ssl.keystore.path=\${nimbus.home}/ssl/keystore.jks:' $NIMBUS_HOME/conf/nimbus.properties
	sed -i 's:^[ \t]*nimbus.ssl.keystore.password[ \t]*=\([ \t]*.*\)$:nimbus.ssl.keystore.password='${KS_PW}':' $NIMBUS_HOME/conf/nimbus.properties
	echo "Done."
}

add_renew_crontab()
{
	echo "Let's Encrypt certificates expire every 90 days, after which SSL ecnryption will stop working."
	CONFIRM=
	echo -n "Would you like to setup a weekly job to automatically renew your certificate(s) if needed? [Y/N]: "
	read CONFIRM
	if [ "$CONFIRM" = "y" ] || [ "$CONFIRM" = "Y" ]
	then
		echo -n "Adding a cron job to automatically renew your SSL certificate(s)... "
		echo "#!/bin/sh\nsh $NIMBUS_HOME/setup_encryption.sh -renew" > /etc/cron.weekly/nimbus-ssl-renew
		chmod 771 /etc/cron.weekly/nimbus-ssl-renew
		echo "Done"
	fi
}

check_nimbus_running()
{
	PIDFILE=$NIMBUS_HOME/logs/nimbus.pid
	NIMBUS_RUNNING=
	if [ -f "$PIDFILE" ]
	then 
		if ps -p `cat $NIMBUS_HOME/logs/nimbus.pid` >/dev/null 2>&1
		then 
			NIMBUS_RUNNING=0
		else
			# Delete the invalid PID file
			rm -f $PIDFILE
		fi
	fi
	[ "x$NIMBUS_RUNNING" = "x" ] && NIMBUS_RUNNING=1
}

start_nimbus()
{
	sh $NIMBUS_HOME/nimbus.sh start >/dev/null 2>&1
}

read_keystore_config()
{
	KS_PW=`grep 'nimbus\.ssl\.keystore\.password=' $NIMBUS_HOME/conf/nimbus.properties | cut -d '=' -f 2`
}

pause()
{
	sleep 1
}

case "$1" in
	"")
		require_root
		check_nimbus_home
		require_nimbus_stopped
		read_port_config
		echo "Welcome to the SSL configuration utility"
		echo "This utility is powered by letsencrypt.org. Before starting, please ensure that:"
		echo "  1. Your Raspberry Pi is accessible from the internet using the HTTP and HTTPS ports configured in $NIMBUS_HOME/conf/nimbus.properties (currently $PORT and $SSLPORT)."
		echo "  2. You have configured Dynamic DNS or have otherwise setup a domain that is properly configured to reach your Pi."
		pause
		prompt_continue
		prompt_email
		echo "Thanks. Next you'll need to enter a list of domains to get certificates for. To be flexible with 'www.' you'll probably need at least two entries."
		echo "For example: cloudnimbus.org, www.cloudnimbus.org"
		pause
		prompt_domains
		run_certbot
		echo "Next we'll setup the Let's Encrypt certificate for Nimbus."
		pause
		prompt_keystore_password
		# this was echoing in the renew job even with stty for some reason, so moved it here
		echo -n "Converting the Let's Encrypt certificate to JKS format... "
		convert_jks
		echo "Done."
		update_nimbus_config
		pause
		add_renew_crontab
		echo "Nimbus SSL encryption has been enabled"
	;;
	"-renew")
		stty -echo
		require_root
		check_nimbus_home
		read_port_config
		check_nimbus_running
		run_certbot_renew
		read_keystore_config
		convert_jks
		if [ "$NIMBUS_RUNNING" = "0" ]
		then
			check_nimbus_running
			if [ "$NIMBUS_RUNNING" = "1" ]
			then
				start_nimbus
			fi
		fi
		stty echo
	;;
	*)
		echo "Usage: setup_encryption.sh [-renew]" >&2
		exit 3
	;;
esac
exit 0