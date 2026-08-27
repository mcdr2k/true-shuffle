module nl.martderoos.trueshuffle {
    requires se.michaelthelin.spotify;
    requires org.apache.logging.log4j;
    requires org.apache.httpcomponents.core5.httpcore5;
    requires com.google.gson;

    exports nl.martderoos.trueshuffle.api;
    exports nl.martderoos.trueshuffle.api.exceptions;
    exports nl.martderoos.trueshuffle.api.jobs;
    exports nl.martderoos.trueshuffle.api.model;
    exports nl.martderoos.trueshuffle.api.requests.exceptions;
}
