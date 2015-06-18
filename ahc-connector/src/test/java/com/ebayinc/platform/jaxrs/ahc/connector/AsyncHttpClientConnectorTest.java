package com.ebayinc.platform.jaxrs.ahc.connector;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.BoundRequestBuilder;
import org.asynchttpclient.FluentCaseInsensitiveStringsMap;
import org.asynchttpclient.ListenableFuture;
import org.asynchttpclient.Request;
import org.asynchttpclient.Response;
import org.glassfish.jersey.client.ClientRequest;
import org.glassfish.jersey.client.ClientResponse;
import org.glassfish.jersey.client.spi.AsyncConnectorCallback;
import org.glassfish.jersey.client.spi.Connector;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ebayinc.platform.jaxrs.ahc.connector.AsyncHttpClientConnector;
import com.ebayinc.platform.jaxrs.ahc.connector.AsyncHttpClientConnectorProvider;

/**
 * Test for the AsynHttpClientConnectorProvider and 
 * AsyncHttpClientConnector creation, setup and execution.
 */
public class AsyncHttpClientConnectorTest {

	private static final int HEADER_SIZE = 2;

	private static Connector s_connector;
	private static AsyncHttpClient client;

	@BeforeClass
	public static void setup(){
		Client jaxrsClient = ClientBuilder.newClient();
		client = mock(AsyncHttpClient.class);
		AsyncHttpClientConnectorProvider cp = new AsyncHttpClientConnectorProvider(client);
		s_connector = cp.getConnector(jaxrsClient, jaxrsClient.getConfiguration());
	}

	@Test(expected=IllegalStateException.class)
	public void testCreateNullClient(){
		new AsyncHttpClientConnectorProvider(null);
	}

	@Test
	public void testCreateAsyncHttpClientConnector(){
		assertNotNull(s_connector);
		assertTrue(s_connector instanceof AsyncHttpClientConnector);
	}

	@Test
	public void testGETMockCall() throws Exception{
		ClientRequest invocation = mockRequest("GET");
		ClientResponse response = s_connector.apply(invocation);
		assertResponse(response);
	}

	@Test(expected=ProcessingException.class)
	public void testBadGetCallwithEntity() throws Exception{
		ClientRequest invocation = mockRequest("GET", new FakeEntity());
		ClientResponse response = s_connector.apply(invocation);
		assertResponse(response);
	}


	@Test(expected=ProcessingException.class)
	public void testBadGetCallErrorinStream() throws Exception{
		ClientRequest invocation = mockRequest("GET", null, true, false);
		ClientResponse response = s_connector.apply(invocation);
		assertResponse(response);
	}

	@Test(expected=IllegalStateException.class)
	public void testHttpClientCallFails() throws Exception{
		ClientRequest invocation = mockRequest("GET", null, false, true);
		ClientResponse response = s_connector.apply(invocation);
		assertResponse(response);
	}

	@Test
	public void testPOSTWithEntity() throws Exception{
		ClientRequest invocation = mockRequest("POST", new FakeEntity());
		ClientResponse response = s_connector.apply(invocation);
		assertResponse(response);
	}

	@Test
	public void testAsyncGETMockCall() throws Exception{
		ClientRequest invocation = mockRequest("GET");
		s_connector.apply(invocation, newConnectorCallback("testAsyncGETMockCall"));
		//wait 5 sec for the async execution to be completed
		Thread.sleep(5000);
	}

	@Test
	public void testAsyncPOSTMockCall() throws Exception{
		ClientRequest invocation = mockRequest("POST", new FakeEntity());
		s_connector.apply(invocation, newConnectorCallback("testAsyncPOSTMockCall"));
		//wait 5 sec for the async execution to be completed
		Thread.sleep(5000);
	}

	private static ClientRequest mockRequest(String method) throws Exception{
		return mockRequest(method, null);
	}

	private static ClientRequest mockRequest(String method, Object entity) throws Exception {
		return mockRequest(method, entity,false, false);
	}

	private static ClientRequest mockRequest(
			String method, //
			Object entity, //
			boolean responseStreamHasException, //
			boolean clientCallFails//
			) throws Exception{
		ClientRequest invocation = mock(ClientRequest.class);
		//mock get
		when(invocation.getMethod()).thenReturn(method);
		//no entity
		when(invocation.getEntity()).thenReturn(entity);
		//fake the headers

		MultivaluedMap<String, Object> inheaders = new MultivaluedHashMap<>();
		inheaders.add("Accept", MediaType.APPLICATION_XML_TYPE);
		inheaders.add("UUID", "12345");
		when(invocation.getHeaders()).thenReturn(inheaders);
		//now fake the URI
		when(invocation.getUri()).thenReturn(new URI("http://localhost:8080/fakeurl"));

		Response mockresp = mock(Response.class);
		when(mockresp.getStatusCode()).thenReturn(200);
		when(mockresp.getContentType()).thenReturn(MediaType.APPLICATION_XML);
		//throw a fake xml snippet
		ByteArrayInputStream bais = new ByteArrayInputStream("<somefakexml><here>yes</here></somefakexml>".getBytes());

		if (!responseStreamHasException){
			when(mockresp.getResponseBodyAsStream()).thenReturn(bais);
		} else {
			when(mockresp.getResponseBodyAsStream()).thenThrow(IOException.class);
		}

		//maybe throw some headers
		when(mockresp.hasResponseHeaders()).thenReturn(Boolean.TRUE);
		FluentCaseInsensitiveStringsMap respheaders = new FluentCaseInsensitiveStringsMap();
		respheaders.add("UUID", "1234");
		respheaders.add("responseheader", "1245", "yesihave", "noidont");
		when(mockresp.getHeaders()).thenReturn(respheaders);


		ListenableFuture<Response> mockrespfut = mock(ListenableFuture.class);
		when(mockrespfut.get()).thenReturn(mockresp);

		//this is the execution of the request itself that is being mocked
		BoundRequestBuilder brb = mock(BoundRequestBuilder.class);

		if (!clientCallFails) {
			when(brb.execute()).thenReturn(mockrespfut);
		}else {
			when(brb.execute()).thenThrow(IllegalStateException.class);
		}

		when(client.prepareRequest(any(Request.class))).thenReturn(brb);
		return invocation;
	}

	private static void assertResponse(ClientResponse response) {
		assertNotNull(response);

		//this will return false if there is no stream so just kick it and the stream mocked above will kick in
		assertTrue(response.hasEntity());
		assertTrue(response.getHeaders().size() == HEADER_SIZE);
	}

	private static AsyncConnectorCallback newConnectorCallback(final String testMethodName){
		return new AsyncConnectorCallback() {

			@Override
			public void response(ClientResponse response) {
				assertResponse(response);
			}

			@Override
			public void failure(Throwable failure) {
				//should not reach here
				fail("Exception in processing " + testMethodName);
			}
		};
	}

	private static class FakeEntity{
		String fake;

		public void setFake(String fake){
			this.fake = fake;
		}

		public String getFake(){
			return fake;
		}
	}
}
