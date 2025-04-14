package michael.slf4j.investment.init;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;

import michael.slf4j.investment.etl.DataLoaderClient;
import michael.slf4j.investment.taskmanager.TaskManager;
import michael.slf4j.investment.util.LoadFreqFutureData;
import michael.slf4j.investment.util.TradeUtil;

@Component
@Controller
public class InitRunner implements CommandLineRunner {
	private static final Logger log = Logger.getLogger(InitRunner.class);
	
	@Autowired
	private TaskManager taskManager;
	
	@Autowired
	private DataLoaderClient dataLoaderClient;
	
	@Autowired
	private LoadFreqFutureData loader;
	
	@Override
    public void run(String... args) throws Exception {
//		log.info("Start loading data...");
//		loader.loadFreqData();
//		log.info("Done to load data");
		
    	log.info("Initializing...");
    	taskManager.subscribeSecurities();
    	dataLoaderClient.init();
    	if(TradeUtil.isTradingTime()) {
			dataLoaderClient.init15MinData();
			dataLoaderClient.init30MinData();
    	}
//    	dataLoaderClient.load15MiData("RB2510");
//    	dataLoaderClient.reload("RB2510");
    	log.info("Done to initialize resources.");
    	dataLoaderClient.cleanup();
    }
}