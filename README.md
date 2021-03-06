<center>
<img src="https://i.imgur.com/UruLe2H.png" alt="Drawing" width="100px"/>
<img src="https://i.imgur.com/DW2erAV.png" alt="Drawing" width="200px"/>
</center>

[![Build Status](https://travis-ci.org/openmole/gama-plugin.svg?branch=master)](https://travis-ci.org/openmole/gama-plugin)

# Installation of prebuild Gama plugins

## Download the set of Gama plugin for you OpenMOLE version

Version 7: https://jenkins.iscpif.fr/job/gama-plugin-7dev/lastSuccessfulBuild/artifact/bundles.tgz

Version 8: https://jenkins.iscpif.fr/job/gama-plugin-8dev/lastSuccessfulBuild/artifact/bundles.tgz

## Start OpenMole with Gama-plugin

You could list all OpenMole option typing `openmole -h` command in your terminal.

Into `gama-plugin/` root folder, run OpenMole with plugin `-p` (load all the jars from plugin): 

`openmole -p ./bundles/`

Alternatively you can also upload them in the plugin pannel of the OpenMOLE interface.


# Installation of Gama Plugin from scratch

# Ubuntu 

## Install Git LFS

OpenMOLE use git LFS in order to download large file during clonning process.

```bash
curl -s https://packagecloud.io/install/repositories/github/git-lfs/script.deb.sh | sudo bash
sudo apt-get install git-lfs
git lfs install
```

## Install JDK 8

package `openjdk-8-jdk` name may vary accroding to your linux distribution / version

```bash
sudo apt-get install openjdk-8-jdk
update-alternatives --config java
update-alternatives --config javac
```
If you jave multiple version of java on your computer, you can manage the different version easily using the [jenv project](http://www.jenv.be/).

## Install SBT

You could find the information to install sbt 0.13 on [sbt site](http://www.scala-sbt.org/download.html)

```bash
echo "deb https://dl.bintray.com/sbt/debian /" | sudo tee -a /etc/apt/sources.list.d/sbt.list
sudo apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv 2EE0EA64E40A89B84B2DF73499E82A75642AC823
sudo apt-get update
sudo apt-get install sbt
```

## OpenMole

Gama-plugin need some [OpenMole](http://openmole.org) packages to work. We use the latest released version.

You could clone the released version here `git clone https://github.com/openmole/openmole.git`

Go to openmole folder using `cd openmole` command.

You can list the different tag available for OpenMole using the `git tag` command. If you want to use the latest dev version, so could run `git checkout dev` into the root openmole folder. 

In order to compile OpenMole (see [official documentation ](https://next.openmole.org/Documentation_Development_Compilation.html) here ) you need to run these commands into your terminal : 

```bash
git submodule init
git submodule update

cd build-system
sbt publish-local
cd ../libraries
sbt publish-local
cd ../openmole
sbt "project openmole" assemble
sbt publish-local
```

You can find OpenMole application in `bin/openmole/target/assemble/`

Run `./openmole` in this folder to launch openmole. It should open in your internet browser, if you're not on a distant server. If this is the case, you need to indicate the port using `--port xxxx` option where `xxxx` is the port number, like `8181` for example.  

__Optional commands :__ 

Add `OpenMole` directly to your `.bashrc` (change `mygitrepositories` by the correct folder): 

`export PATH=$HOME/mygitrepositories/openmole/openmole/bin/openmole/target/assemble:$PATH
`
Close and reopen your terminal (or run `source .bashrc` in your `home` directory) to take in account this modification. 

Test by typing `openmole` in terminal.

## Gama-plugin

Clone this repository using `git clone https://github.com/openmole/gama-plugin`

Checkout the branch which correspond to openmole version you use : `git checkout master` for latest OpenMOLE release or some dev branch if you are crazy (see branches available with `git branch`)

You could now compile `gama-plugin` using these commands : 
```bash 
cd gama-plugin
sbt generateTask
```

Everything is awesome when you're part of a team, sbt download all the jar needed by OpenMole and Gama to the `/bundles` folder.

__Warning :__ Request and download of __jar__ from P2 repository could be very very slow, so this is normal if the sbt compilation is very long to finish.

## OpenMole with Gama-plugin

You could list all OpenMole option typing `openmole -h` command in your terminal.

Into `gama-plugin/` root folder, run OpenMole with plugin `-p` (load all the jars from plugin): 

`openmole -p ./bundles/ --logger-level FINE`

If everything is ok :+1:, you could see something like that when OpenMole start : 

```
>GAMA plugin loaded in 1096 ms:         msi.gama.core
>GAMA plugin loaded in 4 ms:    msi.gama.headless
>GAMA plugin loaded in 29 ms:   irit.gaml.extensions.database
>GAMA plugin loaded in 8 ms:    msi.gama.lang.gaml
>GAMA plugin loaded in 14 ms:   ummisco.gaml.extensions.maths
>GAMA plugin loaded in 50 ms:   simtools.gaml.extensions.traffic
>GAMA total load time 1315 ms.
```

## Optional Steps for Gama Developers

This is optional step, only if you are a Gama developer and want to works with the latest version of gama development.

You need to install `maven` version 3.3.9 

`sudo apt-get install maven`

Next, clone and compile the gama project : 

```bash
git clone https://github.com/gama-platform/gama.git
cd gama
/.build.sh
```

If you're a dev and you have credentials (into `~/.m2/settings.xml`), you can also modify the file `/msi.gama.p2updatesite/pom.xml` to deploy the latest dev version to this p2 online repository `sftp://gama.unthinkingdepths.fr`

Replace this two existing lines by :

```xml
<ftp.url>sftp://gama.unthinkingdepths.fr</ftp.url>
<ftp.toDir>/home/reyman64/webapps/gamap2/</ftp.toDir>
```
The `/home/reyman64/webapps/gamap2/` is the correct absolute dir to upload p2 site on the server.


Then, run these commands to deploy : 

`./deploy.sh`

When you compile the gama-plugin, you need to see some log like that, which indicate that sbt use the latest compiled local jar of Gama : 

```bash
[warn] [OSGi manager:*] The following locally built units have been used to resolve project dependencies:
[warn] [OSGi manager:*]   ummisco.gama.annotations/1.7.0.201702020957
[warn] [OSGi manager:*]   msi.gama.lang.gaml/1.7.0.201702021000
[warn] [OSGi manager:*]   irit.gaml.extensions.database/1.7.0.201702021000
[warn] [OSGi manager:*]   msi.gama.core/1.7.0.201702021000
[warn] [OSGi manager:*]   ummisco.gaml.extensions.maths/1.7.0.201702021000
[warn] [OSGi manager:*]   msi.gama.processor/1.4.0
[warn] [OSGi manager:*]   simtools.gaml.extensions.traffic/1.7.0.201702021000
[warn] [OSGi manager:*]   msi.gama.ext/1.7.0.201702021000
[warn] [OSGi manager:*]   msi.gama.headless/1.7.0.201702021000
```

##  Execute a workflow launching a Gama model in OpenMOLE:

```scala
// Declare the variable
val number_of_preys = Var[Int]
val nb_preys_init = Var[Int]

// Gama task
// The third argument of the GamaTask is the gama experiment name
// and the fourth argument is the number of steps
val gama = 
  GamaTask("/path/to/predatorPrey.gaml", "preyPred", 10) set (
    gamaInputs += nb_preys_init,
    gamaOutputs += number_of_preys 
  )

val exploration = 
  ExplorationTask(
    nb_preys_init in (0 to 200 by 10)
  )

exploration -< (gama hook ToStringHook())
```

