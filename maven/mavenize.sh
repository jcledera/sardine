set -e

VER=$1

if [ -z "$VER" ] ; then 
  echo "   Usage:  "
  echo "   "
  echo "      svn -co https://sardine.googlecode.com/svn/maven svn-maven"
  echo "      #wget http://sardine.googlecode.com/files/sardine-\$version.zip"
  echo "      #unzip -q sardine-\$version.zip"
  echo "      mavenize.sh \$version"
  echo "      # Now change the maven-metadata.xml - change <version> and add <versions>/<version>."
  echo "      gedit svn-maven/com/googlecode/sardine/sardine/maven-metadata.xml"
  exit 1
fi

if [ ! -d "com" ] ; then
  echo "Not executed in svn-maven/ dir - pls go there first. CWD: `pwd`"; exit 2;
fi

pushd .
   mkdir sardine-$VER~
   cd sardine-$VER~
      WORKDIR=`pwd`

      wget http://sardine.googlecode.com/files/sardine-$VER.zip
      unzip -q sardine-$VER.zip -d tmp
      cd tmp/sardine-$VER

      # Javadoc
      cd javadoc
         zip -q -r sardine-$VER-javadoc.jar *
         mv sardine-$VER-javadoc.jar $WORKDIR
      cd ..

      # Sources
      cd src
         zip -q -r sardine-$VER-sources.jar *
         mv sardine-$VER-sources.jar $WORKDIR
      cd ..

      # Binary
      mv sardine.jar $WORKDIR/sardine-$VER.jar

   cd $WORKDIR/..

   MVN_ART_DIR="com/googlecode/sardine/sardine"
   DEST_DIR="$MVN_ART_DIR/$VER"
   mkdir -p "$DEST_DIR"
   cp $WORKDIR/*.jar "$DEST_DIR/"
   cp "$MVN_ART_DIR/template.pom" "$DEST_DIR/sardine-$VER.pom"
   sed "s#@VER@#$VER#" -i "$DEST_DIR/sardine-$VER.pom"

   echo "  DON'T FORGET TO CHECK THE DEPENDENCIES UPDATES!  "

popd
rm -rf sardine-$VER~
read
