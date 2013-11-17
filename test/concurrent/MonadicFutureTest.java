package concurrent;


import jrx.concurrent.Future;
import org.junit.Test;

import static concurrent.FutureTestSupport.ControlState.COMPLETED;
import static concurrent.FutureTestSupport.ControlState.RUNNING;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MonadicFutureTest extends FutureTestSupport {

    @Test
    public void testOneTimeFlatMap() throws Exception {

        Control futureControl = nextControl();
        Control flatMappedFutureControl = nextControl();
        Control onSuccessControl = nextControl();

        Future<String> future = future( () -> {
            futureControl.waitFor( RUNNING );
            futureControl.become( COMPLETED );
            return "success";
        });

        Future<Integer> flatMappedFuture = future.flatMap( s ->
            future( () -> {
                flatMappedFutureControl.become( RUNNING );
                flatMappedFutureControl.become( COMPLETED );
                return s.length();
            } ) );

        flatMappedFuture.onSuccess( i -> {
            onSuccessControl.become( RUNNING );
            assertEquals( new Integer( "success".length() ), i );
            counter.incrementAndGet();
            onSuccessControl.become( COMPLETED );
        } );

        futureControl.become( RUNNING );

        onSuccessControl.waitFor( COMPLETED );

        assertTrue( flatMappedFuture.isDone() );
        assertEquals( new Integer( "success".length() ), flatMappedFuture.get() );
        assertEquals( 1, counter.get() );

        assertTrue( flatMappedFutureControl.became( RUNNING ).isAfter( futureControl.became( COMPLETED ) )
                    ||
                    flatMappedFutureControl.became( RUNNING ).equals( futureControl.became( COMPLETED ) ) );

        assertTrue( onSuccessControl.became( RUNNING ).isAfter( flatMappedFutureControl.became( COMPLETED ) )
                    ||
                    onSuccessControl.became( RUNNING ).equals( flatMappedFutureControl.became( COMPLETED ) ) );

    }

}
