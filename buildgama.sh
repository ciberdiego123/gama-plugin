
mkdir -p target
cd target
rm master.zip
rm -rf gama-master
wget https://github.com/gama-platform/gama/archive/master.zip
unzip master.zip
cd -

cd target/gama-master/
./build.sh
cd -


