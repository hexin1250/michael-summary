package michael.slf4j.investment.init;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;

import michael.slf4j.investment.model.Variety;
import michael.slf4j.investment.quant.live.LiveProcessor;
import michael.slf4j.investment.research.CoinDataResearch;
import michael.slf4j.investment.research.XAUDataResearch;
import michael.slf4j.investment.service.FileService;
import michael.slf4j.investment.util.HolidayUtil;

@Component
@Controller
public class StrategyInitRunner implements CommandLineRunner {
	
	@Autowired
	private LiveProcessor liveProcessor;
	
	@Autowired
	private FileService fileService;
	
	@Autowired
	private XAUDataResearch xauDataResearch;
	
	@Autowired
	private CoinDataResearch coinDataResearch;
	
 	@Override
	public void run(String... args) throws Exception {
 		liveProcessor.beforeTrading();
		HolidayUtil.$.loadHolidays();
		
		fileService.getFileStatus(Variety.I);
		fileService.getFileStatus(Variety.RB);
		
//		xauDataResearch.summarize();
//		coinDataResearch.summarize();
	}

}
