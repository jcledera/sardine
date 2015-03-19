## Sardine Maven repository ##

To add Sardine as a maven dependency, add the repo to your pom. Note, unless someone volunteers to keep this up to date, don't expect it to have the latest version.

```
<project>
  ...
  <repositories>
     <repository>
        <id>sardine-google-svn-repo</id>
        <snapshots> <enabled>true</enabled> </snapshots>
        <name>Sardine maven repo at Google Code</name>
        <url>http://sardine.googlecode.com/svn/maven/</url>
     </repository>
  </repositories>
  ...
  <dependencies>
    <dependency>
      <groupId>com.googlecode.sardine</groupId>
      <artifactId>sardine</artifactId>
      <version>314</version>
    </dependency>
  </dependencies>

</project>
```

## JDK 5 and JAXB ##

JDK 5 does not include JAXB, so you have to include the deps when using JDK 5:

```
  <profiles>

    <!-- JAXB is not in JDK 5. -->
    <profile>
      <id>jdk5</id>
      <activation> <jdk>1.5</jdk> </activation>

      <dependencies>
        <dependency>
          <groupId>javax.xml.bind</groupId>
          <artifactId>jaxb-api</artifactId>
          <version>2.1</version>
          <classifier>jdk5</classifier>
        </dependency>
        <dependency>
          <groupId>com.sun.xml.bind</groupId>
          <artifactId>jaxb-impl</artifactId>
          <version>2.1.12</version>
          <classifier>jdk5</classifier>
        </dependency>
      </dependencies>

    </profile>
    
  </profiles>
```