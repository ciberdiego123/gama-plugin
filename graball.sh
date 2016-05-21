
GAMA=target/gama-master/msi.gama.application/target/products/msi.gama.application.product/linux/gtk/x86_64/plugins/
LOCAL=.

BUNDLES=$LOCAL/bundles/
LIBS=$LOCAL/lib

rm -rf "$BUNDLES" "$LIBS"

mkdir -p $BUNDLES
mkdir -p $LIBS

for f in `ls $GAMA`
do
  cp -r $GAMA/$f $BUNDLES/
done

for f in `cat bundles.blacklist`
do
#  rm $BUNDLES/$f 
done


cd $BUNDLES

DIRS=`find . -mindepth 1 -maxdepth 1 -type d`

for d in $DIRS
do 
  NAME=`ls -d $d`
  if [[ $? != 0 ]]; then exit 1; fi
  jar -cvfM $NAME.jar -C $NAME .
  cp $NAME/* ../$LIBS
  rm "$NAME.jar"
done

cd -
cd $LIBS

for f in `ls ../bundles`
do
  ln -s ../bundles/$f
done

rm *.so

cd -

mv $BUNDLES/org.apache.commons.logging_*.jar $LIBS
mv $BUNDLES/org.apache.log4j_*.jar $LIBS
