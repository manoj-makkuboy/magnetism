#!/bin/sh

targetdir=$1

echo "Starting MySQL..."
/usr/bin/mysqld_safe --defaults-file=$targetdir/conf/my.cnf > /dev/null &
pid=$!

need_set_password=false
if [ ! -d $targetdir/data/mysql ] ; then
    echo "Creating database...."
    /usr/bin/mysql_install_db --datadir=$targetdir/data > /dev/null

    need_set_password=true
fi

started=false
failed=false

timeout=30
interval=1
while [ $timeout -gt 0 ] ; do
    if ps -p $pid > /dev/null ; then : ; else
	failed=true
	break
    fi
    #'ping' works fine if there is no such user, but requires login if there 
    # is a user. (Security leak? If this is fixed, we need to grep the
    # response on failure)
    if /usr/bin/mysqladmin -S $targetdir/run/mysql.sock -uUNKNOWN_MYSQL_USER ping > /dev/null 2>&1 ; then
	started=true
	break
    fi
 
    sleep $interval
    let timeout="$timeout - $interval"
done

if $started ; then
    if $need_set_password ; then
	eval `grep 'password=' $targetdir/conf/my.cnf`
	/usr/bin/mysqladmin -S $targetdir/run/mysql.sock -u root password $password
	echo "grant all on *.* to root@'127.0.0.1' identified by '$password'" | /usr/bin/mysql -S $targetdir/run/mysql.sock -u root --password=$password mysql
    fi
    echo "...sucessfully started"
    exit 0
elif $failed ; then
    echo "...failed to start"
    exit 1
else
    echo "...timed out"
    exit 1
fi

