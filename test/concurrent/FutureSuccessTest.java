package concurrent;


import jrx.concurrent.Future;
import org.junit.Test;

import static concurrent.FutureTestSupport.ControlState.RUNNING;
import static concurrent.FutureTestSupport.ControlState.COMPLETED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FutureSuccessTest extends FutureTestSupport {



  @Test
  public void testSuccessfulCompletion() throws Exception {

      Control futureControl = nextControl();
      Control onCompleteControl = nextControl();
      Control onSuccessControl = nextControl();

      Future<String> future = future( () -> {
          futureControl.waitFor( RUNNING );
          futureControl.become( COMPLETED );
          return "success";
      });

      future.onComplete( t -> {
          onCompleteControl.become( RUNNING );
          assertTrue( t.isSuccess() );
          assertEquals("success", t.value());
          counter.incrementAndGet();
          onCompleteControl.become( COMPLETED );
      } );

      future.onSuccess( s -> {
          onSuccessControl.become( RUNNING );
          assertEquals("success", s );
          counter.incrementAndGet();
          onSuccessControl.become( COMPLETED );
      } );

      futureControl.become( RUNNING );

      onCompleteControl.waitFor( COMPLETED );
      onSuccessControl.waitFor( COMPLETED );

      assertTrue( future.isDone() );
      assertEquals( "success", future.get() );
      assertEquals( 2, counter.get() );

      assertTrue( onCompleteControl.became( RUNNING ).isAfter( futureControl.became( COMPLETED ) ) );
      assertTrue( onSuccessControl.became( RUNNING ).isAfter( futureControl.became( COMPLETED ) ) );
  }



}
