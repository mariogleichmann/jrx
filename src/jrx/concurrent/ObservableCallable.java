package jrx.concurrent;

import java.util.concurrent.Callable;


public class ObservableCallable<T> implements Callable<T> {

	private CompletionListener<T> listener = null;
	
	protected Callable<T> target = null;
	
	public ObservableCallable(Callable<T> target, CompletionListener<T> listener){
		this.listener = listener;
		this.target = target;
	}
	
	
	@Override
    public T call() throws Exception {

		Try<T> result = null;
		
		try{
			result = new Success<T>( target.call() );

			return result.value();
		}
		catch( Exception exc ){

			result = new Failure<T>( exc );

			throw exc;
		}
		finally{
			
			if( listener != null ) listener.complete( result );
		}	
	}
	




	public static interface CompletionListener<T>{
		public void complete(Try<T> result);
	}
}
