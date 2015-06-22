package com.ebayinc.platform.jaxrs.ahc.handler;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import org.asynchttpclient.AsyncCompletionHandler;
import org.asynchttpclient.FluentCaseInsensitiveStringsMap;
import org.asynchttpclient.HttpResponseBodyPart;
import org.asynchttpclient.HttpResponseHeaders;
import org.asynchttpclient.HttpResponseStatus;
import org.asynchttpclient.Response;

public class PipedAsyncHandler extends AsyncCompletionHandler<Response> {

	private static final int PIPE_SIZE = 1024 * 1024 * 5; //5 MB
	private PipedInputStream pipedInputStream;
	private int statusCode;
	private FluentCaseInsensitiveStringsMap ahcHeaders;
	
	protected PipedOutputStream pipedOutputStream;

	@Override
	public org.asynchttpclient.AsyncHandler.State onStatusReceived(
			HttpResponseStatus status) throws Exception {
		statusCode = status.getStatusCode();
		return super.onStatusReceived(status);
	}
	
	@Override
	public org.asynchttpclient.AsyncHandler.State onHeadersReceived(
			HttpResponseHeaders headers) throws Exception {
		ahcHeaders = headers.getHeaders();
		return super.onHeadersReceived(headers);
	}
	
	@Override
	public Response onCompleted(Response response) throws Exception {
		pipedOutputStream.flush();
		pipedOutputStream.close();
		return response;
	}
	public PipedAsyncHandler() {
		pipedOutputStream = new PipedOutputStream();
		try {
			pipedInputStream = new PipedInputStream(pipedOutputStream, PIPE_SIZE);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public org.asynchttpclient.AsyncHandler.State onBodyPartReceived(
			HttpResponseBodyPart bodyPart) throws Exception {
		pipedOutputStream.write(bodyPart.getBodyPartBytes());
		return State.CONTINUE;
	}

	public InputStream getInputStream() {
		return pipedInputStream;
	}
	
	public boolean headersReceived() {
		return ahcHeaders != null;
	}

	public int getStatusCode() {
		return statusCode;
	}

	public FluentCaseInsensitiveStringsMap getAhcHeaders() {
		return ahcHeaders;
	}
}
