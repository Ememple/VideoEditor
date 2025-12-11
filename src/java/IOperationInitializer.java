import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.locks.Lock;

public interface IOperationInitializer {
    ProducerAnalyser initialize(
            Properties props,
            BlockingQueue<TaskData> encodingQueue,
            Lock logLock,
            String inputPath
    ) throws Exception;
}
