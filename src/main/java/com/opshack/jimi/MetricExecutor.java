package com.opshack.jimi;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;

public class MetricExecutor extends ScheduledThreadPoolExecutor{

	private final Map<Runnable, Boolean> inProgress = new ConcurrentHashMap<Runnable,Boolean>();
	private final ThreadLocal<Long> startTime = new ThreadLocal<Long>();
	private long totalTime;
	private int totalTasks;
	
	public MetricExecutor(int size) {
		super(size);
	}
	
	protected void beforeExecute(Thread t, Runnable r) {
        super.beforeExecute(t, r);
        inProgress.put(r, Boolean.TRUE);
        startTime.set(new Long(System.currentTimeMillis()));
    }

    protected void afterExecute(Runnable r, Throwable t) {
        long time = System.currentTimeMillis() - startTime.get().longValue();
        synchronized (this) {
            totalTime += time;
            ++totalTasks;
        }
        inProgress.remove(r);
        super.afterExecute(r, t);
    }

    public Set<Runnable> getInProgressTasks() {
        return Collections.unmodifiableSet(inProgress.keySet());
    }

    public synchronized int getTotalTasks() {
        return totalTasks;
    }

    public synchronized double getAverageTaskTime() {
    	
    	if (totalTasks < 0) {
    		totalTasks = 0;
    		totalTime = 0;
    	}
    	
        return (totalTasks == 0) ? 0 : totalTime / totalTasks;
    }
}
