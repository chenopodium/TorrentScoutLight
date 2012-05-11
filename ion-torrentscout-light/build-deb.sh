#!/bin/bash
# Copyright (C) 2011 Ion Torrent Systems, Inc. All Rights Reserved
set -eu

temp_dir=./torrentscout

function cleanup()
{
	rm -rf $1
}

trap "cleanup $temp_dir" 0

# the plugins/torrentscout directory gets copied wholesale into target
# directory so we have to stage the directory in order to purge the .svn control
# directories.

cp -rp ./plugins/torrentscout $temp_dir
# Delete svn control directories
find $temp_dir -type d -name .svn -exec rm -rf {} \; || true

source ./version
export DEBEMAIL="Chantal Roth <chantal.roth@lifetech.com"
dch -b -v $MAJOR.$MINOR.$RELEASE

dpkg-buildpackage -us -uc

exit 0
