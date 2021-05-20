package michael.slf4j.investment.cron;

import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;

import michael.slf4j.investment.constant.Constants;
import michael.slf4j.investment.etl.FutureLoader;
import michael.slf4j.investment.model.TimeseriesModel;
import michael.slf4j.investment.repo.TimeseriesRepository;

@Component
@Controller
@PropertySource("classpath:/schedule.properties")
public class ScheduleJob {
	private static final Logger log = Logger.getLogger(ScheduleJob.class);
	
	@Autowired
	private TimeseriesRepository timeseriesRepository;

	@Autowired
	private FutureLoader loader;
	
	@Scheduled(cron = "${clean-schedule}")
	public void cleanData() {
		log.info("Start to clean data.");
		for (String variety : Constants.VARIETY_LIST) {
			List<String> tradeDateList = timeseriesRepository.findMaxTradeDate(variety);
			tradeDateList.stream().forEach(tradeDate -> {
				List<String> securites = timeseriesRepository.findSecurities(variety, tradeDate);
				securites.stream().forEach(security -> {
					List<TimeseriesModel> eodList = timeseriesRepository.findByTradeDateWithPeriod(security, tradeDate, "1D");
					if(eodList.isEmpty()) {
						List<TimeseriesModel> tickList = timeseriesRepository.findByTradeDateWithPeriod(security, tradeDate, "1MI");
						if(!tickList.isEmpty()) {
							TimeseriesModel latest = tickList.get(tickList.size() - 1).copy();
							latest.setFreq("1D");
							timeseriesRepository.save(latest);
							log.info("Update for security[" + security + "," + tradeDate + "]");
						}
					}
				});
			});
		}
		log.info("complete to update.");
	}
	
	@Scheduled(cron = "${update-primary}")
	public void initPrimaryContract() {
		loader.init();
	}

}
