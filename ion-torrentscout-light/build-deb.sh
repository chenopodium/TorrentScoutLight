#!/bin/bash
# Copyright (C) 2012 Ion Torrent Systems, Inc. All Rights Reserved
set -eu

source ./version
export DEBEMAIL="Chantal Roth <chantal.roth@lifetech.com"
dch -b -v $MAJOR.$MINOR.$RELEASE

dpkg-buildpackage -us -uc

exit 0
