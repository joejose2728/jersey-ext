
package com.ebayinc.platform.services.resources;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.glassfish.jersey.media.sse.EventOutput;
import org.glassfish.jersey.media.sse.OutboundEvent.Builder;
import org.glassfish.jersey.media.sse.SseFeature;

@Path("events")
public class SseResource {

	@GET 
	@Produces(SseFeature.SERVER_SENT_EVENTS)
	public EventOutput getIt() {
		final EventOutput eventOutput = new EventOutput();
		System.out.println("Request received");
		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					for (;;){

						final Builder builder = new Builder();
						builder.name("message to-esa");
						builder.data("Hello Earth!");

						eventOutput.write(builder.build());		
						Thread.sleep(2000); //sleep for 2 seconds
					}
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
		}).start();
		return eventOutput;
	}
}
