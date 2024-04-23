package michael.slf4j.investment.quant.formula.impl;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import michael.slf4j.investment.model.Security;
import michael.slf4j.investment.model.Timeseries;
import michael.slf4j.investment.quant.formula.IFormula;
import michael.slf4j.investment.quant.formula.calculator.KDJCalculator;
import michael.slf4j.investment.quant.formula.calculator.KDJCalculator.KDJ;

public class KDJFormula implements IFormula<KDJ> {
	private int n;
	private int kCycle;
	private int dCycle;
	
	public KDJFormula(int n, int kCycle, int dCycle) {
		this.n = n;
		this.kCycle = kCycle;
		this.dCycle = dCycle;
	}

	@Override
	public Map<String, Map<String, KDJ>> getModel(Security security, Collection<Timeseries> securityList) {
		return getModel(security.getName(), securityList);
	}

	@Override
	public Map<String, Map<String, KDJ>> getModel(String security, Collection<Timeseries> securityList) {
		Map<String, Map<String, KDJ>> tradeDateMap = new TreeMap<>();
		KDJCalculator calculator = new KDJCalculator(n, kCycle, dCycle);
		for (Timeseries model : securityList) {
			KDJ kdj = calculator.calc(model.getClose(), model.getHigh(), model.getLow());
			tradeDateMap.putIfAbsent(model.getTradeDate(), new TreeMap<>());
			Map<String, KDJ> tradeTsMap = tradeDateMap.get(model.getTradeDate());
			tradeTsMap.put(model.getTradeTs().toString(), kdj);
		}
		return tradeDateMap;
	}

}
