package michael.slf4j.investment.quant.formula.impl;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import michael.slf4j.investment.model.Security;
import michael.slf4j.investment.model.Timeseries;
import michael.slf4j.investment.quant.formula.IFormula;
import michael.slf4j.investment.quant.formula.calculator.MACDCalculator;
import michael.slf4j.investment.quant.formula.calculator.MACDCalculator.MACD;

public class MACDFormula implements IFormula<MACD> {
	private int s;
	private int l;
	private int diff;

	public MACDFormula(int s, int l, int diff) {
		this.s = s;
		this.l = l;
		this.diff = diff;
	}

	@Override
	public Map<String, Map<String, MACD>> getModel(Security security, Collection<Timeseries> securityList) {
		throw new UnsupportedOperationException("MACD doesn't support yet");
	}

	@Override
	public Map<String, Map<String, MACD>> getModel(String security, Collection<Timeseries> securityList) {
		Map<String, Map<String, MACD>> tradeDateMap = new TreeMap<>();
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
		MACDCalculator calculator = new MACDCalculator(s, l, diff, emas, emal, dea);
		for (Timeseries model : securityList) {
			MACD macd = calculator.calc(model.getClose());
			tradeDateMap.putIfAbsent(model.getTradeDate(), new TreeMap<>());
			Map<String, MACD> tradeTsMap = tradeDateMap.get(model.getTradeDate());
			tradeTsMap.put(model.getTradeTs().toString(), macd);
		}
		return tradeDateMap;
	}

}
