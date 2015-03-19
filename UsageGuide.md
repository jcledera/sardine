

&lt;wiki:gadget url="http://sardine.googlecode.com/svn/wiki/adsense.xml" border="0" width="468" height="60" /&gt;

# Introduction #

Sardine is a next generation WebDAV client for Java. It is intended to be simple to use and does not implement the full WebDAV client specification (although a good amount is covered). Instead, the goal is to provide methods for most use case scenarios when working with a webdav server. The code needs to run as fast as possible and use the latest released Apache [HttpComponents](http://hc.apache.org).

# Dependencies #

You will need [commons-logging.jar](http://commons.apache.org/logging/), [commons-codec.jar](http://commons.apache.org/codec/) and [HttpClient/HttpCore 4.1.1](http://hc.apache.org/) in your classpath. I've included these jars for convenience in the lib directory.

Java 5 does not include JAXB, but you are using Java 6 now, right? =) If not, you will need some version of JAXB 2.1.x and its dependencies (ie:  [jaxb-api-2.1.jar](http://repo1.maven.org/maven2/javax/xml/bind/jaxb-api/2.1/jaxb-api-2.1.jar), [jaxb-impl-2.1.12.jar](http://repo1.maven.org/maven2/com/sun/xml/bind/jaxb-impl/2.1.12/jaxb-impl-2.1.12.jar), [activation-1.1.1.jar](http://repo1.maven.org/maven2/javax/activation/activation/1.1.1/activation-1.1.1.jar) and [stax-api-1.0-2.jar](http://repo1.maven.org/maven2/javax/xml/stream/stax-api/1.0-2/stax-api-1.0-2.jar)
) in your classpath as well.

# Usage #
## Factory ##
To use Sardine, you first call `SardineFactory.begin()` or if you have HTTP authentication enabled on your webdav server: `SardineFactory.begin(username, password)`. This will give you an instance of the Sardine interface. Note: once you `begin()` with a username/password, all method calls on the Sardine interface will use that same credentials.

Alternatively you can also directly instantiate the [SardineImpl](http://sardine.googlecode.com/svn/trunk/javadoc/com/googlecode/sardine/impl/SardineImpl.html) class with the option to pass your own preconfigured [HttpClient](http://hc.apache.org/httpcomponents-client-ga/httpclient/apidocs/org/apache/http/client/HttpClient.html) instance.

## SSL ##
By default, Sardine uses [SSLSocketFactory.getSocketFactory()](http://hc.apache.org/httpcomponents-client/httpclient/xref/org/apache/http/conn/ssl/SSLSocketFactory.html) to support HTTPS requests. For customized SSL use, you can override a [createDefaultSecureSocketFactory()](http://sardine.googlecode.com/svn/trunk/javadoc/com/googlecode/sardine/impl/SardineImpl.html#createDefaultSecureSocketFactory()) or provide your own configured HTTP client instance.

```
String keyStoreFilename = "/tmp/mystore";
File keystoreFile = new File(keyStoreFilename);
FileInputStream fis = new FileInputStream(keystoreFile);		
KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType()); // JKS
keyStore.load(fis, null);		
final SSLSocketFactory socketFactory = new SSLSocketFactory(keyStore);				
	
Sardine sardine = new SardineImpl() {
	@Override
	protected SSLSocketFactory createDefaultSecureSocketFactory() {
		return socketFactory;
	}			
};
```

## Proxy ##
For customized proxy use, you can pass a [ProxySelector](http://download.oracle.com/javase/1.5.0/docs/api/java/net/ProxySelector.html) into the methods on `SardineFactory`.

## Ports ##
If you would like to specify a port number to connect different from port 80 (HTTP) and 443 (HTTPS), just must override [createDefaultSchemeRegistry()](http://sardine.googlecode.com/svn/trunk/javadoc/com/googlecode/sardine/impl/SardineImpl.html#createDefaultSchemeRegistry()) or provide your own configured HTTP client instance by instantiating [SardineImpl](http://sardine.googlecode.com/svn/trunk/javadoc/com/googlecode/sardine/impl/SardineImpl.html#SardineImpl(org.apache.http.impl.client.AbstractHttpClient)) directly.

## Threading ##
Sardine uses the HttpComponents [ThreadSafeClientConnManager](http://hc.apache.org/httpcomponents-client/httpclient/apidocs/org/apache/http/impl/conn/tsccm/ThreadSafeClientConnManager.html). This means that you can safely use an instance of Sardine in a multithreaded environment. However, be aware that the default values for [ThreadSafeClientConnManager](http://hc.apache.org/httpcomponents-client/httpclient/apidocs/org/apache/http/impl/conn/tsccm/ThreadSafeClientConnManager.html) may not support enough concurrency for your environment. Therefore, it is probably safer to just call begin() each time you want to call methods on Sardine. If enough demand warrants it, I'd consider providing a way to tune these values.

## Authentication ##
When passing credentials to the client using `Sardine.setCredentials(String username, String password)`, the authentication scope is configured for `Basic`, `Digest` and `NTLM` authentication to respond to the challenge of the server.

### Preemptive Authentication ###
When enabling preemptive authentication using `Sardine.enablePreemptiveAuthentication(String host)`, sending an authentication header before any challenge received by the server. Only the `Basic` scheme is supported and a authentication failure is thrown when the server requests a different authentication scheme such as `Digest`. To disable the use from subsequent requests, use `Sardine.disablePreemptiveAuthentication`.

## Compression ##
GZIP compression can be enabled using `Sardine.enableCompression()` but is currently broken ([HTTPCLIENT-1075](https://issues.apache.org/jira/browse/HTTPCLIENT-1075)). To disable the use from subsequent requests, use `Sardine.disableCompression`.

## API ##
Below is the list of methods on the Sardine interface and example of how to use them. Each of these methods throw generic I/O exceptions or a [SardineException](http://sardine.googlecode.com/svn/trunk/javadoc/com/googlecode/sardine/SardineException.html) for an unexpected server response code. [SardineException](http://sardine.googlecode.com/svn/trunk/javadoc/com/googlecode/sardine/util/SardineException.html) has both the status code and message of the HTTP error response.

---


## `List<DavResource> list(String url, int depth)` ##

This returns a List of [DavResource](http://sardine.googlecode.com/svn/trunk/javadoc/com/googlecode/sardine/DavResource.html) objects for a directory or a single [DavResource](http://sardine.googlecode.com/svn/trunk/javadoc/com/googlecode/sardine/DavResource.html) for a file on a remote dav server. The URL should be properly encoded and must end with a `"/"` for a directory. The depth is an optional parameter that defaults to 1.

```
Sardine sardine = SardineFactory.begin();
List<DavResource> resources = sardine.list("http://yourdavserver.com/adirectory/");
for (DavResource res : resources)
{
     System.out.println(res); // calls the .toString() method.
}
```

## `InputStream get(String url)` ##

This will get an InputStream reference to a remote file. Obviously you want to point at a file and not a directory for this one. The url should be properly encoded.

```
Sardine sardine = SardineFactory.begin("username", "password");
InputStream is = sardine.get("http://yourdavserver.com/adirectory/afile.jpg");
```

## `void put(String url, byte[] data, [String contentType])` ##

This allows you to HTTP PUT a file up on a webdav server. Most likely you will want
to pass in a username/password for this one unless the server is behind a firewall. =) It is also helpful to use the [commons-io](http://commons.apache.org/io/) library to read the file from disk.

```
Sardine sardine = SardineFactory.begin("username", "password");
byte[] data = FileUtils.readFileToByteArray(new File("/file/on/disk"));
sardine.put("http://yourdavserver.com/adirectory/nameOfFile.jpg", data);
```

Optionally, you may specify a Content-Type header value.

## `void put(String url, InputStream dataStream, [String contentType], [boolean expectContinue])` ##

This allows you to HTTP PUT a file up on a webdav server.
It takes an InputStream so that you don't have to buffer the entire file into
memory first as a byte array.

```
Sardine sardine = SardineFactory.begin();
InputStream fis = new FileInputStream(new File("/some/file/on/disk.txt"));
sardine.put("http://yourdavserver.com/adirectory/nameOfFile.jpg", fis);
```

Optionally, you may specify a Content-Type header value. Optionally, an Expect: Continue header is added to receive a possible error by the server before any data is sent.

## `void delete(String url)` ##

This uses HTTP PUT to delete a resource on a webdav server. Most likely you will want
to pass in a username/password for this one unless the server is behind a firewall. =)

```
Sardine sardine = SardineFactory.begin("username", "password");
sardine.delete("http://yourdavserver.com/adirectory/nameOfFile.jpg");
```

## `void createDirectory(String url)` ##

This creates a directory on the remote server.

```
Sardine sardine = SardineFactory.begin("username", "password");
sardine.createDirectory("http://yourdavserver.com/adirectory/");
```

## `void move(String sourceUrl, String destinationUrl)` ##

This moves a file from one location to another on the remote server. It assumes you want to overwrite all files.

```
Sardine sardine = SardineFactory.begin("username", "password");
sardine.move("http://yourdavserver.com/adirectory/file1.jpg", "http://yourdavserver.com/adirectory/file2.jpg");
```

## `void copy(String sourceUrl, String destinationUrl)` ##

This copies a file from one location to another on the remote server. It assumes you want to overwrite all files.

```
Sardine sardine = SardineFactory.begin("username", "password");
sardine.copy("http://yourdavserver.com/adirectory/file1.jpg", "http://yourdavserver.com/adirectory/file2.jpg");
```

## `boolean exists(String url)` ##

This uses a HTTP HEAD request to see if a file exists on the remote server.

```
Sardine sardine = SardineFactory.begin();
if (sardine.exists("http://yourdavserver.com/adirectory/file1.jpg"))
    System.out.println("got here!");
```

## `void setCustomProps(String url, Map<String,String> addProps, List<String> removeProps)` ##

This uses a HTTP PROPPATCH request to add and/or remove custom properties on a file that exist on a remote server.

```
Sardine sardine = SardineFactory.begin();

String someUrl = "http://yourdavserver.com/adirectory/somefile.pdf";

// 1. Set custom properties on a resource:

Map<String,String> addProps = new HashMap<String,String>(2);
addProps.put("author", "J. Diamond");
addProps.put("title", "The Third Chimpanzee");

sardine.setCustomProps(someURL, addProps, null)

// 2. Retrieve custom properties on a resource:

List<DavResource> resources = sardine.getResources(someURL)

for(DavResource resource : resources) {
    Map<String,String> customProps = resource.getCustomProps();
    // Use custom properties...
    String author = (String) customProps.get("author");
    String title = (String) customProps.get("title");
}

// 3. Update and/or delete custom properties on a resource:

Map<String,String> updateProps = new HashMap<String,String>(1);
updateProps.put("author", "Jared Diamond");

List<String> removeProps = new ArrayList<String>(1);
removeProps.add("title");

sardine.setCustomProps(someURL, updateProps, removeProps)

```


&lt;wiki:gadget url="http://sardine.googlecode.com/svn/wiki/adsense.xml" border="0" width="468" height="60" /&gt;