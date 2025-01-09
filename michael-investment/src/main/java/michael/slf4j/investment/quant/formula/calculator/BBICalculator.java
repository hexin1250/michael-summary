package michael.slf4j.investment.quant.formula.calculator;

public class BBICalculator {
	private int[] cycles;
	
	public BBICalculator(int... cycles) {
		this.cycles = cycles;
	}
	
	public double calc(double[] closeArr) {
		double sum = 0D;
		for (int i = 0; i < cycles.length; i++) {
			int period = cycles[i];
			int len = closeArr.length;
			double tmp = 0D;
			for (int j = len - period - 1; j < len; j++) {
				tmp += closeArr[j];
			}
			sum += tmp / ((double) period);
		}
		return sum / ((double)cycles.length);
	}

}
