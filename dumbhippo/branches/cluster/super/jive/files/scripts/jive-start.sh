#!/bin/sh

targetdir=@@targetdir@@
twiddle="@@twiddle@@"
slaveMode="@@slaveMode@@"

echo "Starting Jive Wildfire..."

######################################################################

@@if mysqlEnabled
if test x"$slaveMode" != xyes; then
mysqlTargetdir="@@mysqlTargetdir@@"
mysqlOptions="@@mysqlOptions@@"
dbcommand="/usr/bin/mysql $mysqlOptions jive"

if [ -d $mysqlTargetdir/data/jive ] ; then : ; else
    /usr/bin/mysqladmin $mysqlOptions create jive
    $dbcommand < $targetdir/resources/database/wildfire_mysql.sql
fi
fi
@@elif pgsqlEnabled
if test x"$slaveMode" != xyes; then
pgsqlOptions="@@pgsqlOptions@@"
dbcommand="/usr/bin/psql $pgsqlOptions jive"

if [ echo "" | $dbcommand > /dev/null 2>&1 ] ; then : ; else
    /usr/bin/createdb $pgsqlOptions -O dumbhippo jive
    $dbcommand < $targetdir/resources/database/wildfire_mysql.sql
fi
fi
@@else
@@  error "No database"
@@endif

######################################################################

if test x"$slaveMode" != xyes; then
$dbcommand <<EOF
DELETE FROM jiveProperty ;
INSERT INTO jiveProperty VALUES ( 'xmpp.socket.plain.interface', '@@jnpHost@@') ;
INSERT INTO jiveProperty VALUES ( 'xmpp.socket.plain.port', @@jivePlainPort@@ ) ;
INSERT INTO jiveProperty VALUES ( 'xmpp.socket.ssl.port', @@jiveSecurePort@@ ) ;
INSERT INTO jiveProperty VALUES ( 'xmpp.server.socket.port', @@jiveServerPort@@ ) ;
INSERT INTO jiveProperty VALUES ( 'xmpp.component.socket.port', @@jiveComponentPort@@ ) ;
INSERT INTO jiveProperty VALUES ( 'xmpp.domain', 'dumbhippo.com' ) ;
INSERT INTO jiveProperty VALUES ( 'xmpp.client.tls.policy', 'disabled') ;
EOF
fi

eval $twiddle invoke jboss.system:service=MainDeployer deploy file://$targetdir/deploy/wildfire.sar/ > /dev/null

started=false

timeout=30
interval=1
while [ $timeout -gt 0 ] ; do
    if $targetdir/scripts/jive-running.py ; then
	started=true
	break
    fi
done

if $started ; then
    echo "...sucessfully started"
    exit 0
else
    echo "...timed out"
    exit 1
fi
