import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.locks.Lock;

public class TrimInitializer implements IOperationInitializer {

    @Override
    public ProducerAnalyser initialize(
            Properties props,
            BlockingQueue<TaskData> encodingQueue,
            Lock logLock,
            String inputPath
    ) throws Exception {
        String trimStart = props.getProperty("trim.start");
        String trimDuration = props.getProperty("trim.duration");
        String defaultFormat = props.getProperty("format.default", "mp4");


        if (trimStart == null || trimStart.isEmpty() || trimDuration == null || trimDuration.isEmpty()) {
            throw new IllegalArgumentException("Chybí kritické parametry pro operaci TRIM (trim.start a trim.duration).");
        }

        return new ProducerAnalyser(
                encodingQueue,
                logLock,
                inputPath,
                OperationType.TRIM,
                defaultFormat, // Výchozí formát (např. mp4)
                trimStart,     // Hodnota z konfigurace
                trimDuration   // Hodnota z konfigurace
        );
    }
}
