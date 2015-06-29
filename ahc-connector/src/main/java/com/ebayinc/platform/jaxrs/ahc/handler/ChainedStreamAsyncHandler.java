package com.ebayinc.platform.jaxrs.ahc.handler;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.asynchttpclient.AsyncCompletionHandler;
import org.asynchttpclient.FluentCaseInsensitiveStringsMap;
import org.asynchttpclient.HttpResponseBodyPart;
import org.asynchttpclient.HttpResponseHeaders;
import org.asynchttpclient.HttpResponseStatus;
import org.asynchttpclient.Response;

import com.ebayinc.platform.jaxrs.ahc.io.ChainedByteArrayInputStream;

public class ChainedStreamAsyncHandler extends AsyncCompletionHandler<Response> {

	private ChainedByteArrayInputStream chainedByteArrayIS;
	private int statusCode;
	private FluentCaseInsensitiveStringsMap ahcHeaders;

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
		return super.onHeadersReceived(headers);
	}

	public ChainedStreamAsyncHandler() {
		this.chainedByteArrayIS = new ChainedByteArrayInputStream();
	}

	@Override
	public org.asynchttpclient.AsyncHandler.State onBodyPartReceived(HttpResponseBodyPart bodyPart) throws Exception {
		this.chainedByteArrayIS.chain((ByteArrayInputStream) bodyPart.readBodyPartBytes());
		return State.CONTINUE;
	}

	public InputStream getInputStream() {
		return this.chainedByteArrayIS;
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

	@Override
	public Response onCompleted(Response response) throws Exception {
		return response;
	}
}
