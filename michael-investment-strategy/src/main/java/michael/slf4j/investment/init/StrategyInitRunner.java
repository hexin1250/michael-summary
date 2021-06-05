package michael.slf4j.investment.init;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;

import michael.slf4j.investment.util.HolidayUtil;

@Component
@Controller
public class StrategyInitRunner implements CommandLineRunner {

	@Override
	public void run(String... args) throws Exception {
		HolidayUtil.$.loadHolidays();
	}

}
