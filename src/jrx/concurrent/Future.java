package jrx.concurrent;

import funct.Action;

public interface Future<T> extends java.util.concurrent.Future<T> {

	/**
	 * When this future is completed, either through an exception, or a value, apply the provided callback function.
	 * If the future has already been completed, this will either be applied immediately or be scheduled asynchronously.
	 * Multiple callbacks may be registered; there is no guarantee that they will be executed in a particular order.
	 * There is also no guarantee that the execute() will be called in the current thread. That is, the implementation may run 
	 * multiple callbacks in a batch within a single execute() and it may run execute() either immediately or asynchronously. 
	 */
	public void onComplete(Action<Try<T>> callback);
	
	/**
	 * When this future is completed successfully (i.e. with a value), apply the provided callback function to the value. 
	 * If the future has already been completed with a value, this will either be applied immediately or be scheduled asynchronously.
	 * The callback will not be called in case that the future is completed with a failure.
	 * 
	 * Multiple callbacks may be registered; there is no guarantee that they will be executed in a particular order.
	 * 
	 * There is also no guarantee that the execute() will be called in the current thread. That is, the implementation may run 
	 * multiple callbacks in a batch within a single execute() and it may run execute() either immediately or asynchronously. 
	 */
	public void onSuccess(final Action<? super T> callback);
	
	/**
	 * When this future is completed with a failure (i.e. with a throwable), apply the provided callback to the throwable.
	 * The future may contain a throwable object and this means that the future failed. 
	 * Futures obtained through combinators have the same exception as the future they were obtained from.
	 * The callback will not be called in case that the future is completed with a value.
	 * 
	 * If failed, the future is completed with an ExecutionException (which holds the original exception as its cause)
	 * If the future has already been completed with a failure, this will either be applied immediately or be scheduled asynchronously.
	 * 
	 * Multiple callbacks may be registered; there is no guarantee that they will be executed in a particular order.
	 * 
	 * There is also no guarantee that the execute() will be called in the current thread. That is, the implementation may run 
	 * multiple callbacks in a batch within a single execute() and it may run execute() either immediately or asynchronously. 
	 */
	public void onFailure(final Action<Throwable> callback);
	
	/**
	 * Creates a new future that will handle any matching throwable that this future might contain. 
	 * If this future contains a valid result then the new future will contain the same. 
	 */
	public Future<T> recover(final funct.on1<Throwable, ? extends T> recovery);
	
	/**
	 * Creates a new future that will handle any matching throwable that this future might contain by assigning 
	 * it a value of another future.
	 * If this future contains a valid result then the new future will contain the same result. 
	 */
	public Future<T> recoverWith(final funct.on1<Throwable, Future<? extends T>> recoveryWithAction);
	
	/**
	 * Creates a new future by applying a function to the successful result of this future.
	 * If this future is completed with an exception then the new future will also contain this exception.
	 * 
	 * (for further information, search the Internet for 'Functor')
	 */
	public <B> Future<B> map(final funct.on1<? super T, B> mapper);
	
	/**
	 * Creates a new future that will hold the result of applying the given lifted Function
	 * to this future.  
	 * 
	 * (for further information, search the Internet for 'Applicative Functor')
	 */
	public <B> Future<B> apply(Future<funct.on1<T, B>> liftedMapper);
	
	/**
	 * Creates a new future by applying a function to the successful result of this future, 
	 * and returns the result of the function as the new future. 
	 * If this future is completed with an exception then the new future will also contain this exception.
	 * 
	 * (for further information, search the Internet for 'Monad')
	 */
	public <B> Future<B> flatMap(final funct.on1<? super T, Future<B>> mapper);

}
