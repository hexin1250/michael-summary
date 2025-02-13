package michael.slf4j.investment.etl;

import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.jms.JMSException;

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

@Component("dataResearch")
public class DataResearch {
	private static final Logger log = Logger.getLogger(DataResearch.class);
	
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
	
	public void summarize() {
		Variety variety = Variety.RB;
		List<String> lastTradeDates = timeseriesRepository.getLast2TradeDate(variety.name(), FreqEnum._1MI.getValue());
		List<String> securityList = timeseriesRepository.getSecurityList(variety.name(), lastTradeDates.get(0));
		String mainSecurity = null;
		double maxOpenInterest = 0D;
		for (String security : securityList) {
			Double openInterest = timeseriesRepository.getLastOpenInterest(security);
			if(mainSecurity == null) {
				mainSecurity = security;
				maxOpenInterest = openInterest;
			}
			if(openInterest > maxOpenInterest) {
				mainSecurity = security;
				maxOpenInterest = openInterest;
			}
		}
		
		String freqStr = FreqEnum._15MI.getValue();
		List<Timeseries> historyList = timeseriesRepository.getAllDataByPeriod(mainSecurity, lastTradeDates.get(1), freqStr);
		List<Timeseries> realTimeList = timeseriesRepository.getDataByPeriod(mainSecurity, lastTradeDates.get(0), freqStr);
		summarizeDataByFreq(freqStr, historyList, realTimeList);
		generate30MinSummary(FreqEnum._30MI.getValue(), historyList, realTimeList);
		generate1DSummary(FreqEnum._1D.getValue(), mainSecurity, lastTradeDates);
	}
	
	private void generate1DSummary(String freqStr, String mainSecurity, List<String> lastTradeDates) {
		List<Timeseries> historyList = timeseriesRepository.getAllDataByPeriod(mainSecurity, lastTradeDates.get(1), freqStr);
		List<Timeseries> realTimeList = timeseriesRepository.getDataByPeriod(mainSecurity, lastTradeDates.get(0), freqStr);
		summarizeDataByFreq(freqStr, historyList, realTimeList);
	}

	private void generate30MinSummary(String freqStr, List<Timeseries> historyList, List<Timeseries> realTimeList) {
		List<Timeseries> history30List = DataLoaderUtil.generate30TsListBy15ForBack(historyList);
		List<Timeseries> realTime30List = DataLoaderUtil.generate30TsListBy15ForBack(realTimeList);
		summarizeDataByFreq(freqStr, history30List, realTime30List);
		generate60MinSummary(FreqEnum._1H.getValue(), history30List, realTime30List);
	}
	
	private void generate60MinSummary(String freqStr, List<Timeseries> historyList, List<Timeseries> realTimeList) {
		List<Timeseries> history60List = DataLoaderUtil.generate60TsListBy30ForBack(historyList);
		List<Timeseries> realTime60List = DataLoaderUtil.generate60TsListBy30ForBack(realTimeList);
		summarizeDataByFreq(freqStr, history60List, realTime60List);
	}

	private void summarizeDataByFreq(String freqStr, List<Timeseries> historyList, List<Timeseries> realTimeList) {
		List<Double> highs = new ArrayList<>();
		List<Double> closes = new ArrayList<>();
		List<Double> lows = new ArrayList<>();
		for (Timeseries ts : historyList) {
			highs.add(ts.getHigh().doubleValue());
			lows.add(ts.getLow().doubleValue());
			closes.add(ts.getClose().doubleValue());
		}
		summarizeDataByFreq(freqStr, realTimeList, highs, closes, lows);
	}

	private void summarizeDataByFreq(String freqStr, List<Timeseries> realTimeTsList, List<Double> highs,
			List<Double> closes, List<Double> lows) {
		String ret = null;
		for (Timeseries ts : realTimeTsList) {
			StringBuffer sb = new StringBuffer();
			highs.add(ts.getHigh().doubleValue());
			lows.add(ts.getLow().doubleValue());
			closes.add(ts.getClose().doubleValue());
			sb.append("以下为在").append(ts.getTradeTs()).append("时，").append(freqStr).append("周期的指标数据：").append("\n");
			sb.append("开盘价:").append(nf.format(ts.getOpen())).append("\n");
			sb.append("最高价:").append(nf.format(ts.getHigh())).append("\n");
			sb.append("最低价:").append(nf.format(ts.getLow())).append("\n");
			sb.append("收盘价:").append(nf.format(ts.getClose())).append("\n");
			sb.append("持仓:").append(nf.format(ts.getOpenInterest())).append("\n");
			sb.append("成交量:").append(nf.format(ts.getVolume())).append("\n");
			Map<String, List<Double>> mas = IndicatorUtils.calculateMA(closes);
			for (Entry<String, List<Double>> entry : mas.entrySet()) {
				double value = entry.getValue().get(entry.getValue().size() - 1);
				sb.append(entry.getKey()).append(":").append(nf.format(value));
				sb.append("\n");
			}
			
			sb.append("BOLL(26,2)");
			Map<String, List<Double>> boll = IndicatorUtils.calculateBOLL(closes, 26, 2);
			for (Entry<String, List<Double>> entry : boll.entrySet()) {
				double value = entry.getValue().get(entry.getValue().size() - 1);
				sb.append(" ");
				sb.append(entry.getKey()).append(":").append(nf.format(value));
			}
			sb.append("\n");
			
			sb.append("BIAS(6,12,24)");
			Map<String, List<Double>> bias = IndicatorUtils.calculateBIAS(closes);
			for (Entry<String, List<Double>> entry : bias.entrySet()) {
				double value = entry.getValue().get(entry.getValue().size() - 1);
				sb.append(" ");
				sb.append(entry.getKey()).append(":").append(nf.format(value));
			}
			sb.append("\n");
			
			sb.append("WR(6,10,-80,-20)");
			Map<String, List<Double>> wr = IndicatorUtils.calculateWR(highs, lows, closes);
			for (Entry<String, List<Double>> entry : wr.entrySet()) {
				double value = entry.getValue().get(entry.getValue().size() - 1);
				sb.append(" ");
				sb.append(entry.getKey()).append(":").append(nf.format(value * -1));
			}
			sb.append("\n");
			
			sb.append("ATR(15)");
			Map<String, List<Double>> atr = IndicatorUtils.calculateATR(highs, lows, closes, 15);
			for (Entry<String, List<Double>> entry : atr.entrySet()) {
				double value = entry.getValue().get(entry.getValue().size() - 1);
				sb.append(" ");
				sb.append(entry.getKey()).append(":").append(nf.format(value));
			}
			sb.append("\n");
			ret = sb.toString();
		}
		try {
			messageService.send(TopicConstants.NOTIFICATION_TOPIC, ret);
		} catch (JMSException e) {
			log.error("Error when sending message to topic", e);
		}
	}

}
