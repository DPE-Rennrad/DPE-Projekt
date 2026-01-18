// Eduard Merker

package edu.thi.demo.jms;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.jms.ConnectionFactory;

@ApplicationScoped
public class JmsConfiguration {

    @ConfigProperty(name = "activemq.broker.url", defaultValue = "tcp://localhost:61616")
    String brokerUrl;

    @ConfigProperty(name = "activemq.username", defaultValue = "admin")
    String username;

    @ConfigProperty(name = "activemq.password", defaultValue = "admin")
    String password;

    @Produces
    @Named("jmsConnectionFactory")
    @ApplicationScoped
    public ConnectionFactory connectionFactory() {
        ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory();
        factory.setBrokerURL(brokerUrl);
        factory.setUserName(username);
        factory.setPassword(password);
        factory.setTrustAllPackages(true); // For development only!
        return factory;
    }
}

