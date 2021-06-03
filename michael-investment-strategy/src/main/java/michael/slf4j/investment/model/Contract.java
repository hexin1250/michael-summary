package michael.slf4j.investment.model;

public interface Contract {
	public Security getSecurity();
	public double getLow();
	public double getHigh();
	public double getOpen();
	public double getClose();
	public double getOpenInterest();

}
