#!/bin/sh

targetdir=$1

echo "Stopping MySQL..."

if [ \! -f $targetdir/run/mysqld.pid ] ; then 
    echo "... not running"
    exit 0
fi

pid=`cat $targetdir/run/mysqld.pid`

if kill $pid ; then : ; else
    echo "... not running"
    exit 0
fi

timeout=30
interval=1
while [ $timeout -gt 0 ] ; do
    if ps -p $pid > /dev/null ; then : ; else
	echo "... stopped"
	exit 0
    fi

    sleep $interval
    let timeout="$timeout - $interval"
done

echo "...timed out"
exit 1

