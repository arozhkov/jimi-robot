package com.opshack.jimi;

import java.util.List;
import java.util.Map;

public class MetricGroups {
	
	public Map<String, List<Map>> metrics;
	
	
	public Map<String, List<Map>> getMetrics() {
		return metrics;
	}
	public void setMetrics(Map<String, List<Map>> metrics) {
		this.metrics = metrics;
	}

	public List<Map> get(String group) {
		return this.metrics.get(group);
	}
}
