package michael.slf4j.investment.init;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;

import michael.slf4j.investment.controller.TaskManagerController;
import michael.slf4j.investment.etl.FutureLoader;

@Component
@Controller
public class InitRunner implements CommandLineRunner {
	private static final Logger log = Logger.getLogger(InitRunner.class);
	
	@Autowired
	private FutureLoader loader;
	
	@Autowired
	private TaskManagerController taskManagerController;

    @Override
    public void run(String... args) throws Exception {
    	log.info("Initializing...");
    	loader.init();
    	taskManagerController.retrieveData();
    	log.info("Done to initialize resources.");
    }
}