package jrx.concurrent;

public class Failure<A> extends Try<A> {

	private Throwable failure = null;
	
	public Failure( Throwable failure ){
		this.failure = failure;
	}
	
	public boolean isFailure(){ return true; }

	public Throwable getException(){ return failure; }

    public A get() throws Throwable{
        throw failure;
    }
}
