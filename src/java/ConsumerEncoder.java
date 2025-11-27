import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.locks.Lock;

/**
 * provádí CPU náročné kódování paralelně
 * konzumuje úlohy z queue a po dokončení je vkládá do finishedQueue
 */
public class ConsumerEncoder implements Runnable {
    private final BlockingQueue<TaskData> encodingQueue;
    private final BlockingQueue<TaskData> finishedQueue;
    private final Lock logLock;
    private final int id;

    /**
     * konstruktor pro třídu ConsumerEncoder
     * @param id id jednotlivých threadů
     * @param encodingQueue queue ze které bere
     * @param finishedQueue queue do které po dokončení přidá
     * @param logLock lock pro synchonizaci logování
     */
    public ConsumerEncoder(int id, BlockingQueue<TaskData> encodingQueue, BlockingQueue<TaskData> finishedQueue, Lock logLock) {
        this.id = id;
        this.encodingQueue = encodingQueue;
        this.finishedQueue = finishedQueue;
        this.logLock = logLock;
    }

    /**
     * bezpečné vypsání do konzole
     * @param message zpráva kterou má vyspat
     */
    private void safeLog(String message) {
        logLock.lock();
        try {
            System.out.println("ConsumerEncoder " + id +": " + message);
        }
        finally {
            logLock.unlock();
        }
    }

    /**
     * dá příkaz do FFmpeg který se následně spustí
     * @param inputPath cesta k videím
     * @param outputPath cesta k videím
     * @return vrací jestli byl úspěšný
     */
    private int runFFmpeg(String inputPath, String outputPath) throws Exception {

        String ffmpegPath = Main.FFMPEG_PATH_EXTERNAL;

        List<String> command = new ArrayList<>(List.of(
                ffmpegPath, "-y",
                "-i", inputPath,
                "-i", Main.EXTERNAL_WATERMARK_PATH,
                "-filter_complex", "[0:v][1:v] overlay=10:10",
                outputPath
        ));

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);

        Process process = pb.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            while (reader.readLine() != null) {}
        }


        return process.waitFor();
    }

    /**
     * ověřuje vstupy a bezpečné předává úkoly
     */
    @Override
    public void run() {
        while (true) {
            try {
                TaskData task = encodingQueue.take();

                if (task.isPoisonPill()) {
                    finishedQueue.put(new TaskData());
                    break;
                }

                int exitCode = runFFmpeg(task.getInputPath(), task.getOutputPath());

                if (exitCode == 0) {
                    safeLog("Úspěšně dokončeno: " + task);
                    finishedQueue.put(new TaskData(task.getInputPath(), task.getOutputPath()));
                } else {
                    safeLog("Chyba kódování "+ exitCode + ": " + task);
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                safeLog("Přerušení konzumenta.");
                break;
            } catch (Exception e) {
                safeLog("Neznámá chyba při FFmpeg: " + e.getMessage());
            }
        }
        safeLog("Ukončen");
    }
}