// camel-k: dependency=camel-infinispan dependency=camel-bean dependency=camel-jackson dependency=mvn:org.wildfly.security:wildfly-elytron:1.11.2.Final dependency=mvn:io.netty:netty-codec:4.1.49.Final configmap=test-config secret=test-cert-secret
package test;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.infinispan.InfinispanConstants;
import org.apache.camel.component.infinispan.InfinispanOperation;

public class Test extends RouteBuilder {
	@Override
	public void configure() throws Exception {	
		from("timer:cleanup?repeatCount=1")
			.routeId("reset-cache")
			.log("Sending entry to cache")
			.setHeader(InfinispanConstants.OPERATION).constant(InfinispanOperation.PUT)
			.setHeader(InfinispanConstants.KEY).constant("test")
			.setHeader(InfinispanConstants.VALUE).constant("test")
			.to("infinispan:default");
	}
}
