package michael.slf4j.investment.research;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.text.NumberFormat;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import michael.slf4j.investment.configuration.FreqEnum;
import michael.slf4j.investment.constant.TopicConstants;
import michael.slf4j.investment.message.service.MessageService;
import michael.slf4j.investment.model.Timeseries;
import michael.slf4j.investment.model.Variety;
import michael.slf4j.investment.repo.TimeseriesRepository;
import michael.slf4j.investment.util.DataLoaderUtil;
import michael.slf4j.investment.util.IndicatorUtils;
import michael.slf4j.investment.util.PositionFileUtil;
import michael.slf4j.investment.util.StockIndicatorCalculator;
import michael.slf4j.investment.util.StockIndicatorCalculator.RSIResult;
import michael.slf4j.investment.util.StockIndicatorCalculator.StockData;
import michael.slf4j.investment.util.TechnicalIndicator;
import michael.slf4j.investment.util.TechnicalIndicator.KDJResult;
import michael.slf4j.investment.util.TechnicalIndicator.MACDResult;
import michael.slf4j.investment.util.TradeUtil;

@Component("dataResearch")
public class DataResearch {
	private static final Logger log = Logger.getLogger(DataResearch.class);
	private static final Map<String, String> HEADER_MAP = new LinkedHashMap<String, String>();
	
	static {
		HEADER_MAP.put("time", "时间");
		HEADER_MAP.put("freq", "周期");
		HEADER_MAP.put("open", "开盘价");
		HEADER_MAP.put("high", "最高价");
		HEADER_MAP.put("low", "最低价");
		HEADER_MAP.put("close", "收盘价");
		HEADER_MAP.put("OI", "OI");
		HEADER_MAP.put("VOLUME", "VOLUME");
		HEADER_MAP.put("MA1", "MA5");
		HEADER_MAP.put("MA2", "MA10");
		HEADER_MAP.put("MA3", "MA20");
		HEADER_MAP.put("MA4", "MA40");
		HEADER_MAP.put("MA5", "MA60");
		HEADER_MAP.put("BOLL LOWER", "BOLL(26,2) LOWER");
		HEADER_MAP.put("BOLL MID", "BOLL(26,2) MID");
		HEADER_MAP.put("BOLL UPPER", "BOLL(26,2) UPPER");
		HEADER_MAP.put("EMA", "EMA(10)");
		HEADER_MAP.put("BIAS1", "BIAS(6,12,24) BIAS1");
		HEADER_MAP.put("BIAS2", "BIAS(6,12,24) BIAS2");
		HEADER_MAP.put("BIAS3", "BIAS(6,12,24) BIAS3");
		HEADER_MAP.put("WR1", "WR(10,6,-80,-20) WR1");
		HEADER_MAP.put("WR2", "WR(10,6,-80,-20) WR2");
		HEADER_MAP.put("TR", "ATR(15) TR");
		HEADER_MAP.put("ATR", "ATR(15) ATR");
		HEADER_MAP.put("CCI", "CCI(14)");
		HEADER_MAP.put("ENE LOWER", "ENE(10,11,9) LOWER");
		HEADER_MAP.put("ENE ENE", "ENE(10,11,9) ENE");
		HEADER_MAP.put("ENE UPPER", "ENE(10,11,9) UPPER");
		HEADER_MAP.put("MFI", "MFI(14)");
		HEADER_MAP.put("MACD DIFF", "MACD(12,26,9) DIFF");
		HEADER_MAP.put("MACD DEA", "MACD(12,26,9) DEA");
		HEADER_MAP.put("MACD MACD", "MACD(12,26,9) MACD");
		HEADER_MAP.put("K", "KDJ(9,3,3) K");
		HEADER_MAP.put("D", "KDJ(9,3,3) D");
		HEADER_MAP.put("J", "KDJ(9,3,3) J");
		HEADER_MAP.put("RSI1", "RSI(6,12,24) RSI1");
		HEADER_MAP.put("RSI2", "RSI(6,12,24) RSI2");
		HEADER_MAP.put("RSI3", "RSI(6,12,24) RSI3");

	}

	private NumberFormat nf;

	public DataResearch() {
		this.nf = NumberFormat.getInstance();
		nf.setMaximumFractionDigits(2);
		nf.setRoundingMode(RoundingMode.HALF_UP);
		nf.setGroupingUsed(false);
	}

	@Autowired
	private TimeseriesRepository timeseriesRepository;

	@Autowired
	MessageService messageService;

	public void summarize(boolean full, boolean isSendMessage) {
		summarize(LocalDateTime.now(), full, isSendMessage);
	}
	
	public void summarize(LocalDateTime current, boolean full, boolean isSendMessage) {
		Variety variety = Variety.RB;
		Timestamp ts = TradeUtil.getTimestamp(current);
		List<String> lastTradeDates = timeseriesRepository.getLast2TradeDate(variety.name(), FreqEnum._1MI.getValue(), ts);
		String tTradeDate = lastTradeDates.get(0);
		String tMinus1TradeDate = lastTradeDates.get(1);
		String tMinus2TradeDate = lastTradeDates.get(2);
		List<String> securityList = timeseriesRepository.getSecurityList(variety.name(), lastTradeDates.get(0));
		String mainSecurity = null;
		double maxOpenInterest = 0D;
		for (String security : securityList) {
			Double openInterest = timeseriesRepository.getLastOpenInterest(security, ts);
			if (mainSecurity == null) {
				mainSecurity = security;
				maxOpenInterest = openInterest;
			}
			if (openInterest > maxOpenInterest) {
				mainSecurity = security;
				maxOpenInterest = openInterest;
			}
		}

		FreqEnum freq = FreqEnum._15MI;
		List<StringBuffer> formatList = new ArrayList<StringBuffer>();
		StringBuffer sb = new StringBuffer();
		sb.append("|");
		sb.append(HEADER_MAP.values().stream().collect(Collectors.joining("|")));
		sb.append("|");
		sb.append("\n");
		formatList.add(sb);
		
		String previousTradeDate = null;
		String currentTradeDate = null;
		if(full) {
			previousTradeDate = tMinus2TradeDate;
			currentTradeDate = tMinus1TradeDate;
		} else {
			previousTradeDate = tMinus1TradeDate;
			currentTradeDate = tTradeDate;
		}
		List<Timeseries> historyList = timeseriesRepository.getAllDataByPeriod(mainSecurity, previousTradeDate, freq.getValue());
		List<Timeseries> realTimeList = timeseriesRepository.getDataByPeriod(mainSecurity, currentTradeDate, freq.getValue());
		summarizeDataByFreq(formatList, freq, current, historyList, realTimeList, full);
		generate30MinSummary(formatList, FreqEnum._30MI, current, historyList, realTimeList, full);
		if (current.getHour() >= 15 && current.getHour() <= 20) {
			generate1DSummary(formatList, FreqEnum._1D, current, mainSecurity, lastTradeDates, full);
		}
		if(!full) {
			generateTendency(formatList, mainSecurity, currentTradeDate, current, full);
		}
		generateTrail(formatList, mainSecurity, current, realTimeList.get(0), realTimeList.get(realTimeList.size() - 1), full);
		
		String fileName = "C:\\Users\\HP\\python-workspace\\myproject\\data\\test.txt";
		try(BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileName)))) {
			for (StringBuffer strb : formatList) {
				bw.write(strb.toString());
				if(isSendMessage) {
					messageService.send(TopicConstants.NOTIFICATION_TOPIC, strb.toString());
				}
			}
		} catch (Exception e) {
			log.error("Error when sending message to topic", e);
		}
	}

	private void generateTrail(List<StringBuffer> formatList, String mainSecurity,
			LocalDateTime current, Timeseries firstTs, Timeseries lastTs, boolean full) {
		StringBuffer sb = new StringBuffer();
		sb.append("现在时间是").append(TradeUtil.getTimestamp(current)).append(",").append("已经收盘,收盘点位").append(lastTs.getClose().intValue()).append("\n");
		sb.append("当前文本中包括了从").append(firstTs.getTradeTs()).append("到当前时间下,不同周期指标的数据.");
		if(!full) {
			sb.append("其中第二个表格是持仓量的价格的记录,注意持仓量的变化情况");
		}
		sb.append("\n");
		Map<String, String> map = PositionFileUtil.readPositionData();
		if(!map.isEmpty()) {
			sb.append("目前持有").append(map.get(PositionFileUtil.DIRECTION)).append(",");
			sb.append("成本").append(map.get(PositionFileUtil.PRICE)).append(",");
			sb.append("仓位").append(map.get(PositionFileUtil.POSITION_PER)).append("%").append(".");
			sb.append("注意仓位方向");
		} else {
			sb.append("目前空仓");
		}
		sb.append("\n");
		if(current.getHour() >= 23 || current.getHour() <= 8
				|| (current.getDayOfWeek() == DayOfWeek.SATURDAY || current.getDayOfWeek() == DayOfWeek.SUNDAY)) {
			sb.append("请根据当前时间的(15M,30M,1H,2H)周期的所有数据指标以及过往的趋势,分析螺纹钢期货");
			sb.append("(OI,VOLUME,MA5,MA10,MA20,MA40,MA60,BOLL(26,2) LOWER,BOLL(26,2) MID,BOLL(26,2) UPPER,EMA(10),BIAS(6,12,24) BIAS1,BIAS(6,12,24) BIAS2,BIAS(6,12,24) BIAS3,WR(10,6,-80,-20) WR1,WR(10,6,-80,-20) WR2,ATR(15) TR,ATR(15) ATR,CCI(14),ENE(10,11,9) LOWER,ENE(10,11,9) ENE,ENE(10,11,9) UPPER,MFI(14),MACD(12,26,9) DIFF,MACD(12,26,9) DEA,MACD(12,26,9) MACD,KDJ(9,3,3) K,KDJ(9,3,3) D,KDJ(9,3,3) J,RSI(6,12,24) RSI1,RSI(6,12,24) RSI2,RSI(6,12,24) RSI3)");
			sb.append("以及过往的趋势,分析螺纹钢期货");
			sb.append(mainSecurity);
			sb.append("日盘");
		} else if(current.getHour() >= 9 && current.getHour() <= 12) {
			sb.append("请根据当前时间的(15M,30M,1H,2H)周期的所有数据指标以及过往的趋势,分析螺纹钢期货");
			sb.append("(OI,VOLUME,MA5,MA10,MA20,MA40,MA60,BOLL(26,2) LOWER,BOLL(26,2) MID,BOLL(26,2) UPPER,EMA(10),BIAS(6,12,24) BIAS1,BIAS(6,12,24) BIAS2,BIAS(6,12,24) BIAS3,WR(10,6,-80,-20) WR1,WR(10,6,-80,-20) WR2,ATR(15) TR,ATR(15) ATR,CCI(14),ENE(10,11,9) LOWER,ENE(10,11,9) ENE,ENE(10,11,9) UPPER,MFI(14),MACD(12,26,9) DIFF,MACD(12,26,9) DEA,MACD(12,26,9) MACD,KDJ(9,3,3) K,KDJ(9,3,3) D,KDJ(9,3,3) J,RSI(6,12,24) RSI1,RSI(6,12,24) RSI2,RSI(6,12,24) RSI3)");
			sb.append("以及过往的趋势,分析螺纹钢期货");
			sb.append(mainSecurity);
			sb.append("下午日盘");
		} else if(current.getHour() >= 15 && current.getHour() <= 20) {
			sb.append("请根据当前时间的(15M,30M,1H,2H,1D)周期的所有数据指标以及过往的趋势,分析螺纹钢期货");
			sb.append("(OI,VOLUME,MA5,MA10,MA20,MA40,MA60,BOLL(26,2) LOWER,BOLL(26,2) MID,BOLL(26,2) UPPER,EMA(10),BIAS(6,12,24) BIAS1,BIAS(6,12,24) BIAS2,BIAS(6,12,24) BIAS3,WR(10,6,-80,-20) WR1,WR(10,6,-80,-20) WR2,ATR(15) TR,ATR(15) ATR,CCI(14),ENE(10,11,9) LOWER,ENE(10,11,9) ENE,ENE(10,11,9) UPPER,MFI(14),MACD(12,26,9) DIFF,MACD(12,26,9) DEA,MACD(12,26,9) MACD,KDJ(9,3,3) K,KDJ(9,3,3) D,KDJ(9,3,3) J,RSI(6,12,24) RSI1,RSI(6,12,24) RSI2,RSI(6,12,24) RSI3)");
			sb.append("以及过往的趋势,分析螺纹钢期货");
			sb.append(mainSecurity);
			sb.append("下一个交易日的夜盘和日盘");
		}
		sb.append("的走势预演,和对应的概率,和关键价位预判.基于当前持仓指定执行策略,以及反手条件.");
		sb.append("分析指标的时候,需标注对应的周期.").append("\n");
		sb.append("注意:在分析过程中,要分析全部技术指标(请仔细检查).在结果展示中,至少包括以下5几点:多周期技术面共振分析,关键价位预判,主力持仓行为解析,日内走势预演,日内交易策略");
		sb.append("\n");
		formatList.add(sb);
	}

	private void generateTendency(List<StringBuffer> formatList, String mainSecurity, String currentTradeDate, LocalDateTime current, boolean full) {
		List<Timeseries> realTimeList = timeseriesRepository.getDataByPeriod(mainSecurity, currentTradeDate, FreqEnum._1MI.getValue());
		formatList.get(formatList.size() - 1).append("\n").append("时间,价格,持仓量").append("\n");
		for (Timeseries ts : realTimeList) {
			boolean accept = pickupData(current, ts, full);
			if (!accept) {
				continue;
			}
			StringBuffer formatSb = new StringBuffer();
			formatSb.append(ts.getTradeTs()).append(",").append(ts.getClose().intValue()).append(",").append(ts.getOpenInterest().intValue()).append("\n");
//			LocalDateTime ldt = TradeUtil.getLocalDateTime(ts.getTradeTs());
//			formatSb.append(ldt.getHour()).append(":").append(ldt.getMinute()).append(",").append(ts.getClose().intValue()).append(",").append(ts.getOpenInterest().intValue()).append("\n");
			
			StringBuffer currentSb = formatList.get(formatList.size() - 1);
			if(currentSb.length() + formatSb.length() <= 1900) {
				currentSb.append(formatSb);
			} else {
				StringBuffer newSb = new StringBuffer();
				newSb.append(formatSb);
				formatList.add(newSb);
			}
		}
	}

	private void generate1DSummary(List<StringBuffer> formatList, FreqEnum freq, LocalDateTime current, String mainSecurity, List<String> lastTradeDates, boolean full) {
		List<Timeseries> historyList = timeseriesRepository.getAllDataByPeriod(mainSecurity, lastTradeDates.get(1),
				freq.getValue());
		List<Timeseries> realTimeList = timeseriesRepository.getDataByPeriod(mainSecurity, lastTradeDates.get(0),
				freq.getValue());
		summarizeDataByFreq(formatList, freq, current, historyList, realTimeList, full);
	}

	private void generate30MinSummary(List<StringBuffer> formatList, FreqEnum freq, LocalDateTime current, List<Timeseries> historyList, List<Timeseries> realTimeList, boolean full) {
		List<Timeseries> history30List = DataLoaderUtil.generate30TsListBy15ForRealTime(historyList);
		List<Timeseries> realTime30List = DataLoaderUtil.generate30TsListBy15ForRealTime(realTimeList);
		summarizeDataByFreq(formatList, freq, current, history30List, realTime30List, full);
		generate1HSummary(formatList, FreqEnum._1H, current, history30List, realTime30List, full);
		generate2HSummary(formatList, FreqEnum._2H, current, history30List, realTime30List, full);
	}

	private void generate1HSummary(List<StringBuffer> formatList, FreqEnum freq, LocalDateTime current, List<Timeseries> historyList, List<Timeseries> realTimeList, boolean full) {
		List<Timeseries> history60List = DataLoaderUtil.generate60TsListBy30ForBack(historyList);
		List<Timeseries> realTime60List = DataLoaderUtil.generate60TsListBy30ForBack(realTimeList);
		summarizeDataByFreq(formatList, freq, current, history60List, realTime60List, full);
	}

	private void generate2HSummary(List<StringBuffer> formatList, FreqEnum freq, LocalDateTime current, List<Timeseries> historyList, List<Timeseries> realTimeList, boolean full) {
		List<Timeseries> history2HList = DataLoaderUtil.generate2HTsListBy30ForBack(historyList);
		List<Timeseries> realTime2HList = DataLoaderUtil.generate2HTsListBy30ForBack(realTimeList);
		summarizeDataByFreq(formatList, freq, current, history2HList, realTime2HList, full);
	}

	private void summarizeDataByFreq(List<StringBuffer> formatList, FreqEnum freq, LocalDateTime current, List<Timeseries> historyList, List<Timeseries> realTimeList, boolean full) {
		List<Double> opens = new ArrayList<>();
		List<Double> highs = new ArrayList<>();
		List<Double> lows = new ArrayList<>();
		List<Double> closes = new ArrayList<>();
		List<Double> volumes = new ArrayList<>();
		List<StockData> dataList = new ArrayList<>();
		for (Timeseries ts : historyList) {
			opens.add(ts.getOpen().doubleValue());
			highs.add(ts.getHigh().doubleValue());
			lows.add(ts.getLow().doubleValue());
			closes.add(ts.getClose().doubleValue());
			volumes.add(ts.getVolume().doubleValue());
			double preClose = 0D;
			if (!dataList.isEmpty()) {
				preClose = dataList.get(dataList.size() - 1).getClose();
			}
			dataList.add(new StockData(ts.getOpen().doubleValue(), ts.getHigh().doubleValue(),
					ts.getLow().doubleValue(), ts.getClose().doubleValue(), ts.getVolume().doubleValue(), preClose));
		}
		summarizeDataByFreq(formatList, freq, current, realTimeList, opens, highs, closes, lows, volumes, dataList, full);
	}

	private void summarizeDataByFreq(List<StringBuffer> formatList, FreqEnum freq, LocalDateTime current, List<Timeseries> realTimeTsList, List<Double> opens,
			List<Double> highs, List<Double> closes, List<Double> lows, List<Double> volumes,
			List<StockData> dataList, boolean full) {
		for (Timeseries ts : realTimeTsList) {
			opens.add(ts.getOpen().doubleValue());
			highs.add(ts.getHigh().doubleValue());
			lows.add(ts.getLow().doubleValue());
			closes.add(ts.getClose().doubleValue());
			volumes.add(ts.getVolume().doubleValue());
			double preClose = 0D;
			if (!dataList.isEmpty()) {
				preClose = dataList.get(dataList.size() - 1).getClose();
			}
			dataList.add(new StockData(ts.getOpen().doubleValue(), ts.getHigh().doubleValue(),
					ts.getLow().doubleValue(), ts.getClose().doubleValue(), ts.getVolume().doubleValue(), preClose));
			boolean accept = pickupData(current, ts, full);
			if (!accept) {
				continue;
			}
			Map<String, String> dataMap = new LinkedHashMap<String, String>();

			Timestamp end = ts.getTradeTs();
			Timestamp start = new Timestamp(end.getTime() - freq.getPeriod() * 60L * 1000L);
			LocalDateTime ldt = TradeUtil.getLocalDateTime(end);
			if(freq == FreqEnum._1H && ldt.getHour() == 14) {
				start = new Timestamp(end.getTime() - (3 * 60 - 1) * 60L * 1000L);
			} else if(freq == FreqEnum._2H) {
				if(ldt.getHour() == 11) {
					start = new Timestamp(end.getTime() - (150 - 1) * 60L * 1000L);
				} else if(ldt.getHour() == 15) {
					start = new Timestamp(end.getTime() - (90 - 1) * 60L * 1000L);
				}
			}
			StringBuffer timeSb = new StringBuffer();
			timeSb.append(start).append(" - ").append(end);
			dataMap.put("time", timeSb.toString());
			dataMap.put("freq", freq.getValue());
			dataMap.put("open", nf.format(ts.getOpen()));
			dataMap.put("high", nf.format(ts.getHigh()));
			dataMap.put("low", nf.format(ts.getLow()));
			dataMap.put("close", nf.format(ts.getClose()));
			dataMap.put("OI", nf.format(ts.getOpenInterest()));
			dataMap.put("VOLUME", nf.format(ts.getVolume()));
			Map<String, List<Double>> mas = IndicatorUtils.calculateMA(closes);
			for (Entry<String, List<Double>> entry : mas.entrySet()) {
				double value = entry.getValue().get(entry.getValue().size() - 1);
				dataMap.put(entry.getKey(), nf.format(value));
			}

			Map<String, List<Double>> boll = IndicatorUtils.calculateBOLL(closes, 26, 2);
			for (Entry<String, List<Double>> entry : boll.entrySet()) {
				double value = entry.getValue().get(entry.getValue().size() - 1);
				dataMap.put(entry.getKey(), nf.format(value));
			}

			double ema10 = IndicatorUtils.calculateEMA(closes, 10);
			dataMap.put("EMA", nf.format(ema10));

			Map<String, List<Double>> bias = IndicatorUtils.calculateBIAS(closes);
			for (Entry<String, List<Double>> entry : bias.entrySet()) {
				double value = entry.getValue().get(entry.getValue().size() - 1);
				dataMap.put(entry.getKey(), nf.format(value));
			}

			Map<String, List<Double>> wr = IndicatorUtils.calculateWR(highs, lows, closes);
			for (Entry<String, List<Double>> entry : wr.entrySet()) {
				double value = entry.getValue().get(entry.getValue().size() - 1);
				dataMap.put(entry.getKey(), nf.format(value * -1));
			}

			Map<String, List<Double>> atr = IndicatorUtils.calculateATR(highs, lows, closes, 15);
			for (Entry<String, List<Double>> entry : atr.entrySet()) {
				double value = entry.getValue().get(entry.getValue().size() - 1);
				dataMap.put(entry.getKey(), nf.format(value));
			}
			List<Double> cci = IndicatorUtils.calculateCCI(highs, lows, closes, 14);
			dataMap.put("CCI", nf.format(cci.get(cci.size() - 1)));

			Map<String, List<Double>> ene = IndicatorUtils.calculateENE_10_11_9(closes);
			for (Entry<String, List<Double>> entry : ene.entrySet()) {
				double value = entry.getValue().get(entry.getValue().size() - 1);
				dataMap.put(entry.getKey(), nf.format(value));
			}

			double mfi14 = IndicatorUtils.calculateMFI(highs, lows, closes, volumes, 14);
			dataMap.put("MFI", nf.format(mfi14));

			MACDResult macd = TechnicalIndicator.calculateMACD(closes);
			dataMap.put("MACD DIFF", nf.format(macd.dif));
			dataMap.put("MACD DEA", nf.format(macd.dea));
			dataMap.put("MACD MACD", nf.format(macd.macd));

			KDJResult kdj = TechnicalIndicator.calculateKDJ(highs, lows, closes);
			dataMap.put("K", nf.format(kdj.k));
			dataMap.put("D", nf.format(kdj.d));
			dataMap.put("J", nf.format(kdj.j));
			
			RSIResult rsi = StockIndicatorCalculator.calculateRSI(dataList);
			dataMap.put("RSI1", nf.format(rsi.getRsi6()));
			dataMap.put("RSI2", nf.format(rsi.getRsi12()));
			dataMap.put("RSI3", nf.format(rsi.getRsi24()));
			
			StringBuffer dataSb = new StringBuffer();
			dataSb.append("|");
			dataSb.append(HEADER_MAP.keySet().stream().map(key -> dataMap.get(key)).collect(Collectors.joining("|")));
			dataSb.append("|");
			dataSb.append('\n');
			
			StringBuffer currentSb = formatList.get(formatList.size() - 1);
			if(currentSb.length() + dataSb.length() <= 1900) {
				currentSb.append(dataSb);
			} else {
				StringBuffer newSb = new StringBuffer();
				newSb.append(dataSb);
				formatList.add(newSb);
			}
		}
	}

	private boolean pickupData(LocalDateTime current, Timeseries ts, boolean full) {
		if(full) {
			return true;
		}
		LocalDateTime lt = TradeUtil.getLocalDateTime(ts.getTradeTs());
		boolean accept = false;
		if (lt.getHour() >= 21 && (current.getHour() <= 8 || current.getHour() >= 21)) {
			accept = true;
		} else if ((lt.getHour() >= 9 && lt.getHour() <= 12)
				&& (current.getHour() <= 12 || (current.getHour() == 13 && current.getMinute() < 30))) {
			accept = true;
		} else if ((lt.getHour() >= 13 && lt.getHour() <= 15) && current.getHour() <= 20) {
			accept = true;
		}
		return accept;
	}

}
