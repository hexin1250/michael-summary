package michael.slf4j.investment.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Sets;

import michael.slf4j.investment.exception.NotSubscribeException;

public class Bar {
	public Map<String, Contract> map = new HashMap<>();
	
	public Contract getContract(String security) {
		if(!map.containsKey(security)) {
			throw new NotSubscribeException("Doesn't find the security[" + security + "].");
		}
		return map.get(security);
	}
	
	public void update(String security, Contract newContract) {
		if(!map.containsKey(security)) {
			throw new NotSubscribeException("Doesn't find the security[" + security + "].");
		}
		map.replace(security, newContract);
	}
	
	public void subscribe(List<String> securities) {
		Set<String> exists = Sets.newHashSet(map.keySet());
		Set<String> cross = Sets.newHashSet(exists);
		Set<String> targets = Sets.newHashSet(securities);
		
		cross.retainAll(targets);
		exists.removeAll(cross);
		exists.stream().forEach(security -> map.remove(security));
		
		targets.removeAll(cross);
		targets.parallelStream().forEach(security -> map.put(security, null));
	}

}
