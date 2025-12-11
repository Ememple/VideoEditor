import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.locks.Lock;

public class WatermarkInitializer implements IOperationInitializer {

    @Override
    public ProducerAnalyser initialize(
            Properties props,
            BlockingQueue<TaskData> encodingQueue,
            Lock logLock,
            String inputPath
    ) throws Exception {
        String defaultFormat = props.getProperty("format.default", "mp4");

        return new ProducerAnalyser(
                encodingQueue,
                logLock,
                inputPath,
                OperationType.ADD_WATERMARK,
                defaultFormat,
                null,
                null
        );
    }
}
