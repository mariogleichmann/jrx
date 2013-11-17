package jrx.concurrent;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.Future;

public class ExecutorServiceAdapter implements ExecutorService, ReactiveExecutorService {

	private ExecutorService targetExecutor = null;

	public ExecutorServiceAdapter( ExecutorService target ){
		this.targetExecutor = target;
	}
	
	@Override
    public void execute( Runnable command ) {
		targetExecutor.execute( command );
    }

	@Override
    public boolean awaitTermination( long timeout, TimeUnit unit ) throws InterruptedException {
		return targetExecutor.awaitTermination( timeout, unit );
    }

	@Override
    public <T> List<Future<T>> invokeAll( Collection<? extends Callable<T>> callables ) throws InterruptedException {
	    return targetExecutor.invokeAll( callables );
    }

	@Override
    public <T> List<Future<T>> invokeAll( Collection<? extends Callable<T>> callables, long timeout, TimeUnit unit ) throws InterruptedException {
		return targetExecutor.invokeAll( callables, timeout, unit );
    }

	@Override
    public <T> T invokeAny( Collection<? extends Callable<T>> callables ) throws InterruptedException, ExecutionException {
	    return targetExecutor.invokeAny( callables );
    }

	@Override
    public <T> T invokeAny( Collection<? extends Callable<T>> callables, long timeout, TimeUnit unit ) throws InterruptedException, ExecutionException, TimeoutException {
		return targetExecutor.invokeAny( callables, timeout, unit );
	}

	@Override
    public void shutdown() {
		targetExecutor.shutdown();
    }

	@Override
    public List<Runnable> shutdownNow() {
	    return targetExecutor.shutdownNow();
    }

	@Override
    public boolean isShutdown() {
	    return targetExecutor.isShutdown();
    }

	@Override
    public boolean isTerminated() {
	    return targetExecutor.isTerminated();
    }

	@Override
    public <T> Future<T> submit( Callable<T> task ) {
	    return targetExecutor.submit( task );
    }
	
	@Override
    public <T> Future<T> submit( Runnable task, T result ) {
	    return targetExecutor.submit( task, result );
    }

	@Override
    public Future<?> submit( Runnable task ) {
	    return targetExecutor.submit( task );
    }

	
	public <T> jrx.concurrent.Future<T> execute( Callable<T> task ){
		return FutureImpl.futureFor(task, targetExecutor);
	}
	
	public <T> jrx.concurrent.Future<T> pure( T value ){
		return FutureImpl.pure( value, targetExecutor );
	}
	
	public <A> jrx.concurrent.Future<A> reduce( List<jrx.concurrent.Future<A>> futures, funct.on2<A,A,A> reducer ){
		return FutureImpl.reduce( futures, reducer, targetExecutor );
	}
	
	public <A,B> jrx.concurrent.Future<B> fold( List<jrx.concurrent.Future<A>> futures, B seed, funct.on2<A,B,B> folder ){
		return FutureImpl.fold( futures, seed, folder, targetExecutor );
	}
	
	public <A> jrx.concurrent.Future<List<A>> sequence( List<jrx.concurrent.Future<A>> futures ){
		return FutureImpl.sequence( futures, targetExecutor );
	}
	
}
