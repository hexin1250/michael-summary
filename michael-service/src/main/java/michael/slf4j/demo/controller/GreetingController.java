package michael.slf4j.demo.controller;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GreetingController {
	private static Logger log = LoggerFactory.getLogger(GreetingController.class);

	@RequestMapping(value = "/greeting")
	public String index() {
		log.info("Access /greeting");

		List<String> greetings = Arrays.asList("Hi there", "Greetings", "Salutations");
		Random rand = new Random();

		int randomNum = rand.nextInt(greetings.size());
		return greetings.get(randomNum);
	}

	@RequestMapping(value = "/")
	public String home() {
		log.info("Access /");
		return "Hi!";
	}
}
