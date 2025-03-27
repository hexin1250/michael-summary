package michael.slf4j.investment.util;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import michael.slf4j.investment.configuration.FreqEnum;
import michael.slf4j.investment.model.Timeseries;

public class DataLoaderUtil {
	public static List<Timeseries> generate30TsListBy15ForRealTime(List<Timeseries> series) {
		List<Timeseries> list = new ArrayList<Timeseries>();
		Timeseries ts = null;
		for (Timeseries min15Ts : series) {
			Timestamp timestamp = min15Ts.getTradeTs();
			LocalDateTime ldt = TradeUtil.getLocalDateTime(timestamp);
			int hour = ldt.getHour();
			int min = ldt.getMinute();
			if(ts == null && min % 30 == 0) {
				continue;
			}
			if(min % 30 == 15) {
				ts = min15Ts.copy();
			}
			if(min % 30 == 0 || (hour == 10 && min == 15)) {
				ts.setClose(min15Ts.getClose());
				ts.setHigh(new BigDecimal(Math.max(ts.getHigh().doubleValue(), min15Ts.getHigh().doubleValue())));
				ts.setLow(new BigDecimal(Math.min(ts.getLow().doubleValue(), min15Ts.getLow().doubleValue())));
				ts.setFreq(FreqEnum._30MI.getValue());
				ts.setTradeTs(min15Ts.getTradeTs());
				ts.setOpenInterest(min15Ts.getOpenInterest());
				if((hour == 10 && min == 15)) {
					LocalDateTime newLdt = ldt.plusMinutes(15);
					ts.setTradeTs(new Timestamp(TradeUtil.getLong(newLdt)));
				} else {
					ts.setVolume(ts.getVolume().add(min15Ts.getVolume()));
				}
				list.add(ts.copy());
				ts = null;
			}
		}
		if(ts != null) {
			ts.setFreq(FreqEnum._30MI.getValue());
			Timestamp timestamp = ts.getTradeTs();
			LocalDateTime ldt = TradeUtil.getLocalDateTime(timestamp);
			LocalDateTime newLdt = ldt.plusMinutes(15);
			ts.setTradeTs(new Timestamp(TradeUtil.getLong(newLdt)));
			list.add(ts);
		}
		return list;
	}
	
	public static List<Timeseries> generate60TsListBy30ForBack(List<Timeseries> series) {
		Map<Timestamp, Timeseries> map = new TreeMap<>();
		for (Timeseries min30Ts : series) {
			Timestamp timestamp = min30Ts.getTradeTs();
			LocalDateTime ldt = TradeUtil.getLocalDateTime(timestamp);
			int hour = ldt.getHour();
			int min = ldt.getMinute();
			if(min == 30) {
				int plusMinute = 30;
				if(hour == 11) {
					plusMinute += 120;
				}
				LocalDateTime newLdt = ldt.plusMinutes(plusMinute);
				timestamp = new Timestamp(TradeUtil.getLong(newLdt));
			}
			Timeseries ts = map.get(timestamp);
			if(ts == null) {
				ts = min30Ts.copy();
				ts.setTradeTs(timestamp);
				map.put(timestamp, ts);
			} else {
				ts.setClose(min30Ts.getClose());
				ts.setVolume(ts.getVolume().add(min30Ts.getVolume()));
				ts.setOpenInterest(min30Ts.getOpenInterest());
				ts.setHigh(new BigDecimal(Math.max(ts.getHigh().doubleValue(), min30Ts.getHigh().doubleValue())));
				ts.setLow(new BigDecimal(Math.min(ts.getLow().doubleValue(), min30Ts.getLow().doubleValue())));
			}
			ts.setFreq(FreqEnum._1H.getValue());
		}
		return map.entrySet().stream().map(entry -> entry.getValue()).collect(Collectors.toList());
	}
	
	public static List<Timeseries> generate2HTsListBy30ForBack(List<Timeseries> series) {
		List<Timeseries> ret = new ArrayList<>();
		Timeseries ts = null;
		for (Timeseries min30Ts : series) {
			if(ts == null) {
				ts = min30Ts.copy();
			} else {
				ts.setClose(min30Ts.getClose());
				ts.setHigh(new BigDecimal(Math.max(ts.getHigh().doubleValue(), min30Ts.getHigh().doubleValue())));
				ts.setLow(new BigDecimal(Math.min(ts.getLow().doubleValue(), min30Ts.getLow().doubleValue())));
				ts.setVolume(ts.getVolume().add(min30Ts.getVolume()));
				ts.setOpenInterest(min30Ts.getOpenInterest());
			}
			Timestamp timestamp = min30Ts.getTradeTs();
			LocalDateTime ldt = TradeUtil.getLocalDateTime(timestamp);
			int hour = ldt.getHour();
			int min = ldt.getMinute();
			if((hour == 23 && min == 0) || (hour == 11 && min == 30) || (hour == 15 && min == 0)) {
				ts.setFreq(FreqEnum._2H.getValue());
				ts.setTradeTs(min30Ts.getTradeTs());
				ret.add(ts.copy());
				ts = null;
			}
		}
		return ret;
	}
	
	public static List<Timeseries> generate1DTsListBy30ForBack(List<Timeseries> series) {
		Map<String, Timeseries> map = new TreeMap<>();
		for (Timeseries min30Ts : series) {
			String tradeDate = min30Ts.getTradeDate();
			Timeseries ts = map.get(tradeDate);
			if(ts == null) {
				ts = min30Ts.copy();
				ts.setFreq(FreqEnum._1D.getValue());
				LocalDate ld = TradeUtil.getTradeDate(ts.getTradeDate());
				LocalTime lt = LocalTime.of(15, 0);
				LocalDateTime ldt = LocalDateTime.of(ld, lt);
				ts.setTradeTs(TradeUtil.getTimestamp(ldt));
				map.put(tradeDate, ts);
				continue;
			}
			ts.setClose(min30Ts.getClose());
			ts.setHigh(new BigDecimal(Math.max(ts.getHigh().doubleValue(), min30Ts.getHigh().doubleValue())));
			ts.setLow(new BigDecimal(Math.min(ts.getLow().doubleValue(), min30Ts.getLow().doubleValue())));
			ts.setVolume(ts.getVolume().add(min30Ts.getVolume()));
			ts.setOpenInterest(min30Ts.getOpenInterest());
		}
		return new ArrayList<>(map.values());
	}
	
	public static List<Timeseries> generate1WTsListBy1D(List<Timeseries> series) {
		Map<LocalDate, Timeseries> map = new TreeMap<>();
		for (Timeseries ts1D : series) {
			LocalDate ld = TradeUtil.getTradeDate(ts1D.getTradeDate());
			int dayOfWeek = ld.getDayOfWeek().getValue();
			ld = ld.plusDays(5 - dayOfWeek);
			
			Timeseries ts = map.get(ld);
			if(ts == null) {
				ts = ts1D.copy();
				ts.setFreq(FreqEnum._1W.getValue());
				LocalTime lt = LocalTime.of(15, 0);
				LocalDateTime ldt = LocalDateTime.of(ld, lt);
				ts.setTradeTs(TradeUtil.getTimestamp(ldt));
				map.put(ld, ts);
				continue;
			}
			ts.setClose(ts1D.getClose());
			ts.setHigh(new BigDecimal(Math.max(ts.getHigh().doubleValue(), ts1D.getHigh().doubleValue())));
			ts.setLow(new BigDecimal(Math.min(ts.getLow().doubleValue(), ts1D.getLow().doubleValue())));
			ts.setVolume(ts.getVolume().add(ts1D.getVolume()));
			ts.setOpenInterest(ts1D.getOpenInterest());
		}
		return new ArrayList<>(map.values());
	}

}
