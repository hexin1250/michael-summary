package michael.slf4j.investment.quant.formula.calculator;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingDeque;

public class KDJCalculator {
	private int n;
	private int kCycle;
	private int dCycle;
	
	private double k = 50d;
	private double d = 50d;
	private double j;
	
	private int day = 0;
	
	private Queue<Double> lowQ = new LinkedBlockingDeque<Double>();
	private Queue<Double> highQ = new LinkedBlockingDeque<Double>();
	
	public KDJCalculator(int n, int kCycle, int dCycle) {
		this.n = n;
		this.kCycle = kCycle;
		this.dCycle = dCycle;
	}
	
	public KDJ calc(BigDecimal close, BigDecimal high, BigDecimal low) {
		return calc(close.doubleValue(), high.doubleValue(), low.doubleValue());
	}
	
	public KDJ calc(double close, double high, double low) {
		day++;
		if(day >= n + 1) {
			lowQ.poll();
			highQ.poll();
		}
		lowQ.add(low);
		highQ.add(high);
		double maxHigh = Collections.max(highQ);
		double minLow = Collections.min(lowQ);
		double RSV = (close - minLow) * 100 / (maxHigh - minLow);
		
		k = (kCycle - 1) * k / kCycle + RSV / kCycle;
		d = (dCycle - 1) * d / dCycle + k / dCycle;
		j = 3 * k - 2 * d;
		return new KDJ(k, d, j);
	}
	
	public static class KDJ {
		private final double k;
		private final double d;
		private final double j;
		public KDJ(double k, double d, double j) {
			this.k = k;
			this.d = d;
			this.j = j;
		}
		public double getK() {
			return k;
		}
		public double getD() {
			return d;
		}
		public double getJ() {
			return j;
		}
		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append(k).append(',').append(d).append(',').append(j);
			return sb.toString();
		}
	}

}
