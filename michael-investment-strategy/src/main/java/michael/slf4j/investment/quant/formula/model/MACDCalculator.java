package michael.slf4j.investment.quant.formula.model;

public class MACDCalculator {
	private int s;
	private int l;
	private int diff;
	private double emas;
	private double emal;
	private double dea;
	public MACDCalculator(int s, int l, int diff, double emas, double emal, double dea) {
		this.s = s;
		this.l = l;
		this.diff = diff;
		this.emas = emas;
		this.emal = emal;
		this.dea = dea;
	}
	public MACD calc(double close) {
		emas = emas * (s - 1) / (s + 1) + close * 2 / (s + 1);
		emal = emal * (l - 1) / (l + 1) + close * 2 / (l + 1);
		double diffV = emas - emal;
		double deaV = dea * (diff - 1) / (diff + 1) + diffV * 2 / (diff + 1);
		double macdV = (diffV - deaV) * 2;
		return new MACD(macdV, diffV, deaV);
	}
	
	public static class MACD {
		private final double MACD;
		private final double DIFF;
		private final double DEA;
		public MACD(double MACD, double DIFF, double DEA) {
			this.DEA = DEA;
			this.DIFF = DIFF;
			this.MACD = MACD;
		}
		public double getMACD() {
			return MACD;
		}
		public double getDIFF() {
			return DIFF;
		}
		public double getDEA() {
			return DEA;
		}
		public int operate() {
			if(MACD > 0) {
				return 1;
			} else if(MACD < 0) {
				return -1;
			}
			return 0;
//			if(MACD < 0 && DEA > 0) {
//				return -1;
//			} else if(MACD > 0 && DIFF < 0) {
//				return 1;
//			}
//			return 0;
		}
	}

}
