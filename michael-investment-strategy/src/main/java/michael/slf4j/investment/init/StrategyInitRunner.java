package michael.slf4j.investment.init;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;

import michael.slf4j.investment.quant.live.LiveProcessor;
import michael.slf4j.investment.service.FileService;
import michael.slf4j.investment.util.HolidayUtil;

@Component
@Controller
public class StrategyInitRunner implements CommandLineRunner {
	
	@Autowired
	private LiveProcessor liveProcessor;
	
	@Autowired
	private FileService fileService;
	
 	@Override
	public void run(String... args) throws Exception {
 		liveProcessor.beforeTrading();
		HolidayUtil.$.loadHolidays();
		
		fileService.getFileStatus();
	}

}
