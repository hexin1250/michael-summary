package michael.slf4j.investment.message.service;

import java.util.Arrays;

import javax.jms.ConnectionFactory;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.core.JmsTemplate;

@Configuration
public class JmsBeanConfiguration {

	@Value("${spring.activemq.broker-url}")
	private String brokerUrl;

	@Bean
	public ConnectionFactory connectionFactory() {
		ActiveMQConnectionFactory activeMQConnectionFactory = new ActiveMQConnectionFactory();
		activeMQConnectionFactory.setBrokerURL(brokerUrl);
		activeMQConnectionFactory.setTrustedPackages(Arrays.asList("michael.slf4j.investment", "java.util", "java.math", "java.lang", "java.sql"));
		return activeMQConnectionFactory;
	}

	@Bean("jmsTemplate")
	public JmsTemplate jmsTemplate() {
		JmsTemplate jmsTemplate = new JmsTemplate();
		jmsTemplate.setConnectionFactory(connectionFactory());
		jmsTemplate.setPubSubDomain(true);
		return jmsTemplate;
	}
	
	@Bean
	public DefaultJmsListenerContainerFactory jmsListenerContainerFactory(){
	    DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
	    factory.setConnectionFactory(connectionFactory());
	    factory.setPubSubDomain(true);
	    return factory;
	}

}
