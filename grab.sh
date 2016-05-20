
mkdir -p target
cd target
rm master.zip
rm -rf gama-master
wget https://github.com/gama-platform/gama/archive/master.zip
tar -xvzf master.zip
cd -

cd target/gama-master/
./build.sh
cd -

GAMA=target/gama-master/msi.gama.application/target/products/msi.gama.application.product/linux/gtk/x86_64/plugins/
LOCAL=.

for f in `cat bundles.list`
do
  ls -d "$GAMA/$f"
  if [[ $? != 0 ]]; then exit 1; fi
done

BUNDLES="$LOCAL/bundles/"
LIBS="$LOCAL/lib"

rm -rf "$BUNDLES" "$LIBS"

mkdir -p "$BUNDLES"
mkdir -p "$LIBS"

for f in `cat bundles.list`
do
  cp -r "$GAMA/$f" "$BUNDLES/"
done

cd "$BUNDLES"

DIRS=`find . -mindepth 1 -maxdepth 1 -type d`

for d in "$DIRS"
do 
  NAME=`ls -d $d`
  if [[ $? != 0 ]]; then exit 1; fi
  jar -cvfM $NAME.jar -C $NAME .
  mv "$NAME/*" "../$LIBS"
  rm -rf "$NAME"
done

cd -
cd "$LIBS"

for f in `ls ../bundles`
do
  ln -s "../bundles/$f"
done

cd -

mv "$BUNDLES/org.apache.commons.logging_*.jar" "$LIBS"
mv "$BUNDLES/org.apache.log4j_*.jar" "$LIBS"
