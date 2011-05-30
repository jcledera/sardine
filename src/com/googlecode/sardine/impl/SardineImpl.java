/*
 * Copyright 2009-2011 Jon Stevens et al.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.googlecode.sardine.impl;

import com.googlecode.sardine.DavResource;
import com.googlecode.sardine.Sardine;
import com.googlecode.sardine.Version;
import com.googlecode.sardine.impl.handler.ExistsResponseHandler;
import com.googlecode.sardine.impl.handler.LockResponseHandler;
import com.googlecode.sardine.impl.handler.MultiStatusResponseHandler;
import com.googlecode.sardine.impl.handler.VoidResponseHandler;
import com.googlecode.sardine.impl.io.ConsumingInputStream;
import com.googlecode.sardine.impl.methods.HttpCopy;
import com.googlecode.sardine.impl.methods.HttpLock;
import com.googlecode.sardine.impl.methods.HttpMkCol;
import com.googlecode.sardine.impl.methods.HttpMove;
import com.googlecode.sardine.impl.methods.HttpPropFind;
import com.googlecode.sardine.impl.methods.HttpPropPatch;
import com.googlecode.sardine.impl.methods.HttpUnlock;
import com.googlecode.sardine.model.Allprop;
import com.googlecode.sardine.model.Exclusive;
import com.googlecode.sardine.model.Lockinfo;
import com.googlecode.sardine.model.Lockscope;
import com.googlecode.sardine.model.Locktype;
import com.googlecode.sardine.model.Multistatus;
import com.googlecode.sardine.model.Prop;
import com.googlecode.sardine.model.Propertyupdate;
import com.googlecode.sardine.model.Propfind;
import com.googlecode.sardine.model.Remove;
import com.googlecode.sardine.model.Response;
import com.googlecode.sardine.model.Set;
import com.googlecode.sardine.model.Write;
import com.googlecode.sardine.util.SardineUtil;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.ProtocolException;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.NTCredentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.params.AuthPolicy;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.client.protocol.RequestAcceptEncoding;
import org.apache.http.client.protocol.ResponseContentEncoding;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.routing.HttpRoutePlanner;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.impl.conn.ProxySelectorRoutePlanner;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.namespace.QName;
import java.io.IOException;
import java.io.InputStream;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of the Sardine interface. This is where the meat of the Sardine library lives.
 *
 * @author jonstevens
 * @version $Id$
 */
public class SardineImpl implements Sardine
{
	private static final String UTF_8 = "UTF-8";

	/**
	 * HTTP Implementation
	 */
	private AbstractHttpClient client;

	/**
	 * Local context with authentication cache. Make sure the same context is used to execute
	 * logically related requests.
	 */
	private HttpContext context = new BasicHttpContext();

	/**
	 * Access resources with no authentication
	 */
	public SardineImpl()
	{
		this(null, null);
	}

	/**
	 * Supports standard authentication mechanisms
	 *
	 * @param username Use in authentication header credentials
	 * @param password Use in authentication header credentials
	 */
	public SardineImpl(String username, String password)
	{
		this(username, password, null);
	}

	/**
	 * @param username Use in authentication header credentials
	 * @param password Use in authentication header credentials
	 * @param selector Proxy configuration
	 */
	public SardineImpl(String username, String password, ProxySelector selector)
	{
		this.init(this.createDefaultClient(selector), username, password);
	}

	/**
	 * @param http Custom client configuration
	 */
	public SardineImpl(AbstractHttpClient http)
	{
		this(http, null, null);
	}

	/**
	 * @param http	 Custom client configuration
	 * @param username Use in authentication header credentials
	 * @param password Use in authentication header credentials
	 */
	public SardineImpl(AbstractHttpClient http, String username, String password)
	{
		this.init(http, username, password);
	}

	private void init(AbstractHttpClient http, String username, String password)
	{
		this.client = http;
		this.client.setRedirectStrategy(new DefaultRedirectStrategy()
		{
			@Override
			public boolean isRedirected(HttpRequest request, HttpResponse response, HttpContext context) throws ProtocolException
			{
				int statusCode = response.getStatusLine().getStatusCode();
				String method = request.getRequestLine().getMethod();
				Header locationHeader = response.getFirstHeader("location");
				switch (statusCode)
				{
					case HttpStatus.SC_MOVED_TEMPORARILY:
						return (method.equalsIgnoreCase(HttpGet.METHOD_NAME)
								|| method.equalsIgnoreCase(HttpHead.METHOD_NAME)
								|| method.equalsIgnoreCase(HttpPropFind.METHOD_NAME)) && locationHeader != null;
					case HttpStatus.SC_MOVED_PERMANENTLY:
					case HttpStatus.SC_TEMPORARY_REDIRECT:
						return method.equalsIgnoreCase(HttpGet.METHOD_NAME)
								|| method.equalsIgnoreCase(HttpHead.METHOD_NAME)
								|| method.equalsIgnoreCase(HttpPropFind.METHOD_NAME);
					case HttpStatus.SC_SEE_OTHER:
						return true;
					default:
						return false;
				} //end of switch
			}

			public HttpUriRequest getRedirect(HttpRequest request, HttpResponse response, HttpContext context)
					throws ProtocolException
			{
				URI uri = getLocationURI(request, response, context);
				String method = request.getRequestLine().getMethod();
				if (method.equalsIgnoreCase(HttpHead.METHOD_NAME))
				{
					return new HttpHead(uri);
				}
				if (method.equalsIgnoreCase(HttpPropFind.METHOD_NAME))
				{
					return new HttpPropFind(uri);
				}
				if (method.equalsIgnoreCase(HttpLock.METHOD_NAME))
				{
					return new HttpLock(uri);
				}
				return new HttpGet(uri);
			}
		});
		this.setCredentials(username, password);
	}

	/**
	 * Add credentials to any scope. Supports Basic, Digest and NTLM authentication methods.
	 *
	 * @param username Use in authentication header credentials
	 * @param password Use in authentication header credentials
	 */
	public void setCredentials(String username, String password)
	{
		this.setCredentials(username, password, "", "");
	}

	/**
	 * @param username	Use in authentication header credentials
	 * @param password	Use in authentication header credentials
	 * @param domain	  NTLM authentication
	 * @param workstation NTLM authentication
	 */
	public void setCredentials(String username, String password, String domain, String workstation)
	{
		if (username != null)
		{
			this.client.getCredentialsProvider().setCredentials(
					new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT, AuthScope.ANY_REALM, AuthPolicy.NTLM),
					new NTCredentials(username, password, workstation, domain));
			this.client.getCredentialsProvider().setCredentials(
					new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT, AuthScope.ANY_REALM, AuthPolicy.BASIC),
					new UsernamePasswordCredentials(username, password));
			this.client.getCredentialsProvider().setCredentials(
					new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT, AuthScope.ANY_REALM, AuthPolicy.DIGEST),
					new UsernamePasswordCredentials(username, password));
		}
	}

	/**
	 * Adds handling of GZIP compression to the client.
	 */
	public void enableCompression()
	{
		this.client.addRequestInterceptor(new RequestAcceptEncoding());
		this.client.addResponseInterceptor(new ResponseContentEncoding());
	}

	/**
	 * Disable GZIP compression header.
	 */
	public void disableCompression()
	{
		this.client.removeRequestInterceptorByClass(RequestAcceptEncoding.class);
		this.client.removeResponseInterceptorByClass(ResponseContentEncoding.class);
	}

	/*
	 * (non-Javadoc)
	 * @see com.googlecode.sardine.Sardine#enablePreemptiveAuthentication(java.lang.String, java.lang.String, int)
	 */
	public void enablePreemptiveAuthentication(String hostname)
	{
		AuthCache authCache = new BasicAuthCache();
		// Generate Basic preemptive scheme object and stick it to the local execution context
		BasicScheme basicAuth = new BasicScheme();
		SchemeRegistry registry = this.client.getConnectionManager().getSchemeRegistry();
		// Configure HttpClient to authenticate preemptively by prepopulating the authentication data cache.
		for (String scheme : registry.getSchemeNames())
		{
			int port = registry.getScheme(scheme).getDefaultPort();
			authCache.put(new HttpHost(hostname), basicAuth);
			authCache.put(new HttpHost(hostname, -1, scheme), basicAuth);
			authCache.put(new HttpHost(hostname, port, scheme), basicAuth);
		}
		// Add AuthCache to the execution context
		this.context.setAttribute(ClientContext.AUTH_CACHE, authCache);
	}

	/*
	 * (non-Javadoc)
	 * @see com.googlecode.sardine.Sardine#disablePreemptiveAuthentication(java.lang.String, java.lang.String, int)
	 */
	public void disablePreemptiveAuthentication()
	{
		this.context.removeAttribute(ClientContext.AUTH_CACHE);
	}

	/*
	 * (non-Javadoc)
	 * @see com.googlecode.sardine.Sardine#getResources(java.lang.String)
	 */
	public List<DavResource> getResources(String url) throws IOException
	{
		HttpPropFind entity = new HttpPropFind(url);
		Propfind body = new Propfind();
		body.setAllprop(new Allprop());
		entity.setEntity(new StringEntity(SardineUtil.toXml(body), UTF_8));
		Multistatus multistatus = execute(entity, new MultiStatusResponseHandler());
		List<Response> responses = multistatus.getResponse();
		List<DavResource> resources = new ArrayList<DavResource>(responses.size());
		for (Response resp : responses)
		{
			try
			{
				resources.add(new DavResource(resp));
			}
			catch (URISyntaxException e)
			{
				// Ignore resource with invalid URI
			}
		}
		return resources;
	}

	/**
	 * Creates a {@link Propertyupdate} element containing all properties to set from setProps and all properties to
	 * remove from removeProps. Note this method will use a {@link SardineUtil#DEFAULT_NAMESPACE_URI} as
	 * namespace and {@link SardineUtil#DEFAULT_NAMESPACE_PREFIX} as prefix.
	 *
	 * @see com.googlecode.sardine.Sardine#setCustomProps(String, java.util.Map, java.util.List)
	 */
	public void setCustomProps(String url, Map<String, String> setProps, List<String> removeProps) throws IOException
	{
		HttpPropPatch entity = new HttpPropPatch(url);
		final Document document = SardineUtil.createDocument();
		// Build WebDAV <code>PROPPATCH</code> entity.
		final Propertyupdate body = new Propertyupdate();
		// Add properties
		{
			final Set set = new Set();
			body.getRemoveOrSet().add(set);
			final Prop prop = new Prop();
			// Returns a reference to the live list
			final List<Element> any = prop.getAny();
			for (Map.Entry<QName, String> entry : SardineUtil.toQName(setProps).entrySet())
			{
				final Element element = SardineUtil.createElement(document, entry.getKey());
				element.setTextContent(entry.getValue());
				any.add(element);
			}
			set.setProp(prop);
		}
		// Remove properties
		{
			final Remove remove = new Remove();
			body.getRemoveOrSet().add(remove);
			final Prop prop = new Prop();
			// Returns a reference to the live list
			final List<Element> any = prop.getAny();
			for (QName entry : SardineUtil.toQName(removeProps))
			{
				final Element element = SardineUtil.createElement(document, entry);
				any.add(element);
			}
			remove.setProp(prop);
		}
		entity.setEntity(new StringEntity(SardineUtil.toXml(body), UTF_8));
		this.execute(entity, new VoidResponseHandler());
	}

	/*
	 * (non-Javadoc)
	 * @see com.googlecode.sardine.Sardine#lock(java.lang.String)
	 */
	public String lock(String url) throws IOException
	{
		HttpLock entity = new HttpLock(url);
		Lockinfo body = new Lockinfo();
		Lockscope scopeType = new Lockscope();
		scopeType.setExclusive(new Exclusive());
		body.setLockscope(scopeType);
		final Locktype lockType = new Locktype();
		lockType.setWrite(new Write());
		body.setLocktype(lockType);
		entity.setEntity(new StringEntity(SardineUtil.toXml(body), UTF_8));
		// Return the lock token
		return this.execute(entity, new LockResponseHandler());
	}

	/*
	 * (non-Javadoc)
	 * @see com.googlecode.sardine.Sardine#unlock(java.lang.String, java.lang.String)
	 */
	public void unlock(String url, String token) throws IOException
	{
		HttpUnlock entity = new HttpUnlock(url, token);
		Lockinfo body = new Lockinfo();
		Lockscope scopeType = new Lockscope();
		scopeType.setExclusive(new Exclusive());
		body.setLockscope(scopeType);
		final Locktype lockType = new Locktype();
		lockType.setWrite(new Write());
		body.setLocktype(lockType);
		this.execute(entity, new VoidResponseHandler());
	}

	/*
	 * (non-Javadoc)
	 * @see com.googlecode.sardine.Sardine#get(java.lang.String)
	 */
	public InputStream get(String url) throws IOException
	{
		return get(url, Collections.<String, String>emptyMap());
	}

	/*
	 * (non-Javadoc)
	 * @see com.googlecode.sardine.Sardine#get(java.lang.String, java.util.Map)
	 */
	public InputStream get(String url, Map<String, String> headers) throws IOException
	{
		HttpGet get = new HttpGet(url);
		for (String header : headers.keySet())
		{
			get.addHeader(header, headers.get(header));
		}
		// Must use #execute without handler, otherwise the entity is consumed
		// already after the handler exits.
		HttpResponse response = execute(get);
		VoidResponseHandler handler = new VoidResponseHandler();
		try
		{
			handler.handleResponse(response);
			// Will consume the entity when the stream is closed.
			return new ConsumingInputStream(response);
		}
		catch (IOException ex)
		{
			get.abort();
			throw ex;
		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.googlecode.sardine.Sardine#put(java.lang.String, byte[])
	 */
	public void put(String url, byte[] data) throws IOException
	{
		put(url, data, null);
	}

	/*
	 * (non-Javadoc)
	 * @see com.googlecode.sardine.Sardine#put(java.lang.String, byte[], java.lang.String)
	 */
	public void put(String url, byte[] data, String contentType) throws IOException
	{
		ByteArrayEntity entity = new ByteArrayEntity(data);
		put(url, entity, contentType, true);
	}

	/*
	 * (non-Javadoc)
	 * @see com.googlecode.sardine.Sardine#put(java.lang.String, java.io.InputStream)
	 */
	public void put(String url, InputStream dataStream) throws IOException
	{
		put(url, dataStream, (String) null);
	}

	/*
	 * (non-Javadoc)
	 * @see com.googlecode.sardine.Sardine#put(java.lang.String, java.io.InputStream, java.lang.String)
	 */
	public void put(String url, InputStream dataStream, String contentType) throws IOException
	{
		put(url, dataStream, contentType, true);
	}

	/*
	 * (non-Javadoc)
	 * @see com.googlecode.sardine.Sardine#put(java.lang.String, java.io.InputStream, java.lang.String, boolean)
	 */
	public void put(String url, InputStream dataStream, String contentType, boolean expectContinue) throws IOException
	{
		// A length of -1 means "go until end of stream"
		InputStreamEntity entity = new InputStreamEntity(dataStream, -1);
		put(url, entity, contentType, expectContinue);
	}

	/*
	 * (non-Javadoc)
	 * @see com.googlecode.sardine.Sardine#put(java.lang.String, java.io.InputStream, java.util.Map)
	 */
	public void put(String url, InputStream dataStream, Map<String, String> headers) throws IOException
	{
		// A length of -1 means "go until end of stream"
		InputStreamEntity entity = new InputStreamEntity(dataStream, -1);
		put(url, entity, headers);
	}

	/**
	 * Private helper for doing the work of a put
	 *
	 * @param url			Resource
	 * @param entity		 The entity to read from
	 * @param contentType	Content Type header
	 * @param expectContinue Add <code>Expect: continue</code> header
	 */
	public void put(String url, HttpEntity entity, String contentType, boolean expectContinue) throws IOException
	{
		Map<String, String> headers = new HashMap<String, String>();
		if (contentType == null)
		{
			headers.put(HttpHeaders.CONTENT_TYPE, HTTP.DEFAULT_CONTENT_TYPE);
		}
		else
		{
			headers.put(HttpHeaders.CONTENT_TYPE, contentType);
		}
		if (expectContinue)
		{
			headers.put(HTTP.EXPECT_DIRECTIVE, HTTP.EXPECT_CONTINUE);
		}
		this.put(url, entity, headers);
	}

	/**
	 * @param url	 Resource
	 * @param entity  The entity to read from
	 * @param headers Headers to add to request
	 */
	public void put(String url, HttpEntity entity, Map<String, String> headers) throws IOException
	{
		HttpPut put = new HttpPut(url);
		put.setEntity(entity);
		for (String header : headers.keySet())
		{
			put.addHeader(header, headers.get(header));
		}
		execute(put, new VoidResponseHandler());
	}

	/**
	 * (non-Javadoc)
	 *
	 * @see com.googlecode.sardine.Sardine#delete(java.lang.String)
	 */
	public void delete(String url) throws IOException
	{
		HttpDelete delete = new HttpDelete(url);
		execute(delete, new VoidResponseHandler());
	}

	/**
	 * (non-Javadoc)
	 *
	 * @see com.googlecode.sardine.Sardine#move(java.lang.String, java.lang.String)
	 */
	public void move(String sourceUrl, String destinationUrl) throws IOException
	{
		HttpMove move = new HttpMove(sourceUrl, destinationUrl);
		execute(move, new VoidResponseHandler());
	}

	/**
	 * (non-Javadoc)
	 *
	 * @see com.googlecode.sardine.Sardine#copy(java.lang.String, java.lang.String)
	 */
	public void copy(String sourceUrl, String destinationUrl) throws IOException
	{
		HttpCopy copy = new HttpCopy(sourceUrl, destinationUrl);
		execute(copy, new VoidResponseHandler());
	}

	/**
	 * (non-Javadoc)
	 *
	 * @see com.googlecode.sardine.Sardine#createDirectory(java.lang.String)
	 */
	public void createDirectory(String url) throws IOException
	{
		HttpMkCol mkcol = new HttpMkCol(url);
		execute(mkcol, new VoidResponseHandler());
	}

	/**
	 * (non-Javadoc)
	 *
	 * @see com.googlecode.sardine.Sardine#exists(java.lang.String)
	 */
	public boolean exists(String url) throws IOException
	{
		HttpHead head = new HttpHead(url);
		return execute(head, new ExistsResponseHandler());
	}

	/**
	 * Wraps all checked exceptions to {@link IOException}. Validate the response using the
	 * response handler. Aborts the request if there is an exception.
	 *
	 * @param <T>             Return type
	 * @param request		 Request to execute
	 * @param responseHandler Determines the return type.
	 * @return parsed response
	 */
	private <T> T execute(HttpRequestBase request, ResponseHandler<T> responseHandler)
			throws IOException
	{
		try
		{
			return this.client.execute(request, responseHandler, this.context);
		}
		catch (IOException e)
		{
			request.abort();
			throw e;
		}
	}

	/**
	 * No validation of the response. Wraps all checked exceptions to {@link IOException}.
	 * Aborts the request if there is an exception.
	 *
	 * @param request Request to execute
	 * @return The response to check the reply status code
	 */
	private HttpResponse execute(HttpRequestBase request)
			throws IOException
	{
		try
		{
			return this.client.execute(request, this.context);
		}
		catch (IOException e)
		{
			request.abort();
			throw e;
		}
	}

	/**
	 * Creates an AbstractHttpClient with all of the defaults.
	 */
	protected AbstractHttpClient createDefaultClient(ProxySelector selector)
	{
		SchemeRegistry schemeRegistry = createDefaultSchemeRegistry();
		ClientConnectionManager cm = createDefaultConnectionManager(schemeRegistry);
		HttpParams params = createDefaultHttpParams();
		AbstractHttpClient client = new DefaultHttpClient(cm, params);
		client.setRoutePlanner(createDefaultRoutePlanner(schemeRegistry, selector));
		return client;
	}

	/**
	 * Creates default params setting the user agent.
	 *
	 * @return Basic HTTP parameters with a custom user agent
	 */
	protected HttpParams createDefaultHttpParams()
	{
		HttpParams params = new BasicHttpParams();
		HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
		HttpProtocolParams.setUserAgent(params, "Sardine/" + Version.getSpecification());
		// Only selectively enable this for PUT but not all entity enclosing methods
		HttpProtocolParams.setUseExpectContinue(params, false);
		HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
		HttpProtocolParams.setContentCharset(params, HTTP.DEFAULT_CONTENT_CHARSET);

		HttpConnectionParams.setTcpNoDelay(params, true);
		HttpConnectionParams.setSocketBufferSize(params, 8192);
		return params;
	}

	/**
	 * Creates a new {@link org.apache.http.conn.scheme.SchemeRegistry} for default ports
	 * with socket factories.
	 *
	 * @return a new {@link org.apache.http.conn.scheme.SchemeRegistry}.
	 */
	protected SchemeRegistry createDefaultSchemeRegistry()
	{
		SchemeRegistry registry = new SchemeRegistry();
		registry.register(new Scheme("http", 80, createDefaultSocketFactory()));
		registry.register(new Scheme("https", 443, createDefaultSecureSocketFactory()));
		return registry;
	}

	/**
	 * @return Default socket factory
	 */
	protected PlainSocketFactory createDefaultSocketFactory()
	{
		return PlainSocketFactory.getSocketFactory();
	}

	/**
	 * @return Default SSL socket factory
	 */
	protected SSLSocketFactory createDefaultSecureSocketFactory()
	{
		return SSLSocketFactory.getSocketFactory();
	}

	/**
	 * Use fail fast connection manager when connections are not released properly.
	 *
	 * @param schemeRegistry Protocol registry
	 * @return Default connection manager
	 */
	protected ClientConnectionManager createDefaultConnectionManager(SchemeRegistry schemeRegistry)
	{
		return new ThreadSafeClientConnManager(schemeRegistry);
	}

	/**
	 * Override to provide proxy configuration
	 *
	 * @param schemeRegistry Protocol registry
	 * @param selector	   Proxy configuration
	 * @return ProxySelectorRoutePlanner configured with schemeRegistry and selector
	 */
	protected HttpRoutePlanner createDefaultRoutePlanner(SchemeRegistry schemeRegistry, ProxySelector selector)
	{
		return new ProxySelectorRoutePlanner(schemeRegistry, selector);
	}
}