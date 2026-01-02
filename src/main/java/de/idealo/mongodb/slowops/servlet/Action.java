/*
 * Copyright (c) 2013 idealo internet GmbH -- all rights reserved.
 */
package de.idealo.mongodb.slowops.servlet;


import com.google.common.collect.Lists;
import de.idealo.mongodb.slowops.collector.CollectorManagerInstance;
import de.idealo.mongodb.slowops.dto.ApplicationStatusDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.List;


@Path("/action")
public class Action {

    private static final Logger LOG = LoggerFactory.getLogger(Action.class);

    private static final int SLOWMS_DEFAULT = 100;

    // Add a single shared, safely-configured ObjectMapper to avoid unsafe default typing
    private static final ObjectMapper MAPPER = createSafeObjectMapper();

    private static ObjectMapper createSafeObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        // Do not enable any default typing. If polymorphic deserialization is required,
        // use a strict PolymorphicTypeValidator and explicitly enable only the allowed types.
        // Basic safe defaults:
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        // ...other safe configuration can be added here if needed
        return mapper;
    }


    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public ApplicationStatusDto getApplicationJSON(@QueryParam("cmd") String cmd, @QueryParam("p") String p, @QueryParam("ms") String ms, @Context HttpServletRequest req) {
        LOG.info(">>> getApplicationJSON cmd: {} p: {} ms:{}", new Object[]{cmd, p, ms});
        ApplicationStatusDto result = null;
        try {
			final List<Integer> pList = Lists.newArrayList();
			final boolean isAuthenticated = ApplicationStatus.isAuthenticated(req);

			if (p != null) {
				String[] params = p.split(",");
				if (params.length > 0) {
					for (String param : params) {
						try {
							pList.add(Integer.parseInt(param));
						} catch (NumberFormatException e) {
							LOG.warn("Parameter should but is not numeric: {}", param);
						}
					}
				}

				if (pList.size() > 0) {
					if (isAuthenticated) {
						if ("cstart".equals(cmd)) {
							CollectorManagerInstance.startStopProfilingReaders(pList, false);
						} else if ("cstop".equals(cmd)) {
							CollectorManagerInstance.startStopProfilingReaders(pList, true);
						} else if ("pstart".equals(cmd)) {
							long slowMs = getSlowMs(ms);
							CollectorManagerInstance.setSlowMs(pList, Math.abs(slowMs));
						} else if ("pstop".equals(cmd)) {
							long slowMs = getSlowMs(ms);
							if(slowMs == 0) slowMs = SLOWMS_DEFAULT;//set to default because 0 means profile all ops but we want to stop profiling here
							CollectorManagerInstance.setSlowMs(pList, Math.abs(slowMs)*-1);
						}
					}
					result = CollectorManagerInstance.getApplicationStatus(pList, isAuthenticated);
				}
			} else if ("rc".equals(cmd)) {
				result = CollectorManagerInstance.getApplicationStatus(pList, isAuthenticated);
			} else {
				result = CollectorManagerInstance.getApplicationStatus(isAuthenticated);
			}

			
			try {
				// Use shared, safely-configured mapper for logging/serialization
				LOG.debug("getApplicationJSON result:");
				LOG.debug(MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(result));
			} catch (IOException e) {
				LOG.error("IOException while logging ApplicationStatusDto", e);
			}


		}catch(Throwable t){
			ApplicationStatusDto.addWebLog(t.getClass().getSimpleName() + " while getting application status.");
			LOG.error("Error in getApplicationJSON", t);
		}
		LOG.info("<<< getApplicationJSON");
		return result;
	}

	private long getSlowMs(String ms){
		try {
			return Long.parseLong(ms);
		} catch (NumberFormatException e) {
			ApplicationStatusDto.addWebLog("slowMS must be numeric but was: '" + ms + "' so take default: " + SLOWMS_DEFAULT);
			LOG.warn("slowMS must be numeric but was: '{}' so take default: {}", ms, SLOWMS_DEFAULT);
		}
		return SLOWMS_DEFAULT;
	}


}
