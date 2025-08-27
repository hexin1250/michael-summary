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
	
	static {
		System.setProperty("webdriver.chrome.driver", "C:\\software\\chrome\\chromedriver.exe");
	}

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
    	log.info("Done to initialize resources.");
    	dataLoaderClient.cleanup();
    	log.info("Done to cleanup.");
    	
//    	log.info("Start to initialize top deal data.");
//    	dataLoaderClient.loadMainTopDeal();
//    	log.info("Done to load top deal data.");
    }
}