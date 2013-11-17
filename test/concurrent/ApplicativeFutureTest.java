package concurrent;


import jrx.concurrent.Future;
import jrx.concurrent.FutureImpl;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import static concurrent.FutureTestSupport.ControlState.COMPLETED;
import static concurrent.FutureTestSupport.ControlState.RUNNING;
import static jrx.concurrent.FutureImpl.on;

public class ApplicativeFutureTest extends FutureTestSupport {

     @Test
    public void testSimpleApplication() throws Exception {

         Control timesFutureControl = nextControl();
         Control wordFutureControl = nextControl();
         Control repeatedWordFutureControl = nextControl();
         Control onSuccessControl = nextControl();

         Future<String> wordFuture = future( () -> {
             wordFutureControl.waitFor( RUNNING );
             wordFutureControl.become( COMPLETED );
             return "Ho";
         });

         Future<Integer> timesFuture = future(() -> {
             timesFutureControl.waitFor(RUNNING);
             timesFutureControl.become(COMPLETED);
             return 3;
         });

         funct.on2<String,Integer,String> repeat = (word,times) -> {
             repeatedWordFutureControl.become( RUNNING );
             String s = "";
             for( int i=0;i<times;i++ ) s = s + word;
             repeatedWordFutureControl.become(  COMPLETED );
             return s;
         };

         Future<String> repeatedWordFuture = on( pure( repeat ) ).applyTo( wordFuture, timesFuture );

         repeatedWordFuture.onSuccess( s -> {
             onSuccessControl.become( RUNNING );
             assertEquals( "HoHoHo", s );
             counter.incrementAndGet();
             onSuccessControl.become( COMPLETED );

         });

         wordFutureControl.become( RUNNING );
         timesFutureControl.become( RUNNING );

         onSuccessControl.waitFor( COMPLETED );

         assertTrue( repeatedWordFuture.isDone() );
         assertEquals( "HoHoHo", repeatedWordFuture.get() );
         assertEquals( 1, counter.get() );

         assertTrue( repeatedWordFutureControl.became( RUNNING ).isAfter( wordFutureControl.became( COMPLETED ) )
                     ||
                     repeatedWordFutureControl.became( RUNNING ).equals( wordFutureControl.became( COMPLETED ) ) );

         assertTrue( repeatedWordFutureControl.became( RUNNING ).isAfter( timesFutureControl.became( COMPLETED ) )
                     ||
                     repeatedWordFutureControl.became( RUNNING ).equals( timesFutureControl.became( COMPLETED ) ) );
     }

    @Test
    public void testReduction() throws Exception {

        Control futuresControl = nextControl();
        Control onSuccessControl = nextControl();

        List<Future<Integer>> futures = new ArrayList<Future<Integer>>();

        for( int i = 1; i <= 10; i++ ) {
            final int num = i;
            futures.add( future( () -> { futuresControl.waitFor( RUNNING ); return num; } ) );
        }

        Future<Integer> sum = executor.reduce( futures, (i1, i2) -> i1 + i2 );

        sum.onSuccess( s -> {
            onSuccessControl.become( RUNNING );
            assertEquals( new Integer( 55 ), s );
            onSuccessControl.become( COMPLETED );
        });

        futuresControl.become( RUNNING );

        onSuccessControl.waitFor( COMPLETED );

        assertTrue( sum.isDone() );
        assertEquals( new Integer( 55 ), sum.get() );
    }

    @Test
    public void testFold() throws Exception {

        Control futuresControl = nextControl();
        Control onSuccessControl = nextControl();

        List<Future<String>> futures = new ArrayList<Future<String>>();

        for( int i = 1; i <= 10; i++ ) {
            final int num = i;
            futures.add( future( () -> { futuresControl.waitFor( RUNNING ); return "Ho"; } ) );
        }

        Future<Integer> wordsSum = executor.fold(futures, 0, (String w, Integer acc) -> acc + w.length());

        wordsSum.onSuccess( s -> {
            onSuccessControl.become( RUNNING );
            assertEquals( new Integer( 20 ), s );
            onSuccessControl.become( COMPLETED );
        });

        futuresControl.become( RUNNING );

        onSuccessControl.waitFor( COMPLETED );

        assertTrue( wordsSum.isDone() );
        assertEquals( new Integer( 20 ), wordsSum.get() );
    }

    @Test
    public void testSequence() throws Exception {

        Control futuresControl = nextControl();
        Control onSuccessControl = nextControl();

        List<Future<Integer>> futures = new ArrayList<Future<Integer>>();

        for( int i = 1; i <= 10; i++ ) {
            final int num = i;
            futures.add( future( () -> { futuresControl.waitFor( RUNNING ); return num; } ) );
        }

        Future<List<Integer>> sequence = executor.sequence( futures );

        Future<Integer> sum = sequence.apply( pure( is -> {
            int s = 0;
            for (int i : is) s = s + i;
            return s;
        }));

        sum.onSuccess( s -> {
            onSuccessControl.become( RUNNING );
            assertEquals( new Integer( 55 ), s );
            onSuccessControl.become( COMPLETED );
        });

        futuresControl.become( RUNNING );

        onSuccessControl.waitFor( COMPLETED );

        assertTrue( sum.isDone() );
        assertEquals( new Integer( 55 ), sum.get() );
    }
}
