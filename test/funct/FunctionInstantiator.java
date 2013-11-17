package funct;



import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Mario
 * Date: 14.04.13
 * Time: 11:47
 * To change this template use File | Settings | File Templates.
 */
public class FunctionInstantiator {

    public void instantiate(){

        on1<Integer,Integer> succ =  i -> i + 1;

        System.out.println( succ._(2) );


        on2<Integer,Integer,Integer> add = (a,b) -> a+b;

        on1<Integer,Integer> succ2 = add.fst(1);

        System.out.println( succ2._( 2 ) );


        on1<Integer, Integer> succ3 = add.curry()._( 1 );

        System.out.println( succ3._( 2 ) );


        on1<Integer,Integer> doubleSucc = succ.andThen( succ2 );

        System.out.println( doubleSucc._( 2 ) );
    }

    public void curry(){

        on2<Integer,String,String> concatTimes = (i,s) -> {
          String res = "";
          for(int x=0; x<i; x++) res = res + s;
          return res;
        };

        String ho3 = concatTimes._( 3, "ho" );
        System.out.println( concatTimes._( 3, "ho" ) );

        on1<Integer, on1<String, String>> curryConcatTimes = concatTimes.curry();

        String ha3 = curryConcatTimes._( 3 )._( "ha" );
        System.out.println( curryConcatTimes._( 3 )._("ha") );
    }

    public void recursion(){

        on1<List<Integer>,Integer> sumRec
          =
        ( is ) -> {

            if( is.isEmpty() ){
               return 0;
            }
            else {

              return null;
              //return is.get( 0 ) + sumRec._( is.subList( 1, is.size() ) );
            }
        };

        List<Integer> list = new ArrayList<Integer>(){{add(1);add(2);add(3);add(4);add(5);}};
        System.out.println( sumRecInst._( list ) );

    }

    // recursive functions only possible on static or instance instance variable :-(
    // on a previous version, it was also possible with local variables, but this is no longer supported
    on1<List<Integer>,Integer> sumRecInst
            =
            ( is ) -> {

                if( is.isEmpty() ){
                    return 0;
                }
                else {
                    return is.get( 0 ) + sumRecInst._( is.subList( 1, is.size() ) );
                }
            };



    public static void main( String[] args ){

        new FunctionInstantiator().recursion();
    }
}
