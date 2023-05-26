package michael.slf4j.investment.source;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.sql.Timestamp;
import java.util.List;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;

import michael.slf4j.investment.configuration.FreqEnum;
import michael.slf4j.investment.model.Timeseries;
import michael.slf4j.investment.util.TradeUtil;

public class HttpJsonParser {

	public static void main(String[] args) throws IOException {
		String fileName = "output/result2.json";
		List<String> list = Files.readAllLines(new File(fileName).toPath());
		String str = list.stream().collect(Collectors.joining("\n"));
		
		JSONObject obj = new JSONObject(str);
		JSONArray arr = obj.getJSONArray("Obj");
		for (Object securityObj : arr) {
			JSONObject security = (JSONObject) securityObj;
			
			Timeseries m = new Timeseries();
			m.setSecurity(security.getString("S") + security.getString("C"));
			m.setVariety(security.getString("S"));
			m.setSecurityName(security.getString("N"));
			m.setOpen(security.getBigDecimal("O"));
			m.setHigh(security.getBigDecimal("H"));
			m.setLow(security.getBigDecimal("L"));
			m.setClose(security.getBigDecimal("P"));
			m.setOpenInterest(security.getBigDecimal("HD"));
			m.setVolume(security.getBigDecimal("NV"));
			BigDecimal buy1 = security.getBigDecimal("B1");
			BigDecimal sell1 = security.getBigDecimal("S1");
			if (buy1.compareTo(new BigDecimal(0)) == 0) {
				m.setDownLimit(m.getClose());
			}
			if (sell1.compareTo(new BigDecimal(0)) == 0) {
				m.setUpLimit(m.getClose());
			}
			m.setFreq(FreqEnum._TICK.getValue());
			
			m.setTradeDate(TradeUtil.getDateStr(TradeUtil.getTradeDate()));
			m.setTradeTs(new Timestamp(System.currentTimeMillis()));
			
			System.out.println(m);
		}
	}

}
