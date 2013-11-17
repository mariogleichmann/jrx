package jrx.concurrent;

public class Success<A> extends Try<A> {

	private A value = null;
	
	public Success( A result ){
		this.value = result;
	}
	
	public A value(){ return value; }
	
    public boolean isSuccess() { return true; }

    public A get() throws Throwable{
        return value();
    }
    
	
}
