
for f in `cat bundles.list`
do
  ls -d $1/$f
  if [[ $? != 0 ]]; then exit 1; fi
done

BUNDLES="$2/bundles/"
LIBS="$2/lib"

rm -rf $BUNDLES $LIBS

mkdir -p $BUNDLES
mkdir -p $LIBS

for f in `cat bundles.list`
do
  cp -r $1/$f $BUNDLES/
done

DIRS="msi.gama.ext_* msi.gama.headless_*"

cd $BUNDLES

for d in $DIRS
do 
  NAME=`ls -d $d`
  if [[ $? != 0 ]]; then exit 1; fi
  jar -cvfM $NAME.jar -C $NAME .
  mv $NAME/* ../$LIBS
  rm -rf $NAME
done

cd -
cd $LIBS

for f in `ls ../bundles`
do
  ln -s ../bundles/$f
done

#jar -xvf msi.gama.ext_1.0.0.jar
#rm -rf msi.gama.ext_1.0.0.jar META-INF

cd -

mv $BUNDLES/org.apache.commons.logging_*.jar $LIBS
mv $BUNDLES/org.apache.log4j_*.jar $LIBS
