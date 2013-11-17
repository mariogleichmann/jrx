package concurrent;


import jrx.concurrent.Future;
import org.junit.Test;

import static concurrent.FutureTestSupport.ControlState.COMPLETED;
import static concurrent.FutureTestSupport.ControlState.RUNNING;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FunctorFutureTest extends FutureTestSupport {

    @Test
    public void testOneTimeMap() throws Exception {

        Control futureControl = nextControl();
        Control mappingFutureControl = nextControl();
        Control onSuccessControl = nextControl();

        Future<String> future = future( () -> {
            futureControl.waitFor( RUNNING );
            futureControl.become( COMPLETED );
            return "success";
        });

        Future<Integer> mappingFuture = future.map( s -> {
            mappingFutureControl.become( RUNNING );
            mappingFutureControl.become( COMPLETED );
            return s.length();
        });

        mappingFuture.onSuccess( i -> {
            onSuccessControl.become( RUNNING );
            assertEquals( new Integer( "success".length() ), i );
            counter.incrementAndGet();
            onSuccessControl.become( COMPLETED );
        } );

        futureControl.become( RUNNING );

        mappingFutureControl.waitFor( COMPLETED );
        onSuccessControl.waitFor( COMPLETED );

        assertTrue( mappingFuture.isDone() );
        assertEquals( new Integer( "success".length() ), mappingFuture.get() );
        assertEquals( 1, counter.get() );

        assertTrue( mappingFutureControl.became( RUNNING ).isAfter( futureControl.became( COMPLETED ) )
                    ||
                    mappingFutureControl.became( RUNNING ).equals(futureControl.became(COMPLETED)) );

        assertTrue( onSuccessControl.became(RUNNING).isAfter(mappingFutureControl.became(COMPLETED))
                    ||
                    onSuccessControl.became(RUNNING).equals(mappingFutureControl.became(COMPLETED)));
    }


    @Test
    public void testMapChain() throws Exception {

        Control futureControl = nextControl();
        Control map1Control = nextControl();
        Control map2Control = nextControl();
        Control map3Control = nextControl();
        Control onSuccessControl = nextControl();

        Future<String> future = future( () -> {
            futureControl.waitFor( RUNNING );
            futureControl.become( COMPLETED );
            return "success";
        });

        Future<Integer> map1Future = future.map( s -> {
            map1Control.become( RUNNING );
            map1Control.become( COMPLETED );
            return s.length();
        });

        Future<String> finalMapFuture = map1Future.map( i -> {
            map2Control.become( RUNNING );
            map2Control.become( COMPLETED );
            return i < 10;
        })
        .map( b -> {
            map3Control.become( RUNNING );
            map3Control.become( COMPLETED );
            return b ? "yes" : "no";
        } );

        finalMapFuture.onSuccess( s -> {
            onSuccessControl.become( RUNNING );
            assertEquals( "yes", s );
            counter.incrementAndGet();
            onSuccessControl.become( COMPLETED );
        } );

        futureControl.become( RUNNING );

        onSuccessControl.waitFor( COMPLETED );

        assertTrue( finalMapFuture.isDone() );
        assertEquals( "yes", finalMapFuture.get() );
        assertEquals( 1, counter.get() );

        assertTrue( map1Control.became( RUNNING ).isAfter( futureControl.became( COMPLETED ) )
                    ||
                    map1Control.became( RUNNING ).equals( futureControl.became( COMPLETED ) ) );

        assertTrue( map2Control.became( RUNNING ).isAfter( map1Control.became( COMPLETED ) )
                    ||
                    map2Control.became( RUNNING ).equals( map1Control.became( COMPLETED ) ) );

        assertTrue( map3Control.became( RUNNING ).isAfter( map2Control.became( COMPLETED ) )
                    ||
                    map3Control.became( RUNNING ).equals( map2Control.became( COMPLETED ) ) );

        assertTrue( onSuccessControl.became( RUNNING ).isAfter( map3Control.became( COMPLETED ) )
                    ||
                    onSuccessControl.became( RUNNING ).equals( map3Control.became( COMPLETED ) ) );
    }
}
