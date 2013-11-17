package concurrent;


import funct.Action;
import jrx.concurrent.ExecutorServiceAdapter;
import jrx.concurrent.Future;
import jrx.concurrent.ReactiveExecutorService;
import org.junit.Before;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static concurrent.FutureTestSupport.ControlState.RUNNING;
import static concurrent.FutureTestSupport.ControlState.WAITING;
import static java.time.Instant.now;

public class FutureTestSupport {

    enum ControlState{ WAITING, RUNNING, COMPLETED }

    protected volatile ControlState control_1_state;
    protected volatile ControlState control_2_state;
    protected volatile ControlState control_3_state;
    protected volatile ControlState control_4_state;
    protected volatile ControlState control_5_state;

    protected volatile AtomicInteger counter;

    protected ReactiveExecutorService executor = null;

    private AtomicInteger nextFreeControl;

    private List<Control> controls = new ArrayList<Control>(){{

        add( new Control(){
                protected ControlState state() { return control_1_state; }
                protected void set( ControlState newState ) { control_1_state = newState; }
        });

        add( new Control(){
            protected ControlState state() { return control_2_state; }
            protected void set( ControlState newState ) { control_2_state = newState; }
        });

        add( new Control(){
            protected ControlState state() { return control_3_state; }
            protected void set( ControlState newState ) { control_3_state = newState; }
        });

        add( new Control(){
            protected ControlState state() { return control_4_state; }
            protected void set( ControlState newState ) { control_4_state = newState; }
        });

        add( new Control(){
            protected ControlState state() { return control_5_state; }
            protected void set( ControlState newState ) { control_5_state = newState; }
        });
    }};




    @Before
    public void setUp(){
        executor = new ExecutorServiceAdapter( Executors.newFixedThreadPool(5) );

        nextFreeControl = new AtomicInteger( 0 );

        counter = new AtomicInteger( 0 );

        control_1_state = WAITING;
        control_2_state = WAITING;
        control_3_state = WAITING;
        control_4_state = WAITING;
        control_5_state = WAITING;
    }


    protected <T> Future<T> future( Callable<T> task ){

        Future<T> future = executor.execute(task);

        return future;
    }

    protected <A,B,C> Future<funct.on2<A,B,C>> pure( funct.on2<A,B,C> f ){
        return future( () -> f );
    }


    protected <T> Control nextControl(){
        return controls.get( nextFreeControl.getAndIncrement() );
    }



    protected abstract static class Control{

        protected Map<ControlState,Instant> instants = new HashMap<>();

        protected abstract ControlState state();

        protected abstract void set( ControlState newState );

        public void become( ControlState newState ){
            if( newState.ordinal() - state().ordinal() == 1 ){
                set( newState );
                instants.put( newState, now() );
            }
            else{
                throw new RuntimeException( "unallowed transition from " + state() + " to " + newState );
            }
        }

        public void waitFor( ControlState aspiredState ){
          while( state() != aspiredState ){}
        }

        public Instant became( ControlState state ){
            return instants.get( state );
        }

    }

}
