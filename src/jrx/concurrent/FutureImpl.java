package jrx.concurrent;


import funct.Action;
import funct.on1;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.*;

public class FutureImpl<T> implements jrx.concurrent.Future<T>, ObservableCallable.CompletionListener<T>{

	protected ExecutorService executor = null;
	
	private java.util.concurrent.Future<?> delegate = null;
	
	private List<Action<Try<T>>> callbacks = new ArrayList<Action<Try<T>>>();

    private FutureImpl() {
    }

    public void onComplete( Action<Try<T>> callback ){
		schedule( callback );
	}
	

	public void onSuccess( Action<? super T> callback ){
		schedule( new OnSuccessAction<T>( callback ) );
	}
	

	public void onFailure( Action<Throwable> callback ){
		schedule( new OnFailureAction<T>( callback ) );
	}
	

	public jrx.concurrent.Future<T> recover( funct.on1<Throwable,? extends T> recoveryAction ){
		
		FutureImpl<T> recoveryPromise = newFuture( executor );
		
		this.onComplete( new RecoveryAction<T>( this, recoveryPromise, recoveryAction ) );
			
		return recoveryPromise;
	}
	

	public jrx.concurrent.Future<T> recoverWith( funct.on1<Throwable, jrx.concurrent.Future<? extends T>> recoveryWithAction ){
		
		FutureImpl<T> recoveryWithPromise = newFuture( executor );
		
		this.onComplete( new RecoveryWithAction<T>( this, recoveryWithPromise, recoveryWithAction ) );
		
		return recoveryWithPromise;
	}


	public <B> jrx.concurrent.Future<B> apply( jrx.concurrent.Future<on1<T,B>> liftedMapper ){
		
		final jrx.concurrent.Future<T> self = this;
		
		return liftedMapper.flatMap( f -> self.map( f ) );
	}
	
	

	public <B> jrx.concurrent.Future<B> map( final funct.on1<? super T,B> mapper ){
		
		FutureImpl<B> mappedPromise = newFuture( executor );
		
		this.onComplete( new MapAction<T,B>( this, mappedPromise, mapper ) );
			
		return mappedPromise;
	}	
	

	public <B> jrx.concurrent.Future<B> flatMap( final funct.on1<? super T, jrx.concurrent.Future<B>> flatMapFunction ){
		
		FutureImpl<B> flatMappedPromise = newFuture( executor );
		
		this.onComplete( new FlatMapAction<T,B>( this, flatMappedPromise, flatMapFunction  ) );
			
		return flatMappedPromise;
	}	


	public void complete( Try<T> result ){
		run( callbacks, result );
	}

	
	private void schedule( Action<Try<T>> callback ) {
		if( this.isDone() ){
            run( callback, this.result() );
		}
		else{
			enqueue( callback );
		}
    }

	private void enqueue( Action<Try<T>> callback ){
		this.callbacks.add( callback );
	}

	private void run( List<Action<Try<T>>> callbacks, Try<T> result ) {
		for( Action<Try<T>> callback : callbacks ) run( callback, result );
    }
	
	private void run( Action<Try<T>> callback, Try<T> result ) {
        executor.execute( () -> callback._( result )  );
    }

	private Try<T> result(){
		try{
			return new Success<T>( this.get() );
		}
		catch( Throwable t ){
			return new Failure<T>( t );
		}
	}

    public static <A> jrx.concurrent.Future<A> reduce( List<jrx.concurrent.Future<A>> futures, funct.on2<A,A,A> reducer, ExecutorService executor ){

        if( futures.isEmpty() ) return FutureImpl.<A,NoSuchElementException> failed( new NoSuchElementException(), executor );

        if( futures.size() == 1 ) return futures.get( 0 );


        jrx.concurrent.Future<on1<A,on1<A,A>>> liftedCurriedReducer = pure( reducer.curry(), executor );

        jrx.concurrent.Future<A> acc = futures.get( 0 );

        for( int i = 1; i < futures.size(); i++ ){

            acc = futures.get( i ).apply( acc.apply( liftedCurriedReducer ) );
        }

        return acc;
    }

	
	public static <A,B> jrx.concurrent.Future<B> fold( List<jrx.concurrent.Future<A>> futures, B seed, funct.on2<A,B,B> reducer, ExecutorService executor ){
		
		if( futures.isEmpty() ) return pure( seed, executor );

        jrx.concurrent.Future<on1<A, on1<B,B>>> liftedCurriedReducer = pure( reducer.curry(), executor );

		
		jrx.concurrent.Future<B> acc = pure( seed, executor );
		
		for( int i = 0; i < futures.size(); i++ ){
			
			acc = acc.apply( futures.get( i ).apply( liftedCurriedReducer ) );
		}
		
		return acc;
	}
	
	
	public static <A> jrx.concurrent.Future<List<A>> sequence( List<jrx.concurrent.Future<A>> futures, ExecutorService executor  ){

		return fold( futures, (List<A>) new ArrayList<A>(), FutureImpl.<A>append(), executor );
	}

    private static <A> funct.on2<A,List<A>,List<A>> append(){
        return ( a, as ) -> {
            as.add( a );
            return as;
        };
    }

	
	/**
	 * Factory-Methode
	 * 
	 * Erzeugt eine neue Future-Instanz zum uebergebenen Callable und bringt dieses mit Hilfe
	 * des uebergebenen Executors zur Ausfuehrung 
	 */
	public static <A> FutureImpl<A> futureFor( Callable<A> callable, ExecutorService executor ){
		return execute( (FutureImpl<A>) newFuture( executor ), callable );
	}
	
	
	public static <B> jrx.concurrent.Future<B> pure( B value, ExecutorService executor ){
		return new PureValueFuture<B>( value, executor );
	}
	
	private static <B,E extends Throwable> jrx.concurrent.Future<B> failed(  E exception, ExecutorService executor ){
		return PureExceptionFuture.<B,E>newFor(exception, executor);
	}
	
	
	private static <A> FutureImpl<A> newFuture( ExecutorService executor ){
		return equip( new FutureImpl<A>(), executor );
	}
	
	private static <A> FutureImpl<A> equip( FutureImpl<A> future, ExecutorService executor ){
		future.executor = executor;		
		return future;
	}

	private static <A> FutureImpl<A> execute( FutureImpl<A> future, Callable<A> callable  ){
		future.delegate = future.executor.submit( new ObservableCallable<A>( callable, future ) );
		return future;
	}
	
	
	@Override
    public boolean cancel( boolean mayInterruptIfRunning ) {
		waitUntilDelegateDefined();
		return delegate.cancel( mayInterruptIfRunning );    
	}

	@Override
    public boolean isCancelled() {
		return delegate != null && delegate.isCancelled();
    }

	@Override
    public boolean isDone() {
		return delegate != null && delegate.isDone();
    }

	@Override
    public T get() throws InterruptedException, ExecutionException {
		waitUntilDelegateDefined();
	    return (T) delegate.get();
    }

	@Override
    public T get( long timeout, TimeUnit unit ) throws InterruptedException, ExecutionException, TimeoutException {
		waitUntilDelegateDefined();
	    return (T) delegate.get( timeout, unit );
    }

	private void waitUntilDelegateDefined() {
		while( delegate == null ){ try { Thread.sleep( 0 ); } catch (InterruptedException e) {} }
	}
	

	@SuppressWarnings("serial")
    private static class OnSuccessAction<T> implements Action<Try<T>>{

		private Action<? super T> callback;
		
		public OnSuccessAction(Action<? super T> callback){
			this.callback = callback;
		}
		
		public void _( Try<T> result ) {

        	if( result.isSuccess() ){
        		
        		callback._(result.value());
        	}
        }				
	}
	
	@SuppressWarnings("serial")
    private static final class OnFailureAction<T> implements Action<Try<T>>{
		
		private Action<Throwable> callback;
		
		public OnFailureAction(Action<Throwable> callback){
			this.callback = callback;
		}
		
        public void _( Try<T> result ) {

        	if( result.isFailure() ){

        		callback._(result.getException());
        	}
        }				
	}
	
	private static class RecoveryAction<T> implements Action<Try<T>>{
		
		private jrx.concurrent.Future<?> origin;
		
		private FutureImpl<T> promise;
		
		private funct.on1<Throwable,? extends T> recoveryAction;
		
		public RecoveryAction(jrx.concurrent.Future<?> origin, FutureImpl<T> promise, final funct.on1<Throwable, ? extends T> recoveryAction){
			this.origin = origin;
			this.promise = promise;
			this.recoveryAction = recoveryAction;
		}
		
        public void _( final Try<T> t ) {

        	if( t.isFailure() ){

                FutureImpl.execute( promise, () -> recoveryAction._( t.getException() ) );
        	}
        	else {
        		
        		promise.delegate = origin;
        		
        		promise.complete( t );
        	}
        }
	}
	
	private static class MapAction<T,B> implements Action<Try<T>>{
		
		private jrx.concurrent.Future<?> origin;
		
		private FutureImpl<B> promise;
		
		private funct.on1<? super T,B> mapFunction;
		
		public MapAction(jrx.concurrent.Future<?> origin, FutureImpl<B> promise, funct.on1<? super T, B> mapFunction){
			this.origin = origin;
			this.promise = promise;
			this.mapFunction = mapFunction;
		}
		
        public void _( final Try<T> t ) {

        	if( t.isSuccess() ){

                FutureImpl.execute( promise, () -> mapFunction._( t.value() ) );
        	}
        	else {
        		
        		promise.delegate = origin;
        		
        		promise.complete( new Failure<>( t.getException() ) );
        	}
        }
	}
	
	private static class FlatMapAction<T,B> implements Action<Try<T>>{
		
		private jrx.concurrent.Future<?> origin;
		
		private FutureImpl<B> promise;

		private funct.on1<? super T, jrx.concurrent.Future<B>> flatmapFunction;
		
		
		public FlatMapAction(jrx.concurrent.Future<?> origin, FutureImpl<B> promise, funct.on1<? super T, jrx.concurrent.Future<B>> flatmapFunction){
			this.origin = origin;
			this.promise = promise;
			this.flatmapFunction = flatmapFunction;
		}

        public void _( final Try<T> t ) {

        	if( t.isSuccess() ){
        	
        		jrx.concurrent.Future<B> resultingFuture = flatmapFunction._( t.value() );
        		
        		promise.delegate = resultingFuture;
        		
				resultingFuture.onComplete( tryB -> promise.complete( tryB ) );
        	}
        	else{
        		
        		promise.delegate = origin;
        		
        		promise.complete( new Failure<B>( t.getException() ) );
        	}
        }		
	}	
	
	private static class RecoveryWithAction<T> implements Action<Try<T>>{
		
		private jrx.concurrent.Future<T> origin;
		private FutureImpl<T> promise;
		private funct.on1<Throwable, jrx.concurrent.Future<? extends T>> recoveryWithAction;

		public RecoveryWithAction(jrx.concurrent.Future<T> origin, FutureImpl<T> promise, funct.on1<Throwable, jrx.concurrent.Future<? extends T>> recoveryWithAction){
			this.origin = origin;
			this.promise = promise;
			this.recoveryWithAction = recoveryWithAction;
		}

        public void _( Try<T> t ) {

        	if( t.isFailure() ){
            	
        		jrx.concurrent.Future<T> resultingFuture = (Future<T>) recoveryWithAction._( t.getException() );
        		
        		promise.delegate = resultingFuture;
        		
				resultingFuture.onComplete( tryT -> promise.complete( tryT ) );
        	}
        	else{
        		
        		promise.delegate = origin;
        		
        		promise.complete( t );
        	}
        }		
	}
	
	
	private static class PureValueFuture<A> extends FutureImpl<A>{
		
		private A value;
		
		public PureValueFuture( A value, ExecutorService executor ){
			this.value = value;
			this.executor = executor;
		}
		
	    public boolean cancel( boolean mayInterruptIfRunning ) {
			return false;    
		}

		@Override
	    public boolean isCancelled() {
			return false;
	    }

		@Override
	    public boolean isDone() {
			return true;
	    }

		@Override
	    public A get() throws InterruptedException, ExecutionException {
		    return value;
	    }

		@Override
	    public A get( long timeout, TimeUnit unit ) throws InterruptedException, ExecutionException, TimeoutException {
		    return value;
	    }
	}
	
	
	private static class PureExceptionFuture<A,E extends Throwable> extends FutureImpl<A>{
		
		private E exception;
		
		public static <A,E extends Throwable> PureExceptionFuture newFor( E exception, ExecutorService executor ){
			
			PureExceptionFuture<A,E> future = new PureExceptionFuture<A, E>();
			future.exception = exception;
			future.executor = executor;
			
			return future;
		}
		
	    public boolean cancel( boolean mayInterruptIfRunning ) {
			return false;    
		}

		@Override
	    public boolean isCancelled() {
			return false;
	    }

		@Override
	    public boolean isDone() {
			return true;
	    }

		@Override
	    public A get() throws InterruptedException, ExecutionException {
		    throw new ExecutionException( exception );
	    }

		@Override
	    public A get( long timeout, TimeUnit unit ) throws InterruptedException, ExecutionException, TimeoutException {
			throw new ExecutionException( exception );
	    }		
	}
	
	
}
