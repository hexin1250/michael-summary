package michael.slf4j.investment.parse.impl;
import java.sql.Timestamp;
import java.time.LocalDateTime;
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

@Component("aliHistoricalParser")
public class HistoricalParser implements IParser {

	@Override
	public List<Timeseries> parse(String content, FreqEnum freq) {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<Timeseries> parse(Security security, String content, FreqEnum freq) {
		List<Timeseries> ret = new ArrayList<>();
		JSONObject obj = new JSONObject(content);
		JSONArray arr = obj.getJSONArray("Obj");
		for (Object dataObj : arr) {
			JSONObject data = (JSONObject) dataObj;
			Timeseries m = new Timeseries();
			
			m.setSecurity(security.getName());
			m.setVariety(security.getVariety().name());
			m.setSecurityName(security.getName());
			m.setOpen(data.getBigDecimal("O"));
			m.setHigh(data.getBigDecimal("H"));
			m.setLow(data.getBigDecimal("L"));
			m.setClose(data.getBigDecimal("C"));
			m.setOpenInterest(data.getBigDecimal("A"));
			m.setVolume(data.getBigDecimal("V"));
			m.setFreq(freq.getValue());
			
			m.setTradeDate(TradeUtil.getDateStr(TradeUtil.getTradeDate(new java.util.Date(data.getLong("Tick") * 1000L))));
			m.setTradeTs(new Timestamp(data.getLong("Tick") * 1000L));
			
			LocalDateTime ldt = TradeUtil.getLocalDateTime(m.getTradeTs());
			if(ldt.getMinute() % 15 == 0) {
				ret.add(m);
			}
		}
		return ret;
	}

}
