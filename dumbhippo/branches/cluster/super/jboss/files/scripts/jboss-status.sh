#!/bin/sh

jbossdir=@@jbossdir@@
targetdir=@@targetdir@@
jnpPort=@@jnpPort@@
jbossBind="@@jbossBind@@"

#
# Running twiddle can be quite slow, so check first by pid/ps
#
pidfile=$targetdir/run/jboss.pid
if [ \! -f $pidfile ] ; then 
    exit 1
fi

pid=`cat $pidfile`
if ps -p $pid > /dev/null ; then : ; else
    exit 1
fi

if test "$jbossBind" = 'all'; then
  jnphost="localhost"
else
  jnphost="$jbossBind"
fi

result="`JAVA_OPTS=-Dorg.jboss.logging.Logger.pluginClass=org.jboss.logging.NullLoggerPlugin $jbossdir/bin/twiddle.sh -s jnp://$jnphost:$jnpPort get jboss.system:type=Server Started --noprefix`"
if [ $? == 0 -a x"$result" == x"true" ] ; then
    exit 0
else
    exit 1
fi
