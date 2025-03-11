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
    	taskManager.subscribeSecurities();
    	if(TradeUtil.isTradingTime()) {
			dataLoaderClient.init15MinData();
			dataLoaderClient.init30MinData();
    	}
//    	dataLoaderClient.load15MiData("RB2505");
//    	dataLoaderClient.reload("RB2505");
    	log.info("Done to initialize resources.");
    }
}