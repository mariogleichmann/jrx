package funct;

/**
 * Created with IntelliJ IDEA.
 * User: Mario
 * Date: 14.04.13
 * Time: 14:35
 * To change this template use File | Settings | File Templates.
 */
public interface on2<A1,A2,R> {

    public R _(A1 arg1, A2 arg2);

    public default on1<A2,R> fst(A1 a1){
        return (A2 a2) -> this._(a1, a2);
    }

    public default on1<A1,R> snd(A2 a2){
        return (A1 a1) -> this._(a1, a2);
    }

    public default on1<A1,on1<A2,R>> curry(){

        on2<A1,A2,R> thisFunc = this;

        return (A1 a1) -> { return new on1<A2,R>(){
                                        public R _(A2 a2) {
                                            return thisFunc._( a1, a2 );
                                        } };
        };
    }
}
