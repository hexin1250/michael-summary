package michael.slf4j.investment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class InvestmentApplication {
	public static void main(String[] args) {
		SpringApplication.run(InvestmentApplication.class, args);
	}
}