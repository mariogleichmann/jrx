package jrx.concurrent;

import java.util.List;
import java.util.concurrent.Callable;


public interface ReactiveExecutorService {

    public <T> Future<T> execute(Callable<T> task);
    
    public <T> Future<T> pure(T value);
    
	public <A> Future<A> reduce(List<Future<A>> futures, funct.on2<A, A, A> reducer);

	public <A,B> Future<B> fold(List<Future<A>> futures, B seed, funct.on2<A, B, B> folder);

	public <A> Future<List<A>> sequence(List<Future<A>> futures);

}
