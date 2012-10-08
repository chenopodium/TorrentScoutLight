#!/bin/bash
# Copyright (C) 2011 Ion Torrent Systems, Inc. All Rights Reserved
set -eu

temp_dir=./torrentscout

# Delete svn control directories
find $temp_dir -type d -name .svn -exec rm -rf {} \; || true

source ./version
export DEBEMAIL="Chantal Roth <chantal.roth@lifetech.com"
dch -b -v $MAJOR.$MINOR.$RELEASE

dpkg-buildpackage -us -uc

exit 0
