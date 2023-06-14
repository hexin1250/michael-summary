package michael.slf4j.investment.cron;

import java.util.concurrent.Executors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

@Configuration
@PropertySource("classpath:/cleancron.properties")
//@PropertySource("file:C:/Users/CPU/git/michael-summary/michael-investment/src/main/resources/cleancron.properties")
public class ScheduleConfig implements SchedulingConfigurer {
	@Value("${pool.size}")
	private int maxPoolSize;
	
	@Override
	public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
		taskRegistrar.setScheduler(Executors.newScheduledThreadPool(maxPoolSize));
	}
}