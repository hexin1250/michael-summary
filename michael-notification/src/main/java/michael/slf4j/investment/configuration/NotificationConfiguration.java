package michael.slf4j.investment.configuration;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import michael.slf4j.investment.util.WeChatRobot;

@Configuration
public class NotificationConfiguration {
	@Bean
	@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
	public WeChatRobot robot() {
	    return new WeChatRobot();
	}
}