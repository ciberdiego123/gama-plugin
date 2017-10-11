#!/bin/sh

git clone https://github.com/openmole/openmole.git --branch 7-dev
cd openmole
git submodule init
git submodule update
cd build-system
sbt publish-local
cd ../libraries
sbt publish-local
cd ../openmole
sbt "project openmole" assemble
sbt publish-local
cd
