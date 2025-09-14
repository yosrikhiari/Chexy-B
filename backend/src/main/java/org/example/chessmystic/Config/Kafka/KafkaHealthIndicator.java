package org.example.chessmystic.Config.Kafka;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
// import org.springframework.boot.actuator.health.Health;
// import org.springframework.boot.actuator.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

// @Component
public class KafkaHealthIndicator /* implements HealthIndicator */ {

    private static final Logger logger = LoggerFactory.getLogger(KafkaHealthIndicator.class);

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    // @Override
    public String health() {
        try {
            Properties props = new Properties();
            props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
            props.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, 5000);

            try (AdminClient adminClient = AdminClient.create(props)) {
                DescribeClusterResult clusterResult = adminClient.describeCluster();
                
                // Check if we can connect to Kafka cluster
                int nodeCount = clusterResult.nodes().get(5, TimeUnit.SECONDS).size();
                String clusterId = clusterResult.clusterId().get(5, TimeUnit.SECONDS);
                
                if (nodeCount > 0) {
                    return "UP - Kafka cluster connected. Nodes: " + nodeCount + ", Cluster ID: " + clusterId;
                } else {
                    return "DOWN - No Kafka nodes available";
                }
            }
        } catch (Exception e) {
            logger.error("Kafka health check failed", e);
            return "DOWN - " + e.getMessage();
        }
    }
}
