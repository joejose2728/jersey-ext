package com.ebayinc.platform.jaxrs.ahc.handler;

import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.List;

import org.asynchttpclient.AsyncCompletionHandler;
import org.asynchttpclient.FluentCaseInsensitiveStringsMap;
import org.asynchttpclient.HttpResponseBodyPart;
import org.asynchttpclient.HttpResponseHeaders;
import org.asynchttpclient.HttpResponseStatus;
import org.asynchttpclient.Response;

public class PipedAsyncHandler extends AsyncCompletionHandler<Response> {

	private static final int PIPE_SIZE = 1024 * 10; // 10 KB

	private PipedInputStream pipedInputStream;
	private int statusCode;
	private FluentCaseInsensitiveStringsMap ahcHeaders;

	protected PipedOutputStream pipedOutputStream;

	@Override
	public org.asynchttpclient.AsyncHandler.State onStatusReceived(
			HttpResponseStatus status) throws Exception {
		this.statusCode = status.getStatusCode();
		return super.onStatusReceived(status);
	}

	@Override
	public org.asynchttpclient.AsyncHandler.State onHeadersReceived(
			HttpResponseHeaders headers) throws Exception {
		this.ahcHeaders = headers.getHeaders();

		List<String> header = ahcHeaders.get("Content-Length");
		int pipeSize = PIPE_SIZE;
		if (header != null && !header.isEmpty()) {
			try {
				pipeSize = Integer.parseInt(header.get(0));
			} catch (NumberFormatException nfe) {
				pipeSize = PIPE_SIZE;
			}
		}
		/*
		 * Delay input stream creation until response header is received, so
		 * that we can use the content-length header (if present). 
		 * What if the content-length header is large?//TODO
		 */
		this.pipedInputStream = new PipedInputStream(pipedOutputStream,
				pipeSize);
		return super.onHeadersReceived(headers);
	}

	@Override
	public Response onCompleted(Response response) throws Exception {
		this.pipedOutputStream.flush();
		this.pipedOutputStream.close();
		return response;
	}

	public PipedAsyncHandler() {
		this.pipedOutputStream = new PipedOutputStream();
	}

	@Override
	public org.asynchttpclient.AsyncHandler.State onBodyPartReceived(HttpResponseBodyPart bodyPart) throws Exception {
		this.pipedOutputStream.write(bodyPart.getBodyPartBytes());
		return State.CONTINUE;
	}

	public InputStream getInputStream() {
		return this.pipedInputStream;
	}

	public boolean headersReceived() {
		return this.ahcHeaders != null;
	}

	public int getStatusCode() {
		return this.statusCode;
	}

	public FluentCaseInsensitiveStringsMap getAhcHeaders() {
		return this.ahcHeaders;
	}
}
