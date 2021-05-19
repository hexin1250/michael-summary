package michael.slf4j.investment.init;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;

import michael.slf4j.investment.cron.CleanCronJob;
import michael.slf4j.investment.etl.FutureLoader;

@Component
@Controller
public class InitRunner implements CommandLineRunner {
	private static final Logger log = Logger.getLogger(InitRunner.class);
	
	@Autowired
	private FutureLoader loader;
	
	@Autowired
	private CleanCronJob cleanJob;

    @Override
    public void run(String... args) throws Exception {
    	log.info("Initializing...");
    	cleanJob.cleanData();
    	loader.init();
    	log.info("Done to initialize resources.");
    }
}