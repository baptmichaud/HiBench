#!/bin/bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

echo "Install directory: $DIR"

/bin/cp -f conf/hibench.conf.huge.current conf/hibench.conf
/bin/cp -f conf/hadoop.conf.current conf/hadoop.conf
/bin/cp -f conf/benchmarks.lst.partial conf/benchmarks.lst

