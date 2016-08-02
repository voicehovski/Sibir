import java .util.*;
/*
	Время ожидания потока
	Время начала выполнения
	Управление: приостановка всего, изменение приоритетов, групп
*/
public class Krakatau implements Runnable {
	
	public static final int CONSUMER_COUNT = 4;
	public static final int PAUSE = 8196;

	private DataSource source;
	private static Consumer [] consumers;

	public Krakatau ( DataSource source ) {		
		this .source = source;
	}
	
	public void run (  ) {
		while ( true ) {
			try {
				Thread .sleep ( PAUSE );
			} catch ( InterruptedException ie ) {
				Log .write ( "Control thread was interrupted" );
			}
			for ( Consumer t : consumers ) {
				System .out .format ( "%s[%s]#%s at group %s is %s%n",
					t .getName (  ),
					t .getId (  ),
					t .getPriority (  ),
					t .getThreadGroup (  ) .getName (  ),
					t .getState (  )
				);
			}
		}
	}
	
	public static void main ( String ... args ) {
		Krakatau data = new Krakatau ( new DataSource () );
		consumers = new Consumer [CONSUMER_COUNT];
		ThreadGroup patrici = new ThreadGroup ( "PATRICI" );
		ThreadGroup lumumbi = new ThreadGroup ( "LUMUMBI" );
		
		for (int i = 0; i < CONSUMER_COUNT/2; i++) {
			consumers [i] = new Consumer ( data, patrici, "Patric-" + i );
			consumers [i] .start (  );
		}
		for (int i = CONSUMER_COUNT/2; i < CONSUMER_COUNT; i++) {
			consumers [i] = new Consumer ( data, lumumbi, "Lumumb-" + i );
			consumers [i] .start (  );
		}
		Producer p = new Producer ( data, "Producer" );
		p .start (  );
		
		//new Thread ( data ) .start (  );
	}
	
	private volatile boolean sourceIsEmpty = true;
	private volatile boolean sourceIsOverflowed = false;
	
	public String get (  ) {
		String s = "";
		synchronized ( this ) {
			Calendar t = Log .writeTimed ( Thread .currentThread () .getName () + " has started to wait" );	//This is the reason of using Thread .currentThread () - we have no lint to a thread insatance
			while ( sourceIsEmpty )
				try {
					wait (  );
				} catch ( InterruptedException ie ) {
					Log .write ( "Data getting was interrupted" );
				}
			Log .writeTimed ( Thread .currentThread () .getName () + " has been waiting for", t );
			s = source .get (  );
			sourceIsEmpty = source .isEmpty (  );
			sourceIsOverflowed = false;
			//notifyAll (  );	//Notifies all threads, waiting for this object inner monitor, they can put data
			return s;
		}
	}
	public String set ( String s ) {
		synchronized ( this ) {
			Calendar t = Log .writeTimed ( Thread .currentThread () .getName () + " has started to wait" );
			while ( sourceIsOverflowed )
				try {
					wait (  );
				} catch ( InterruptedException ie ) {
					Log .write ( "Data ["+s+"] setting was interrupted" );
				}
			Log .writeTimed ( Thread .currentThread () .getName () + " has been waiting for", t );
			sourceIsOverflowed = source .set ( s );
			sourceIsEmpty = false;
			//notifyAll (  );	//Notifies all waiting threads, they can get data
			return s;
		}
	}

	static class Consumer extends Thread {
		
		public static final int PAUSE = 2048 * 8;
		
		public Consumer ( Krakatau data ) {
			this .data = data;
		}
		public Consumer ( Krakatau data, ThreadGroup group, String name ) {
			super ( group, name );
			this .data = data;
		}
		
		private Krakatau data;
		
		public void run (  ) {
			Log .write ( getName (  ) + " was started" );
			while ( true ) {
				try {
					Calendar t = Log .writeTimed ( getName (  ) + " has asleept" ); //to fall asleep / to drop off
					Thread .sleep ( (int)(Math .random (  ) * PAUSE) );
					Log .writeTimed ( getName (  ) + " has been sleeping for", t );	//to wake up / to awake
				} catch ( InterruptedException ie ) {
					Log .write ( getName (  ) + " was interrupted" );
				}
				System .out .format ( "%s got %s%n", getName (  ), data .get (  ) );
				synchronized ( data ) {
					data .notifyAll (  );
				}
			}
		}
	}
	
	static class Producer extends Thread {
		
		public static final int PAUSE = 2048 * 8;
		
		public Producer ( Krakatau data ) {
			this .data = data;
		}
		public Producer ( Krakatau data, String name ) {
			super (name);
			this .data = data;
		}
		public Producer ( Krakatau data, ThreadGroup group, String name ) {
			super (group, name);
			this .data = data;
		}
		
		private Krakatau data;
		
		public void run (  ) {
			Log .write ( getName (  ) + " was started" );
			while ( true ) {
				try {
					Calendar t = Log .writeTimed ( getName (  ) + " has asleept" ); //to fall asleep / to drop off
					Thread .sleep ( (int)(Math .random (  ) * PAUSE) );
					Log .writeTimed ( getName (  ) + " has been sleeping for", t );	//to wake up / to awake
				} catch ( InterruptedException ie ) {
					Log .write ( getName (  ) + " was interrupted" );	//to wake up / to awake
				}
				System .out .format ( "%s put %s%n", getName (  ), data .set ( Utilities .createRandomString (  ) ) );
				synchronized (data) {
					data .notifyAll (  );
				}
			}
		}
	}	
	
}

/*	A bounded queue.
*Method get () returns the head or null if the queue is empty.
*Method set () puts a string into a queue and returns false if the queue is overflowed	*/

class DataSource {
	public static final int CAPACITY = 8;
	
	private String [] data = new String [CAPACITY];
	private int index = 0;
	
	public boolean set ( String s ) {
		if (index == CAPACITY)
			return false;
		data [index++] = s;
		return true;
	}
	public String get (  ) {
		if (index == 0)
			return null;
		if ( index == 1 ) {
			index = 0;
			return data [0];
		}
		String r = data [0];
		System .arraycopy (data, 1, data, 0, --index);
		return r;
	}
	public boolean isEmpty (  ) {
		return index == 0;
	}
}

class Utilities {
	
	static long id = 0;
	
	static String createRandomString (  ) {
		return "random-" + id++;
	}
}

class Log {
	//Varargs...
	public static void write ( String msg ) {
		System .out .format ( "%s%n", msg );
	}
	public static Calendar writeTimed ( String msg ) {
		Calendar c = Calendar.getInstance (  );
		System .out .format ( "%1$tM:%1$tS:%1$tL - %2$s%n", c, msg );
		return c;
	}
	public static Calendar writeTimed ( String msg, Calendar c ) {
		Calendar current = Calendar .getInstance (  );
		long i = current .getTimeInMillis (  ) - c .getTimeInMillis (  );//System .currentTimeMillis (  )
		System .out .format ( "%1$tM:%1$tS:%1$tL - %2$s %3$d:%4$d:%5$d%n", current, msg, i/60000, i%60000/1000, i%60000000 );
		return current;
	}
}