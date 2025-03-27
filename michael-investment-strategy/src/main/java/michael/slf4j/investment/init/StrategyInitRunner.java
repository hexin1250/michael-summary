package michael.slf4j.investment.init;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;

import michael.slf4j.investment.quant.live.LiveProcessor;
import michael.slf4j.investment.research.DataResearchV2;
import michael.slf4j.investment.util.HolidayUtil;

@Component
@Controller
public class StrategyInitRunner implements CommandLineRunner {
	
	@Autowired
	private LiveProcessor liveProcessor;
	
	@Autowired
	private DataResearchV2 dataResearchV2;

 	@Override
	public void run(String... args) throws Exception {
 		liveProcessor.beforeTrading();
		HolidayUtil.$.loadHolidays();
    	LocalDateTime ldt = LocalDateTime.now();
//    	ldt = ldt.minusHours(6);
//    	dataResearch.summarize(ldt, true);
//    	dataResearchV2.summarize();
	}

}
