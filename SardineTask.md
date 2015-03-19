&lt;wiki:gadget url="http://sardine.googlecode.com/svn/wiki/adsense.xml" border="0" width="468" height="60" /&gt;

# Introduction #

It is nice to be able to do webdav from Ant for various reasons. So, provide a wrapper around Sardine that enables this.

# Details #

Right now, Sardine features such as complicated https configuration and proxies are not supported by this task. If you would like to see this, then I welcome contributions. I'm also only supporting basic auth for the put() tasks right now.

Nothing speaks louder to me than a good example. Basically, you just define the typedef, pass in the location of the sardine.jar, its dependencies and then call the task definitions. Username/password is optional. The exists element will set the named property to "true" on success. Put takes a fileset block and/or a file.

The put() element also takes a contentType attribute.

The sardine element also takes a port number.

```
<project name="sardineTask" basedir=".">

	<target name="init">
		<typedef resource="com/googlecode/sardine/ant/sardinetask.xml">
			<classpath>
				<fileset dir="./target">
					<include name="sardine.jar"/>
				</fileset>
				<fileset dir="./lib">
					<include name="*.jar"/>
				</fileset>
			</classpath>
		</typedef>
	</target>

	<target name="delete" depends="init">
		<sardine username="" password="" port="">
			<delete url="" />
		</sardine>
	</target>

	<target name="move" depends="init">
		<sardine username="" password="">
			<move srcUrl="" dstUrl="" />
		</sardine>
	</target>

	<target name="copy" depends="init">
		<sardine username="" password="">
			<copy srcUrl="" dstUrl="" />
		</sardine>
	</target>

	<target name="createDirectory" depends="init">
		<sardine username="" password="">
			<createDirectory url="" />
		</sardine>
	</target>

	<target name="exists" depends="init">
		<sardine username="" password="">
			<exists url="" property="sardine" />
		</sardine>
	</target>
	
	<target name="put1" depends="init">
		<sardine username="jon" password="stevens">
			<put url="http://www.foo.com/">
				<fileset dir="${somedir}">
					<include name="*" />
				</fileset>
			</put>
		</sardine>
	</target>

	<target name="put2" depends="init">
		<sardine username="jon" password="stevens">
			<put url="http://www.foo.com/" file="/tmp/foo.txt" contentType="text/plain" />
		</sardine>
	</target>
</project>
```