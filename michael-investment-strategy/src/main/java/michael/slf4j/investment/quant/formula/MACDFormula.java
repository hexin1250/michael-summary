package michael.slf4j.investment.quant.formula;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import michael.slf4j.investment.model.Security;
import michael.slf4j.investment.model.Timeseries;
import michael.slf4j.investment.quant.formula.model.MACDCalculator;
import michael.slf4j.investment.repo.TimeseriesRepository;
import michael.slf4j.investment.util.SpringContextUtil;

public class MACDFormula implements IFormula<MACDCalculator> {
	private TimeseriesRepository repo;
	private int s;
	private int l;
	private int diff;
	
	private Map<Security, Map<String, MACDCalculator>> map = new HashMap<>();

	public MACDFormula(int s, int l, int diff) {
		this.repo = SpringContextUtil.getBean("timeseriesRepository", TimeseriesRepository.class);
		this.s = s;
		this.l = l;
		this.diff = diff;
	}

	@Override
	public MACDCalculator getModel(Security security, LocalDate tradeDate) {
		String date = tradeDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
		Map<String, MACDCalculator> tradeDateMap = map.get(security);
		if(tradeDateMap == null) {
			tradeDateMap = new HashMap<>();
			map.put(security, tradeDateMap);
		}
		MACDCalculator ret = tradeDateMap.get(date);
		if(ret == null) {
			String freq = "1D";
			List<Timeseries> list = repo.findBySecurityFreq(security.getName(), freq);
			/**
			 * EMA for short
			 */
			double emas = 0D;
			/**
			 * EMA for long
			 */
			double emal = 0D;
			/**
			 * DEA.
			 */
			double dea = 0D;
			for (Timeseries model : list) {
				MACDCalculator calculator = new MACDCalculator(s, l, diff, emas, emal, dea);
				tradeDateMap.put(model.getTradeDate(), calculator);
				
				double close = model.getClose().doubleValue();
				emas = emas * (s - 1) / (s + 1) + close * 2 / (s + 1);
				emal = emal * (l - 1) / (l + 1) + close * 2 / (l + 1);
				double diffV = emas - emal;
				dea = dea * (diff - 1) / (diff + 1) + diffV * 2 / (diff + 1);
			}
		}
		return tradeDateMap.get(date);
	}

}
