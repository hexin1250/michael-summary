package michael.slf4j.investment.parse.impl;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.springframework.stereotype.Component;

import michael.slf4j.investment.configuration.FreqEnum;
import michael.slf4j.investment.model.Security;
import michael.slf4j.investment.model.Timeseries;
import michael.slf4j.investment.parse.IParser;

@Component("binanceParser")
public class BinanceParser implements IParser {
	private static final String DATE_PATTERN = "yyyy-MM-dd";
	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(DATE_PATTERN);

	@Override
	public List<Timeseries> parse(String content, FreqEnum freq) {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<Timeseries> parse(Security security, String content, FreqEnum freq) {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<Timeseries> parseAll(Security security, String content, FreqEnum freq) {
		List<Timeseries> ret = new ArrayList<>();
		JSONArray arr = new JSONArray(content);
		for (Object obj : arr) {
			JSONArray recordJsonArr = (JSONArray) obj;
			
			int IDX_OPEN_TIME = 0;   // 开盘时间（毫秒）
			int IDX_OPEN      = 1;   // 开盘价
	        int IDX_HIGH      = 2;   // 最高价
	        int IDX_LOW       = 3;   // 最低价
	        int IDX_CLOSE     = 4;   // 收盘价
	        int IDX_VOLUME    = 5;   // 成交量（基础资产）

	        long openTimeMillis = recordJsonArr.optLong(IDX_OPEN_TIME, 0L);
	        String openStr   = recordJsonArr.optString(IDX_OPEN, "0");
	        String highStr   = recordJsonArr.optString(IDX_HIGH, "0");
	        String lowStr    = recordJsonArr.optString(IDX_LOW, "0");
	        String closeStr  = recordJsonArr.optString(IDX_CLOSE, "0");
	        String volumeStr = recordJsonArr.optString(IDX_VOLUME, "0");

	        String tradeDate = formatDate(openTimeMillis);
	        BigDecimal open   = new BigDecimal(openStr);
	        BigDecimal high   = new BigDecimal(highStr);
	        BigDecimal low    = new BigDecimal(lowStr);
	        BigDecimal close  = new BigDecimal(closeStr);
	        BigDecimal volume = new BigDecimal(volumeStr);

	        Timestamp tradeTs = new Timestamp(openTimeMillis);

	        Timeseries ts = new Timeseries();
	        ts.setOpen(open);
	        ts.setHigh(high);
	        ts.setLow(low);
	        ts.setClose(close);
	        ts.setVolume(volume);
	        ts.setTradeTs(tradeTs);
	        ts.setFreq(freq.getValue());
	        ts.setSecurity(security.getName());
	        ts.setVariety(security.getVariety().name());
	        ts.setSecurityName(security.getName());
	        ts.setTradeDate(tradeDate);
	        
	        ret.add(ts);
		}
		return ret;
	}
	
	private static String formatDate(long millis) {
        Instant instant = Instant.ofEpochMilli(millis);
        LocalDate date = instant.atZone(ZoneOffset.UTC).toLocalDate();
        return date.format(DATE_FORMATTER);
    }

}
