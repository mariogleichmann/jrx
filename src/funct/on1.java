package funct;

/**
 * Created with IntelliJ IDEA.
 * User: Mario
 * Date: 14.04.13
 * Time: 11:37
 * To change this template use File | Settings | File Templates.
 */
public interface on1<A1,R> {

    public R _(A1 arg1);

    public default funct.on<R> fst(A1 a1){
        return () -> this._( a1 );
    }

    public default <R2> on1<A1,R2> andThen(on1<R, R2> func){
        return ( A1 a1) -> func._( this._( a1 ) );
    }
}
