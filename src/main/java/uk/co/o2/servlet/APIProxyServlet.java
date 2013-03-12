package uk.co.o2.servlet;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.BitSet;
import java.util.Enumeration;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.AbortableHttpRequest;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.message.HeaderGroup;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;

public class APIProxyServlet extends HttpServlet {

	/**
	 * Serial version uid
	 */
	private static final long serialVersionUID = -6139474403195035373L;

	/**
	 * A boolean parameter then when enabled will log input and target URLs to
	 * the servlet log.
	 */
	public static final String P_LOG = "log";

	/* MISC */

	private static Map<String, String> targetURIMap = new HashMap<String, String>();
	protected boolean doLog = true;
	protected URI targetUri;
	protected HttpClient proxyClient;

	@Override
	// Intentionally maintaining the original authors name, as a mark of respect
	// for his work. - JK
	public String getServletInfo() {
		return "A proxy servlet by David Smiley, dsmiley@mitre.org, and tweaked 5% by JK to cater to our requirements";
	}

	@Override
	public void init(ServletConfig servletConfig) throws ServletException {
		super.init(servletConfig);
		try {
			loadTargetURI(servletConfig);
		} catch (Exception e) {
			throw new RuntimeException(
					"Trying to process targetUri init parameter: " + e, e);
		}

		HttpParams hcParams = new BasicHttpParams();
		readConfigParam(hcParams, ClientPNames.HANDLE_REDIRECTS, Boolean.class);
		proxyClient = createHttpClient(hcParams);
	}

	/**
	 * Loads init config parameters
	 * @param config : The servletconfig
	 */
	private static void loadTargetURI(ServletConfig config) {
		Enumeration<String> configEnum = config.getInitParameterNames();
		String key = null;
		String value = null;
		while (configEnum.hasMoreElements()) {
			key = configEnum.nextElement();
			value = config.getInitParameter(key);
			targetURIMap.put(key, value);
		}
	}

	/**
	 * Called from {@link #init(javax.servlet.ServletConfig)}. HttpClient offers
	 * many opportunities for customization.
	 * 
	 * @param hcParams
	 */
	protected HttpClient createHttpClient(HttpParams hcParams) {
		return new DefaultHttpClient(new ThreadSafeClientConnManager(),
				hcParams);
	}

	private void readConfigParam(HttpParams hcParams, String hcParamName,
			Class type) {
		String val_str = getServletConfig().getInitParameter(hcParamName);
		if (val_str == null)
			return;
		Object val_obj;
		if (type == String.class) {
			val_obj = val_str;
		} else {
			try {
				val_obj = type.getMethod("valueOf", String.class).invoke(type,
						val_str);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		hcParams.setParameter(hcParamName, val_obj);
	}

	@Override
	public void destroy() {
		// shutdown() must be called according to documentation.
		if (proxyClient != null)
			proxyClient.getConnectionManager().shutdown();
		super.destroy();
	}

	@Override
	protected void service(HttpServletRequest servletRequest,
			HttpServletResponse servletResponse) throws ServletException,
			IOException {
		// Make the Request
		// note: we won't transfer the protocol version because I'm not sure it
		// would truly be compatible
		String method = servletRequest.getMethod();
		String proxyRequestUri = rewriteUrlFromRequest(servletRequest);
		HttpRequest proxyRequest;
		System.out.println("Inside API Proxy servlet");
		// spec: RFC 2616, sec 4.3: either these two headers signal that there
		// is a message body.
		if (servletRequest.getHeader(HttpHeaders.CONTENT_LENGTH) != null
				|| servletRequest.getHeader(HttpHeaders.TRANSFER_ENCODING) != null) {
			HttpEntityEnclosingRequest eProxyRequest = new BasicHttpEntityEnclosingRequest(
					method, proxyRequestUri);
			// Add the input entity (streamed)
			// note: we don't bother ensuring we close the servletInputStream
			// since the container handles it
			eProxyRequest.setEntity(new InputStreamEntity(servletRequest
					.getInputStream(), servletRequest.getContentLength()));
			proxyRequest = eProxyRequest;
		} else
			proxyRequest = new BasicHttpRequest(method, proxyRequestUri);

		copyRequestHeaders(servletRequest, proxyRequest);

		try {
			// Execute the request
			if (doLog) {
				log("proxy " + method + " uri: "
						+ servletRequest.getRequestURI() + " -- "
						+ proxyRequest.getRequestLine().getUri());
			}
			HttpResponse proxyResponse = proxyClient.execute(
					URIUtils.extractHost(targetUri), proxyRequest);

			// Process the response
			int statusCode = proxyResponse.getStatusLine().getStatusCode();

			if (doResponseRedirectOrNotModifiedLogic(servletRequest,
					servletResponse, proxyResponse, statusCode)) {
				// just to be sure, but is probably a no-op
				EntityUtils.consume(proxyResponse.getEntity());
				return;
			}

			// Pass the response code. This method with the "reason phrase" is
			// deprecated but it's the only way to pass the
			// reason along too.
			// noinspection deprecation
			servletResponse.setStatus(statusCode, proxyResponse.getStatusLine()
					.getReasonPhrase());

			copyResponseHeaders(proxyResponse, servletResponse);

			// Send the content to the client
			copyResponseEntity(proxyResponse, servletResponse);

		} catch (Exception e) {
			// abort request, according to best practice with HttpClient
			if (proxyRequest instanceof AbortableHttpRequest) {
				AbortableHttpRequest abortableHttpRequest = (AbortableHttpRequest) proxyRequest;
				abortableHttpRequest.abort();
			}
			if (e instanceof RuntimeException)
				throw (RuntimeException) e;
			if (e instanceof ServletException)
				throw (ServletException) e;
			if (e instanceof IOException)
				throw (IOException) e;
			throw new RuntimeException(e);
		}
	}

	private boolean doResponseRedirectOrNotModifiedLogic(
			HttpServletRequest servletRequest,
			HttpServletResponse servletResponse, HttpResponse proxyResponse,
			int statusCode) throws ServletException, IOException {
		// Check if the proxy response is a redirect
		// The following code is adapted from
		// org.tigris.noodle.filters.CheckForRedirect
		if (statusCode >= HttpServletResponse.SC_MULTIPLE_CHOICES /* 300 */
				&& statusCode < HttpServletResponse.SC_NOT_MODIFIED /* 304 */) {
			Header locationHeader = proxyResponse
					.getLastHeader(HttpHeaders.LOCATION);
			if (locationHeader == null) {
				throw new ServletException("Received status code: "
						+ statusCode + " but no " + HttpHeaders.LOCATION
						+ " header was found in the response");
			}
			// Modify the redirect to go to this proxy servlet rather that the
			// proxied host
			String locStr = rewriteUrlFromResponse(servletRequest,
					locationHeader.getValue());

			servletResponse.sendRedirect(locStr);
			return true;
		}
		// 304 needs special handling. See:
		// http://www.ics.uci.edu/pub/ietf/http/rfc1945.html#Code304
		// We get a 304 whenever passed an 'If-Modified-Since'
		// header and the data on disk has not changed; server
		// responds w/ a 304 saying I'm not going to send the
		// body because the file has not changed.
		if (statusCode == HttpServletResponse.SC_NOT_MODIFIED) {
			servletResponse.setIntHeader(HttpHeaders.CONTENT_LENGTH, 0);
			servletResponse.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
			return true;
		}
		return false;
	}

	protected void closeQuietly(Closeable closeable) {
		try {
			closeable.close();
		} catch (IOException e) {
			log(e.getMessage(), e);
		}
	}

	/**
	 * These are the "hop-by-hop" headers that should not be copied.
	 * http://www.w3.org/Protocols/rfc2616/rfc2616-sec13.html I use an
	 * HttpClient HeaderGroup class instead of Set<String> because this approach
	 * does case insensitive lookup faster.
	 */
	private static final HeaderGroup hopByHopHeaders;
	static {
		hopByHopHeaders = new HeaderGroup();
		String[] headers = new String[] { "Connection", "Keep-Alive",
				"Proxy-Authenticate", "Proxy-Authorization", "TE", "Trailers",
				"Transfer-Encoding", "Upgrade" };
		for (String header : headers) {
			hopByHopHeaders.addHeader(new BasicHeader(header, null));
		}
	}

	/** Copy request headers from the servlet client to the proxy request. */
	protected void copyRequestHeaders(HttpServletRequest servletRequest,
			HttpRequest proxyRequest) {
		// Get an Enumeration of all of the header names sent by the client
		Enumeration enumerationOfHeaderNames = servletRequest.getHeaderNames();
		while (enumerationOfHeaderNames.hasMoreElements()) {
			String headerName = (String) enumerationOfHeaderNames.nextElement();
			// Instead the content-length is effectively set via
			// InputStreamEntity
			if (headerName.equalsIgnoreCase(HttpHeaders.CONTENT_LENGTH))
				continue;
			if (hopByHopHeaders.containsHeader(headerName))
				continue;
			// As per the Java Servlet API 2.5 documentation:
			// Some headers, such as Accept-Language can be sent by clients
			// as several headers each with a different value rather than
			// sending the header as a comma separated list.
			// Thus, we get an Enumeration of the header values sent by the
			// client
			Enumeration headers = servletRequest.getHeaders(headerName);
			while (headers.hasMoreElements()) {
				String headerValue = (String) headers.nextElement();
				// In case the proxy host is running multiple virtual servers,
				// rewrite the Host header to ensure that we get content from
				// the correct virtual server
				if (headerName.equalsIgnoreCase(HttpHeaders.HOST)) {
					HttpHost host = URIUtils.extractHost(this.targetUri);
					headerValue = host.getHostName();
					if (host.getPort() != -1)
						headerValue += ":" + host.getPort();
				}
				proxyRequest.addHeader(headerName, headerValue);
			}
		}
	}

	/** Copy proxied response headers back to the servlet client. */
	protected void copyResponseHeaders(HttpResponse proxyResponse,
			HttpServletResponse servletResponse) {
		for (Header header : proxyResponse.getAllHeaders()) {
			if (hopByHopHeaders.containsHeader(header.getName()))
				continue;
			servletResponse.addHeader(header.getName(), header.getValue());
		}
	}

	/**
	 * Copy response body data (the entity) from the proxy to the servlet
	 * client.
	 */
	private void copyResponseEntity(HttpResponse proxyResponse,
			HttpServletResponse servletResponse) throws IOException {
		HttpEntity entity = proxyResponse.getEntity();
		if (entity != null) {
			OutputStream servletOutputStream = servletResponse
					.getOutputStream();
			try {
				entity.writeTo(servletOutputStream);
			} finally {
				closeQuietly(servletOutputStream);
			}
		}
	}

	private String rewriteUrlFromRequest(HttpServletRequest servletRequest) {
		StringBuilder uri = new StringBuilder(500);
		
		String queryStr=servletRequest.getQueryString();
		String requestedURI=null;
		if(null != queryStr)
		{
			String[] queryStrParams=queryStr.split("&");
			
			for(String arg:queryStrParams)
			{
				if(arg.contains("resourceName"))
				{
					String[] argValue=arg.split("=");
					requestedURI=argValue[1];
					break;
				}
			}
		}
		
		
		System.out.println("Requested resource"+requestedURI);
		System.out.println("Requested URI"+targetURIMap.get(requestedURI));
		
		
		uri.append(targetURIMap.get(requestedURI));
		
		try {
			targetUri = new URI(targetURIMap.get(requestedURI));
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// Handle the path given to the servlet
		if (servletRequest.getPathInfo() != null) {// ex: /my/path.html
			uri.append(servletRequest.getPathInfo());
		}
		// Handle the query string
		String queryString = servletRequest.getQueryString();// ex:(following
																// '?'):
																// name=value&foo=bar#fragment
		if (queryString != null && queryString.length() > 0) {
			uri.append('?');
			int fragIdx = queryString.indexOf('#');
			String queryNoFrag = (fragIdx < 0 ? queryString : queryString
					.substring(0, fragIdx));
			uri.append(encodeUriQuery(queryNoFrag));
			if (fragIdx >= 0) {
				uri.append('#');
				uri.append(encodeUriQuery(queryString.substring(fragIdx + 1)));
			}
		}
		return uri.toString();
	}

	private String rewriteUrlFromResponse(HttpServletRequest servletRequest,
			String theUrl) {
		// TODO document example paths
		if (theUrl.startsWith(this.targetUri.toString())) {
			String curUrl = servletRequest.getRequestURL().toString();// no
																		// query
			String pathInfo = servletRequest.getPathInfo();
			if (pathInfo != null) {
				assert curUrl.endsWith(pathInfo);
				curUrl = curUrl.substring(0,
						curUrl.length() - pathInfo.length());// take pathInfo
																// off
			}
			theUrl = curUrl
					+ theUrl.substring(this.targetUri.toString().length());
		}
		return theUrl;
	}

	/**
	 * <p>
	 * Encodes characters in the query or fragment part of the URI.
	 * 
	 * <p>
	 * Unfortunately, an incoming URI sometimes has characters disallowed by the
	 * spec. HttpClient insists that the outgoing proxied request has a valid
	 * URI because it uses Java's {@link URI}. To be more forgiving, we must
	 * escape the problematic characters. See the URI class for the spec.
	 * 
	 * @param in
	 *            example: name=value&foo=bar#fragment
	 */
	static CharSequence encodeUriQuery(CharSequence in) {
		// Note that I can't simply use URI.java to encode because it will
		// escape pre-existing escaped things.
		StringBuilder outBuf = null;
		Formatter formatter = null;
		for (int i = 0; i < in.length(); i++) {
			char c = in.charAt(i);
			boolean escape = true;
			if (c < 128) {
				if (asciiQueryChars.get((int) c)) {
					escape = false;
				}
			} else if (!Character.isISOControl(c) && !Character.isSpaceChar(c)) {// not-ascii
				escape = false;
			}
			if (!escape) {
				if (outBuf != null)
					outBuf.append(c);
			} else {
				// escape
				if (outBuf == null) {
					outBuf = new StringBuilder(in.length() + 5 * 3);
					outBuf.append(in, 0, i);
					formatter = new Formatter(outBuf);
				}
				// leading %, 0 padded, width 2, capital hex
				formatter.format("%%%02X", (int) c);// TODO
			}
		}
		return outBuf != null ? outBuf : in;
	}

	static final BitSet asciiQueryChars;
	static {
		char[] c_unreserved = "_-!.~'()*".toCharArray();// plus alphanum
		char[] c_punct = ",;:$&+=".toCharArray();
		char[] c_reserved = "?/[]@".toCharArray();// plus punct

		asciiQueryChars = new BitSet(128);
		for (char c = 'a'; c <= 'z'; c++)
			asciiQueryChars.set((int) c);
		for (char c = 'A'; c <= 'Z'; c++)
			asciiQueryChars.set((int) c);
		for (char c = '0'; c <= '9'; c++)
			asciiQueryChars.set((int) c);
		for (char c : c_unreserved)
			asciiQueryChars.set((int) c);
		for (char c : c_punct)
			asciiQueryChars.set((int) c);
		for (char c : c_reserved)
			asciiQueryChars.set((int) c);

		asciiQueryChars.set((int) '%');// leave existing percent escapes in
										// place
	}

}
