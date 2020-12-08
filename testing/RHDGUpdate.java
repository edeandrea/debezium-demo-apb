// camel-k: language=java dependency=camel-infinispan dependency=camel-kafka dependency=camel-jsonpath dependency=camel-jackson dependency=mvn:org.wildfly.security:wildfly-elytron:1.11.2.Final configmap=camelk-rhdg-client-config secret=albums-rhdg-cert-secret trait=quarkus.enabled=false
package com.redhat.dbzdemo.rhdg;

import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.infinispan.InfinispanConstants;
import org.apache.camel.component.infinispan.InfinispanOperation;
import org.apache.camel.model.dataformat.JsonLibrary;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.HashMap;

public class RHDGUpdate extends RouteBuilder {
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	@SuppressWarnings("unchecked")
	private Map<String, Object> getPayload(Map<String, Object> body) {
		return (Map<String, Object>) body.getOrDefault("payload", new HashMap<String, Object>());
	}

	public String getAlbumId(Map<String, Object> body) {
		return (String) getPayload(body).get("aggregateId");
	}

	@SuppressWarnings("unchecked")
	public String getAlbum(Map<String, Object> body) {
		try {
			String albumPayload = (String) ((Map<String, Object>) getPayload(body)).getOrDefault("payload", "");
			Map<String, Object> albumPayloadMap = OBJECT_MAPPER.readValue(albumPayload, Map.class);
			Map<String, Object> album = (Map<String, Object>) albumPayloadMap.get("album");

			return OBJECT_MAPPER.writeValueAsString(album);
		}
		catch (JsonProcessingException ex) {
			throw new RuntimeException(ex);
		}
	}

	@Override
	public void configure() throws Exception {
		from("kafka:{{topic}}?groupId={{groupId}}&clientId={{clientId}}&autoOffsetReset=earliest")
			.routeId("{{groupId}}")
			.log("Processing event: ${body}")
			.choice()
				.when()
					.jsonpath("$.payload[?(@.eventType == 'ALBUM_CREATED')]")
					.to("direct:created")
				.when()
					.jsonpath("$.payload[?(@.eventType == 'ALBUM_DELETED')]")
					.to("direct:deleted")
				.when()
					.jsonpath("$.payload[?(@.eventType == 'ALBUM_UPDATED')]")
					.to("direct:updated")
				.otherwise()
					.to("direct:noeventtype");
						
		from("direct:created")
			.routeId("album-created")
			.log("Event is ALBUM_CREATED")
			.unmarshal().json(JsonLibrary.Jackson, Map.class)
			.setHeader(InfinispanConstants.OPERATION).constant(InfinispanOperation.PUT)
			.setHeader(InfinispanConstants.KEY).method(this, "getAlbumId(${body})")
			.setHeader(InfinispanConstants.VALUE).method(this, "getAlbum(${body})")
			.to("infinispan:{{cacheName}}");

		from("direct:deleted")
			.routeId("album-deleted")
			.log("Event is ALBUM_DELETED")
			.unmarshal().json(JsonLibrary.Jackson, Map.class)
			.setHeader(InfinispanConstants.OPERATION).constant(InfinispanOperation.REMOVE)
			.setHeader(InfinispanConstants.KEY).method(this, "getAlbumId(${body})")
			.to("infinispan:{{cacheName}}");

		from("direct:updated")
			.routeId("album-updated")
			.log("Event is ALBUM_UPDATED")
			.unmarshal().json(JsonLibrary.Jackson, Map.class)
			.setHeader(InfinispanConstants.OPERATION).constant(InfinispanOperation.PUT)
			.setHeader(InfinispanConstants.KEY).method(this, "getAlbumId(${body})")
			.setHeader(InfinispanConstants.VALUE).method(this, "getAlbum(${body})")
			.to("infinispan:{{cacheName}}");

		from("direct:noeventtype")
		  .routeId("no-event-type")
			.log(LoggingLevel.ERROR, "No eventType found for album ${headers[kafka.KEY]}");
	}
}
