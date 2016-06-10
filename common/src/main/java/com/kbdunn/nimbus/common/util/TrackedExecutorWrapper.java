package com.kbdunn.nimbus.common.util;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

public class TrackedExecutorWrapper {

	private static final Logger log = LogManager.getLogger(TrackedExecutorWrapper.class);
	
	private final ScheduledExecutorService executor;
	private final AtomicInteger taskCount;
	
	public TrackedExecutorWrapper(ScheduledExecutorService executor) {
		this.executor = executor;
		taskCount = new AtomicInteger(0);
	}

	public int getTaskCount() {
		return taskCount.get();
	}
	
	public boolean isTerminated() {
		return executor.isTerminated();
	}

	public void shutdown() throws InterruptedException {
		executor.shutdown();
	}
	
	public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
		return executor.awaitTermination(timeout, unit);
	}
	
	public List<Runnable> shutdownNow() {
		taskCount.set(0);
		return executor.shutdownNow();
	}
	
	public <T> Future<T> submit(Callable<T> task) {
		return executor.submit(wrapTask(task, clientTrace(), Thread.currentThread().getName()));
	}

	public Future<Void> submit(Runnable command) {
		return executor.submit(wrapTask(toCallable(command), clientTrace(), Thread.currentThread().getName()));
	}
	
	public Future<Void> schedule(Runnable command, long delay, TimeUnit unit) {
		return executor.schedule(wrapTask(toCallable(command), clientTrace(), Thread.currentThread().getName()), delay, unit);
	}
	
	public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long delay, TimeUnit unit) {
		return executor.scheduleAtFixedRate(wrapCommand(command, clientTrace(), Thread.currentThread().getName()), initialDelay, delay, unit);
	}
	
	/*public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
		return tasks.stream().map(this::submit).collect(toList());
	}*/
	
	private <T> Callable<T> wrapTask(final Callable<T> task, final Exception clientStack, String threadName) {
		taskCount.incrementAndGet();
		return () -> {
			try {
				return task.call();
			} catch (Exception e) {
				log.error(e.getClass().getName() + 
						" was thrown from task submitted by thread " + threadName + " at the trace below",
						clientStack);
				log.error("Task exception below", e);
				throw e;
			} finally {
				taskCount.decrementAndGet();
			}
		};
	}
	private Runnable wrapCommand(Runnable command, final Exception clientStack, String threadName) {
		taskCount.incrementAndGet();
		return () -> {
			try {
				command.run();
			} catch (Exception e) {
				log.error(e.getClass().getName() + 
						" was thrown from task submitted by thread " + threadName + " at the trace below",
						clientStack);
				throw e;
			} finally {
				taskCount.decrementAndGet();
			}
		};
	}
	
	private Exception clientTrace() {
		return new Exception("Client stack trace");
	}
	
	private Callable<Void> toCallable(Runnable command) {
		return () -> {
			command.run();
			return null;
		};
	}
}
