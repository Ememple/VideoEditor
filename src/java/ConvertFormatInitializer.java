import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.locks.Lock;

public class ConvertFormatInitializer implements IOperationInitializer {

    @Override
    public ProducerAnalyser initialize(
            Properties props,
            BlockingQueue<TaskData> encodingQueue,
            Lock logLock,
            String inputPath
    ) throws Exception {
        String targetFormat = props.getProperty("format.target");

        if (targetFormat == null || targetFormat.isEmpty()) {
            throw new IllegalArgumentException("Chybí formát pro konverzi v konfiguraci.");
        }

        return new ProducerAnalyser(
                encodingQueue,
                logLock,
                inputPath,
                OperationType.CONVERT_FORMAT,
                targetFormat,
                null,
                null
        );
    }
}
