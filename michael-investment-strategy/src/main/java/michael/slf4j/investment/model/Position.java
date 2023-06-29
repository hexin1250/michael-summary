package michael.slf4j.investment.model;

public interface Position {
	public ProfitLoss deal(DirectionEnum dir, double price, int quantity);
	public double total(double price);
	public boolean done();
	public int buyPosition();
	public int sellPosition();
	public DirectionEnum getDirection();

}
