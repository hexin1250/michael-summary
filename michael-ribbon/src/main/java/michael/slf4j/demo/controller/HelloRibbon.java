package michael.slf4j.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.netflix.ribbon.RibbonClient;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
@RibbonClient(name = "hello-service", configuration = RibbonConfiguration.class)
public class HelloRibbon {
	@Bean
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}

	@Autowired
	public RestTemplate restTemplate;

	@RequestMapping("/hi")
	public String hi(@RequestParam(value = "name", defaultValue = "Artaban") String name) {
		String greeting = this.restTemplate.getForObject("http://localhost:8081/greeting", String.class);
		return String.format("%s, %s!", greeting, name);
	}

}
