package michael.slf4j.investment.parse.impl;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

import michael.slf4j.investment.configuration.FreqEnum;
import michael.slf4j.investment.model.Security;
import michael.slf4j.investment.model.Timeseries;
import michael.slf4j.investment.parse.IParser;
import michael.slf4j.investment.util.TradeUtil;

@Component("aliParser")
public class AliParser implements IParser {

	@Override
	public List<Timeseries> parse(String content, FreqEnum freq) {
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
			m.setFreq(freq.getValue());
			
			m.setTradeDate(TradeUtil.getDateStr(TradeUtil.getTradeDate()));
			m.setTradeTs(new Timestamp(System.currentTimeMillis()));
			
			ret.add(m);
		}
		return ret;
	}

	@Override
	public List<Timeseries> parse(Security security, String content, FreqEnum freq) {
		throw new UnsupportedOperationException();
	}

}
