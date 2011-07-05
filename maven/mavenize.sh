set -e

VER=$1

if [ -z "$VER" ] ; then 
  echo "   Usage:  "
  echo "   "
  echo "      svn -co https://sardine.googlecode.com/svn/maven svn-maven"
  echo "      wget http://sardine.googlecode.com/files/sardine-\$version.zip"
  echo "      unzip -q sardine-\$version.zip"
  echo "      mavenize.sh \$version"
  echo "      # Now change the maven-metadata.xml - change <version> and add <versions>/<version>."
  echo "      gedit svn-maven/com/googlecode/sardine/sardine/maven-metadata.xml"
  exit 1
fi

mkdir -p sardine-$VER
cd sardine-$VER

wget http://sardine.googlecode.com/files/sardine-$VER.zip
unzip sardine-$VER.zip -d tmp
cd tmp/sardine-$VER

cd javadoc
zip -r sardine-$VER-javadoc.jar *
mv sardine-$VER-javadoc.jar ../../../
cd ..


cd src
zip -r sardine-$VER-sources.jar *
mv sardine-$VER-sources.jar ../../../
cd ..

mv sardine.jar ../../sardine-$VER.jar

cd ../..

MVN_ART_DIR="svn-maven/com/googlecode/sardine/sardine"
DEST_DIR="$MVN_ART_DIR/$VER"
mkdir -p "$DEST_DIR"
cp *.jar "$DEST_DIR/"
cp "$MVN_ART_DIR/template.pom" "$DEST_DIR/sardine-$VER.pom"
sed "s#@VER@#$VER#" -i 

echo "  DON'T FORGET TO CHECK THE DEPENDENCIES UPDATES!  "
read
