package concurrent;


import jrx.concurrent.Future;
import jrx.concurrent.FutureImpl;
import org.junit.Test;

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
}
