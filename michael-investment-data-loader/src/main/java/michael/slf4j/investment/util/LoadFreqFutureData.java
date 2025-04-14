package michael.slf4j.investment.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import michael.slf4j.investment.configuration.FreqEnum;
import michael.slf4j.investment.model.Timeseries;
import michael.slf4j.investment.repo.TimeseriesRepository;

@Component("loadFreqData")
public class LoadFreqFutureData {
	private static final Logger log = Logger.getLogger(LoadFreqFutureData.class);
	private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
	private static final Pattern pattern = Pattern.compile(".*[(](.*)[)].*");
	private static final Pattern VARIETY_P = Pattern.compile("([a-zA-Z]+).*");
	
	@Autowired
	private TimeseriesRepository timeseriesRepository;
	
	public void loadFreqData() {
		File dir = new File("src/main/data/freq");
		File[] dirList = dir.listFiles();
		
		Map<Timestamp, Timeseries> map = new HashMap<>();
		Arrays.stream(dirList).parallel().forEach(freqDir -> {
			String freqStr = freqDir.getName();
			FreqEnum freq = FreqEnum.getFreq(freqStr.toUpperCase());
			if(freq == null) {
				throw new RuntimeException("Unknown freq:" + freqStr);
			}
			File[] dataFileList = freqDir.listFiles();
			Arrays.stream(dataFileList).parallel().forEach(file -> {
				String fileName = file.getName();
				log.info("start to get data from " + fileName);
				String[] parts = fileName.split("[.]");
				String securityStr = parts[0];
				Matcher varietyM = VARIETY_P.matcher(securityStr);
				if(!varietyM.matches()) {
					throw new RuntimeException("Variety is not valid:" + securityStr);
				}
				List<Timeseries> list = timeseriesRepository.getTimeseriesBySecurityFreq(securityStr, freq.getValue());
				Set<Timestamp> set = new HashSet<>();
				for (Timeseries ts : list) {
					set.add(ts.getTradeTs());
				}
				String variety = varietyM.group(1);
				
				String line = null;
				try(BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file)))){
					while((line = br.readLine()) != null) {
						Matcher m = pattern.matcher(line);
						if(!m.matches()) {
							continue;
						}
						String data = m.group(1);
						String[] dataParts = data.split(",");
						BigDecimal open = new BigDecimal(dataParts[0].trim());
						BigDecimal high = new BigDecimal(dataParts[1].trim());
						BigDecimal low = new BigDecimal(dataParts[2].trim());
						BigDecimal close = new BigDecimal(dataParts[3].trim());
						BigDecimal volumn = new BigDecimal(dataParts[4].trim());
						BigDecimal openInterest = new BigDecimal(dataParts[5].trim());
						LocalDateTime ldt = LocalDateTime.parse(dataParts[6].trim(), formatter);
						
						Timeseries ts = new Timeseries();
						ts.setOpen(open);
						ts.setHigh(high);
						ts.setLow(low);
						ts.setClose(close);
						ts.setVolume(volumn);
						ts.setOpenInterest(openInterest);
						ts.setTradeTs(TradeUtil.getTimestamp(ldt));
						log.info("TradeDate:" + TradeUtil.getTradeDateByLDT(ldt));
						ts.setTradeDate(TradeUtil.getTradeDateByLDT(ldt));
						ts.setSecurity(securityStr);
						ts.setFreq(freq.getValue());
						ts.setSecurityName(securityStr);
						ts.setVariety(variety);
						if(!set.contains(ts.getTradeTs())) {
							map.put(ts.getTradeTs(), ts);
						}
					}
				} catch (Exception e) {
					throw new RuntimeException("Consuming file issue:" + line, e);
				}
				log.info("Complete to get data from " + fileName);
			});
		});
		log.info("All data set:" + map.size());
		log.info("Start to insert data");
		
		map.values().stream().forEach(ts -> {
			try {
				timeseriesRepository.save(ts);
			} catch(Exception e) {
				throw new RuntimeException("trade date:" + ts.getTradeDate(), e);
			}
		});
	}

}
