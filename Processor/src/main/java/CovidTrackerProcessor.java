import sawtooth.sdk.processor.TransactionProcessor;

import java.util.logging.Logger;


public class CovidTrackerProcessor {
    private final static Logger logger = Logger.getLogger(CovidTrackerProcessor.class.getName());

    public static void main(String[] args) {

        //Check connection string to validator is passed in arguments.
        if (args.length != 1) {
            logger.info("Missing argument!! Please pass validator connection string");
        }
        System.out.println("Args " + args[0]);

        // Connect to validator with connection string (tcp://validator:4004)
        TransactionProcessor transactionProcessor = new TransactionProcessor(args[0]);
        transactionProcessor.addHandler(new CovidTrackerHandler());
        Thread thread = new Thread(transactionProcessor);
        thread.run();
    }

}
