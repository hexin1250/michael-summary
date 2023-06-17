package michael.slf4j.investment;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.EventListener;

import com.google.common.io.Files;

import michael.slf4j.investment.model.RealRunTxn;
import michael.slf4j.investment.repo.RealRunTxnRepository;

@SpringBootApplication
public class InvestmentStrategyApplication {
	
	public static void main(String[] args) {
		SpringApplicationBuilder builder = new SpringApplicationBuilder(InvestmentStrategyApplication.class);
		builder.headless(false);
		builder.run(args);
	}
	
//	private static final Logger log = Logger.getLogger(InvestmentStrategyApplication.class);
//	
//	@Autowired
//	RealRunTxnRepository repo;
//	
//	@EventListener(ApplicationReadyEvent.class)
//	public void runAfterStartup() throws IOException {
//		log.info("Start to initialize");
//		List<String> lines = Files.readLines(new File("src/test/data/testdata.txt"), Charset.defaultCharset());
//		lines.stream().forEach(line -> {
//			String[] parts = line.split(",");
//			RealRunTxn rrt = new RealRunTxn();
//			rrt.setRealRunId(1L);
//			rrt.setSecurity(parts[1]);
//			rrt.setVariety(parts[2]);
//			rrt.setDealPrice(new BigDecimal(parts[3]));
//			rrt.setDealCount(Integer.valueOf(parts[4]));
//			rrt.setDirection(Integer.valueOf(parts[5]));
//			rrt.setTradeDate(parts[6]);
//			LocalDateTime ldt = LocalDateTime.parse(parts[7]);
//			long time = Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant()).getTime();
//			rrt.setTradeTs(new Timestamp(time));
//			repo.save(rrt);
//		});
//		log.info("Done to initialize");
//	}
}