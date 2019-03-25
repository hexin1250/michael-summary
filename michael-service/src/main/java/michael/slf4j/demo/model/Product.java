package michael.slf4j.demo.model;

import java.io.Serializable;
import java.sql.Timestamp;

public class Product implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private long productId;
	private String productName;
	private String category;
	private double price;
	private long availableAmount;
	private Timestamp createTs;
	public long getProductId() {
		return productId;
	}
	public void setProductId(long productId) {
		this.productId = productId;
	}
	public String getProductName() {
		return productName;
	}
	public void setProductName(String productName) {
		this.productName = productName;
	}
	public String getCategory() {
		return category;
	}
	public void setCategory(String category) {
		this.category = category;
	}
	public double getPrice() {
		return price;
	}
	public void setPrice(double price) {
		this.price = price;
	}
	public long getAvailableAmount() {
		return availableAmount;
	}
	public void setAvailableAmount(long availableAmount) {
		this.availableAmount = availableAmount;
	}
	public Timestamp getCreateTs() {
		return createTs;
	}
	public void setCreateTs(Timestamp createTs) {
		this.createTs = createTs;
	}

}
