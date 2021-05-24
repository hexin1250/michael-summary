package michael.slf4j.investment.model;

public class Security {
	private final String name;
	private final VarietyEnum variety;
	
	public Security(String name, VarietyEnum variety) {
		this.name = name;
		this.variety = variety;
	}

	public String getName() {
		return name;
	}

	public VarietyEnum getVariety() {
		return variety;
	}

}
