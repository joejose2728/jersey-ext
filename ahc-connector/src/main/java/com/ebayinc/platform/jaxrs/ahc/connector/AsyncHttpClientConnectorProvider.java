package com.ebayinc.platform.jaxrs.ahc.connector;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.Configuration;

import org.asynchttpclient.AsyncHttpClient;
import org.glassfish.jersey.client.spi.Connector;
import org.glassfish.jersey.client.spi.ConnectorProvider;

public class AsyncHttpClientConnectorProvider implements ConnectorProvider {

	private AsyncHttpClient transport;
	
	public AsyncHttpClientConnectorProvider(AsyncHttpClient transport) {
		if (transport == null){
			throw new IllegalStateException("Transport can not be null.");
		}
		this.transport = transport;
	}
	
	public Connector getConnector(Client client, Configuration runtimeConfig) {
		//TODO: Is there something that we can achieve with 'client' and 'runtimeConfig'. Don't know at this point.
		return new AsyncHttpClientConnector(transport);
	}

}
