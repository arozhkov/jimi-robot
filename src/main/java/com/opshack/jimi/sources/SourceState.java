package com.opshack.jimi.sources;

public enum SourceState {
	
	INIT, 		 // initial state

	// after INIT or RECOVERY
	INITIALIZING,// socket check and properties setup
	ONLINE, 	 // socket is reachable
	OFFLINE, 	 // socket is unreachable

	// after ONLINE
	CONNECTING,  // connection and remote properties setup
	CONNECTED, 	 // if connection opened successfully
	BROKEN, 	 // if connection failed
				 // or if task failed with IOException

	// after BROKEN
	SHUTINGDOWN, // tasks and connections cleanup
	SHUTDOWN,	 // cleanup done

	// after OFFLINE or SHUTDOWN
	RECOVERY 	 // waiting before going to INITIALIZING state
}
