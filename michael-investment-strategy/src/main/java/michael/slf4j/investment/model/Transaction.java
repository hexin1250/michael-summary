package michael.slf4j.investment.model;

public class Transaction {
	private final Security security;
	private final DirectionEnum direction;
	private final double price;
	private final int quantity;
	private final double cost;
	public Transaction(Security security, DirectionEnum direction, double price, int quantity, double cost) {
		this.security = security;
		this.direction = direction;
		this.price = price;
		this.quantity = quantity;
		this.cost = cost;
	}
	public Security getSecurity() {
		return security;
	}
	public DirectionEnum getDirection() {
		return direction;
	}
	public double getPrice() {
		return price;
	}
	public int getQuantity() {
		return quantity;
	}
	public double getCost() {
		return cost;
	}

}
