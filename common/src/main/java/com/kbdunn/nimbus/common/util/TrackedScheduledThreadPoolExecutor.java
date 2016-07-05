package com.kbdunn.nimbus.common.util;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

public class TrackedScheduledThreadPoolExecutor extends ScheduledThreadPoolExecutor implements RejectedExecutionHandler {

	private static final Logger log = LogManager.getLogger(TrackedScheduledThreadPoolExecutor.class);
	
	public TrackedScheduledThreadPoolExecutor(int corePoolSize) {
		super(corePoolSize);
		super.setRejectedExecutionHandler(this);
	}
	
	public TrackedScheduledThreadPoolExecutor(int corePoolSize, ThreadFactory threadFactory) {
		super(corePoolSize, threadFactory);
		super.setRejectedExecutionHandler(this);
	}
	
	public long getQueueSize() {
		return super.getQueue().size();
	}
	
	@Override
	public <T> Future<T> submit(Callable<T> task) {
		return super.submit((Callable<T>) new Task<T>(task, clientTrace(), Thread.currentThread().getName()));
	}

	@Override
	public Future<Void> submit(Runnable command) {
		return super.submit((Callable<Void>) new Task<Void>(command, clientTrace(), Thread.currentThread().getName()));
	}

	@Override
	public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
		return super.schedule((Runnable) new Task<Void>(command, clientTrace(), Thread.currentThread().getName()), delay, unit);
	}

	@Override
	public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long delay, TimeUnit unit) {
		return super.scheduleAtFixedRate((Runnable) new Task<Void>(command, clientTrace(), Thread.currentThread().getName()), initialDelay, delay, unit);
	}
	
	@Override
	public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
		log.warn("Task was rejected " + r.toString());
	}
	
	private Exception clientTrace() {
		return new Exception("Client stack trace");
	}
	
	private class Task<T> implements Callable<T>, Runnable {
		
		private Callable<T> callable;
		private String threadName;
		private Exception clientStack; 
		
		Task(final Callable<T> task, final Exception clientStack, String threadName) {
			this.callable = task;
			this.clientStack = clientStack;
			this.threadName = threadName;
		}
		
		Task(Runnable command, final Exception clientStack, String threadName) {
			this(() -> {
				command.run();
				return null;
			}, clientStack, threadName);
		}
		
		@Override
		public void run() {
			try {
				execute();
			} catch (Exception e) {
				// Already logged
			}
		}
		
		@Override
		public T call() throws Exception {
			return execute();
		}
		
		private T execute() throws Exception {
			try {
				return callable.call();
			} catch (Exception e) {
				log.error("Error encountered while executing task", e);
				log.error(e.getClass().getName() + 
						" was thrown from task submitted by thread " + threadName + " at the trace below",
						clientStack);
				throw e;
			}
		}
	}
}
