import { startServer, stopServer } from './server.js';

/** Thin re-export alias — starts the Express server. */
export const startInternalApi = startServer;
/** Thin re-export alias — stops the Express server. */
export const stopInternalApi = stopServer;
