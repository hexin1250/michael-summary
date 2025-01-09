package michael.slf4j.investment.init;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;

import michael.slf4j.investment.etl.DataLoaderClient;
import michael.slf4j.investment.taskmanager.TaskManager;
import michael.slf4j.investment.util.TradeUtil;

@Component
@Controller
public class InitRunner implements CommandLineRunner {
	private static final Logger log = Logger.getLogger(InitRunner.class);
	
	@Autowired
	private TaskManager taskManager;
	
	@Autowired
	private DataLoaderClient dataLoaderClient;

    @Override
    public void run(String... args) throws Exception {
    	log.info("Initializing...");
    	if(TradeUtil.isTradingTime()) {
			taskManager.subscribeAll();
			dataLoaderClient.init15MinData();
			dataLoaderClient.init30MinData();
    	}
    	log.info("Done to initialize resources.");
    }
}