package com.ebayinc.platform.jaxrs.ahc.connector;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response.StatusType;

import org.apache.commons.io.output.DeferredFileOutputStream;
import org.asynchttpclient.AsyncHandler;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.AsyncHttpClientConfig;
import org.asynchttpclient.FluentCaseInsensitiveStringsMap;
import org.asynchttpclient.Request;
import org.asynchttpclient.RequestBuilder;
import org.asynchttpclient.Response;
import org.asynchttpclient.request.body.generator.BodyGenerator;
import org.asynchttpclient.request.body.generator.ByteArrayBodyGenerator;
import org.asynchttpclient.request.body.generator.FileBodyGenerator;
import org.glassfish.jersey.client.ClientRequest;
import org.glassfish.jersey.client.ClientResponse;
import org.glassfish.jersey.client.spi.AsyncConnectorCallback;
import org.glassfish.jersey.client.spi.Connector;
import org.glassfish.jersey.message.internal.OutboundMessageContext.StreamProvider;
import org.glassfish.jersey.message.internal.Statuses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ebayinc.platform.jaxrs.ahc.handler.ChainedStreamAsyncHandler;

public class AsyncHttpClientConnector2 implements Connector {

	private static Logger logger = LoggerFactory
			.getLogger(AsyncHttpClientConnector2.class);

	private File fileUploadTempFileDir = new File(
			System.getProperty("java.io.tmpdir"));
	private int buffersizethreshold = 10485760; // (1024*1024*10) 10 MB

	private AsyncHttpClient httpClient;

	/* package */AsyncHttpClientConnector2(AsyncHttpClient httpClient) {
		this.httpClient = httpClient;
	}

	public ClientResponse apply(ClientRequest request) {
		Request httpRequest = null;
		try {
			httpRequest = buildRequest(request);
			ClientResponse responseContext = processRequest(httpRequest,
					request);
			return responseContext;
		} catch (IOException e) {
			throw new ProcessingException("Unable to perform call", e);
		}
	}

	private ClientResponse processRequest(Request ahcRequest,
			ClientRequest requestContext) throws IOException {
		ChainedStreamAsyncHandler asyncHandler = new ChainedStreamAsyncHandler();
		while (!asyncHandler.headersReceived());

		return buildResponse(requestContext, ahcRequest, asyncHandler);
	}

	public Future<?> apply(final ClientRequest request,
			final AsyncConnectorCallback callback) {
		try {
			final Request httpRequest = buildRequest(request);
			// make use of AHC's callback based async request
			final ChainedStreamAsyncHandler asynHandler = new CallbackBasedAsyncHandler(
					request, httpRequest, callback);
			Future<Response> responseFuture = processRequest(httpRequest, asynHandler);
			return responseFuture;
		} catch (IOException e) {
			callback.failure(e);
			throw new ProcessingException("Unable to perform call", e);
		}
	}

	public String getName() {
		AsyncHttpClientConfig config = httpClient.getConfig();
		return config.getUserAgent() + '_' + AsyncHttpClientConfig.AHC_VERSION;
	}

	public void close() {
		httpClient.closeAsynchronously();
	}

	private Future<Response> processRequest(Request httpRequest,
			AsyncHandler<Response> asyncHandler) throws IOException {
		logger.debug("About to execute the http call: " + httpRequest.getUrl());

		return asyncHandler != null ? httpClient.prepareRequest(httpRequest)
				.execute(asyncHandler) : httpClient.prepareRequest(httpRequest)
				.execute();
	}

	private ClientResponse buildResponse(ClientRequest requestContext,
			Request httpRequest, AsyncHandler<Response> asyncHandler)
			throws IOException {
		logger.debug("Http call executed " + httpRequest.getUrl());
		ChainedStreamAsyncHandler cbaisAsynHandler
		                 = (ChainedStreamAsyncHandler) asyncHandler;
		return buildResponse(cbaisAsynHandler.getStatusCode(),
				cbaisAsynHandler.getInputStream(),
				cbaisAsynHandler.getAhcHeaders(), requestContext);
	}

	private ClientResponse buildResponse(int statusCode,
			InputStream responseStream,
			FluentCaseInsensitiveStringsMap responseHeaders,
			ClientRequest requestContext) {
		StatusType statusType = Statuses.from(statusCode);
		ClientResponse responseContext = new ClientResponse(statusType,
				requestContext);
		// set the response entity stream
		responseContext.setEntityStream(responseStream);
		// extract response headers
		extractHeaders(responseHeaders, responseContext);
		return responseContext;
	}

	private Request buildRequest(ClientRequest request) throws IOException {
		String method = request.getMethod();

		if ("GET".equalsIgnoreCase(method) && request.getEntity() != null) {
			throw new ProcessingException("A GET request cannot have a body.");
		}
		// do not allow a "raw" request because that double encodes the query
		// params
		RequestBuilder builder = new RequestBuilder(method, false);
		builder.setUrl(request.getUri().toString());
		if (request.getEntity() != null) {
			builder.setBody(getBodyGenerator(request));
		}
		// pull the headers here, not before the body because per spec the MBW
		// can change/modify them
		FluentCaseInsensitiveStringsMap headers = buildHeaders(request
				.getHeaders());
		// add the headers to the builder
		builder.setHeaders(headers);
		return builder.build();
	}

	private static FluentCaseInsensitiveStringsMap buildHeaders(
			MultivaluedMap<String, Object> headers) {
		FluentCaseInsensitiveStringsMap ret = new FluentCaseInsensitiveStringsMap();
		Set<Map.Entry<String, List<Object>>> entries = headers.entrySet();
		for (Map.Entry<String, List<Object>> entry : entries) {
			String key = entry.getKey();
			List<String> value = new ArrayList<String>();
			for (Object param : entry.getValue()) {
				// it can happen that the param is null, since we cannot trust
				// that the header was added without
				// a value
				if (param != null) {
					value.add(param.toString());
				}
			}
			ret.add(key, value);
		}
		return ret;
	}

	private BodyGenerator getBodyGenerator(final ClientRequest request)
			throws IOException {
		File tempfile = new File(fileUploadTempFileDir,
				generateUniqueTempFileName(request));
		final DeferredFileOutputStream output = new DeferredFileOutputStream(
				buffersizethreshold, tempfile);
		try {
			request.setStreamProvider(new StreamProvider() {

				@Override
				public OutputStream getOutputStream(int contentLength)
						throws IOException {
					return output;
				}
			});
			request.writeEntity();
			if (output.isInMemory()) {
				return new ByteArrayBodyGenerator(output.getData());
			} else {
				File out = output.getFile();
				out.deleteOnExit();
				return new FileBodyGenerator(out);
			}
		} finally {
			if (output != null) {
				output.close();
			}
		}
	}

	private static String generateUniqueTempFileName(ClientRequest request) {
		return request.hashCode() + "_" + System.currentTimeMillis();
	}

	/**
	 * Adapts the AsyncHttpClient response headers to Jersey response headers
	 * 
	 * @param {@link Response} response
	 * @param {@link ClientResponse} responseContext
	 */
	private static void extractHeaders(
			FluentCaseInsensitiveStringsMap ahcHeaders,
			ClientResponse responseContext) {
		Set<Map.Entry<String, List<String>>> entries = ahcHeaders.entrySet();
		MultivaluedMap<String, String> headers = responseContext.getHeaders();
		for (Map.Entry<String, List<String>> entry : entries) {
			headers.addAll(entry.getKey(), entry.getValue());
		}
	}

	private class CallbackBasedAsyncHandler extends ChainedStreamAsyncHandler {

		private AsyncConnectorCallback asyncConnectorCallback;
		private Request ahcRequest;
		private ClientRequest jerseyRequest;

		public CallbackBasedAsyncHandler(ClientRequest jerseyRequest,
				Request ahcRequest,
				AsyncConnectorCallback asyncConnectorCallback) {
			this.asyncConnectorCallback = asyncConnectorCallback;
			this.jerseyRequest = jerseyRequest;
			this.ahcRequest = ahcRequest;
		}

		@Override
		public Response onCompleted(Response response) throws Exception {
			ClientResponse responseContext = buildResponse(jerseyRequest,
					ahcRequest, this);
			// let the async connector callback know that the response
			// processing is complete
			asyncConnectorCallback.response(responseContext);
			return super.onCompleted(response);
		}

		@Override
		public void onThrowable(Throwable t) {
			super.onThrowable(t);
			// let the async connector callback know that the response
			// processing has failed
			asyncConnectorCallback.failure(t);
		}
	}
}
