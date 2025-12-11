import java.io.BufferedReader;
import java.io.File;
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
     * metoda na přidání watermarku
     * @param task, objekt ve které jsou uležny data pro ffmpeg
     * @return vrátí kód úspěchu
     * @throws Exception
     */
    private int addWatermark(TaskData task) throws Exception {
        String ffmpegPath = Main.FFMPEG_PATH_EXTERNAL;

        List<String> command = new ArrayList<>(List.of(
                ffmpegPath, "-y",
                "-i", task.getInputPath(),
                "-i", Main.EXTERNAL_WATERMARK_PATH,
                "-filter_complex", "[0:v][1:v] overlay=10:10",
                task.getOutputPath()
        ));

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);

        Process process = pb.start();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
            }
        }
        return process.waitFor();
    }

    /**
     * metoda na vystřižení části videa
     * @param task, objekt ve které jsou uležny data pro ffmpeg
     * @return vrátí kód úspěchu
     * @throws Exception
     */
    private int trim(TaskData task) throws Exception{
        String ffmpegPath = Main.FFMPEG_PATH_EXTERNAL;

        List<String> command = new ArrayList<>(List.of(
                ffmpegPath, "-y",
                "-ss", task.getTrimStart(),
                "-i", task.getInputPath(),
                "-t", task.getTrimDuration(),
                "-c", "copy",
                task.getOutputPath()
        ));

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);

        Process process = pb.start();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
            }
        }

        return process.waitFor();
    }

    /**
     * metoda na změnu formátu videa
     * @param task, objekt ve které jsou uležny data pro ffmpeg
     * @return vrátí kód úspěchu
     * @throws Exception
     */
    private int convertFormat(TaskData task) throws Exception{
        String ffmpegPath = Main.FFMPEG_PATH_EXTERNAL;

        List<String> command = new ArrayList<>(List.of(
                ffmpegPath, "-y",
                "-i", task.getInputPath(),
                task.getOutputPath()
        ));

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);

        Process process = pb.start();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
            }
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
                    finishedQueue.put(task);
                    break;
                }

                int exitCode = 1;

                switch (task.getOperation()) {
                    case ADD_WATERMARK:
                        exitCode = addWatermark(task);
                        break;
                    case TRIM:
                        exitCode = trim(task);
                        break;
                    case CONVERT_FORMAT:
                        exitCode = convertFormat(task);
                        break;
                    default:
                        safeLog("Chyba: " + task);
                        break;
                }

                if (exitCode == 0) {
                    safeLog("Úspěšně dokončeno: " + task.getOperation() + " na " + task);
                    finishedQueue.put(task);
                } else {
                    safeLog("Chyba při akci " + task.getOperation());
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                safeLog("Přerušení konzumenta");
                break;
            } catch (Exception e) {
                safeLog("Neznámá chyba: " + e.getMessage());
            }
        }
        safeLog("Ukončen");
    }
}