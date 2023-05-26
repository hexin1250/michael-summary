package michael.slf4j.investment.source;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import michael.slf4j.investment.configuration.FreqEnum;
import michael.slf4j.investment.model.Timeseries;
import michael.slf4j.investment.util.TradeUtil;

public interface ISource {
	public String getContent(List<String> securities) throws IOException;
	public String getContent(String security) throws IOException;
	
	public default Timeseries getModel(String security) throws IOException {
		String content = getContent(security);
		return generateModel(security, content);
	}
	
	public default Timeseries generateModel(String security, String content) {
		String[] parts = content.split(",");
		Timeseries m = new Timeseries();
		m.setSecurity(security);
		m.setVariety(security.replaceAll("[\\d]+", ""));
		m.setSecurityName(parts[0]);
		m.setOpen(new BigDecimal(parts[2]));
		m.setHigh(new BigDecimal(parts[3]));
		m.setLow(new BigDecimal(parts[4]));
		m.setClose(new BigDecimal(parts[8]));
		m.setOpenInterest(new BigDecimal(parts[13]));
		m.setVolume(new BigDecimal(parts[14]));
		BigDecimal buy1 = new BigDecimal(parts[6]);
		BigDecimal sell1 = new BigDecimal(parts[7]);
		if (buy1.compareTo(new BigDecimal(0)) == 0) {
			m.setDownLimit(new BigDecimal(parts[8]));
		}
		if (sell1.compareTo(new BigDecimal(0)) == 0) {
			m.setUpLimit(new BigDecimal(parts[8]));
		}
		m.setFreq(FreqEnum._TICK.getValue());
		
		m.setTradeDate(TradeUtil.getDateStr(TradeUtil.getTradeDate()));
		m.setTradeTs(new Timestamp(System.currentTimeMillis()));
		return m;
	}
	
	public default List<Timeseries> getModels(List<String> securities) throws IOException {
		String content = getContent(securities);
		return generateModels(content);
	}
	
	public default List<Timeseries> generateModels(String content) {
		List<Timeseries> ret = new ArrayList<>();
		JSONObject obj = new JSONObject(content);
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
			ret.add(m);
		}
		return ret;
	}
}
