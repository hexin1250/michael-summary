package michael.slf4j.investment.model;

import java.util.Objects;

public class Security {
	private final String name;
	private final Variety variety;
	
	public Security(String name, Variety variety) {
		this.name = name;
		this.variety = variety;
	}

	public String getName() {
		return name;
	}

	public Variety getVariety() {
		return variety;
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(name, variety);
	}

	@Override
	public boolean equals(Object obj) {
		if(obj == null || !(obj instanceof Security)) {
			return false;
		}
		Security security = (Security) obj;
		return name.equals(security.getName()) && variety == security.getVariety();
	}

	@Override
	public String toString() {
		return name;
	}

}
