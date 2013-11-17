package jrx.concurrent;

public abstract class Try<A> {
	
	public boolean isSuccess(){ return false; }
	public A value(){ throw new IllegalStateException( "Can't provide a result for instance of type " + getClass() ); }
	
	public boolean isFailure(){ return false; }
	public Throwable getException(){ throw new IllegalStateException( "Can't provide an Exception for instance of type " + getClass() ); }

    public abstract A get() throws Throwable;

    public static <T> Try<T> attempt( funct.on<T> f ){
        try{
            return new Success<T>( f._() );
        }
        catch( ExceptionOnAttempt exc ){
            return new Failure<T>( exc.cause() );
        }
        catch( Exception exc ){
            return new Failure<T>( exc );
        }
    }



    public static class ExceptionOnAttempt extends RuntimeException{

        private Exception cause;

        public ExceptionOnAttempt( Exception cause ){
             this.cause = cause;
        }

        public Exception cause(){
            return cause;
        }
    }

}
