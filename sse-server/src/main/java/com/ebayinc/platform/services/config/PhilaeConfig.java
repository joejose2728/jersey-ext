package com.ebayinc.platform.services.config;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

@ApplicationPath(PhilaeConfig.APPLICATION_PATH)
public class PhilaeConfig extends Application {

	public static final String APPLICATION_PATH = "/services/v1";
	
}
