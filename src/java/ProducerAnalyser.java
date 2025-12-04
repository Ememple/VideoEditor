import java.io.File;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.locks.Lock;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.ArrayList;

/**
 * Analyzuje vstupní složku, volá FFprobe a vkládá úlohy do fronty
 */
public class ProducerAnalyser implements Runnable {
    private final BlockingQueue<TaskData> encodingQueue;
    private final Lock logLock;
    private final String inputPath;
    private final OperationType action;
    private final String format;
    private final String trimStart;
    private final String trimDuration;

    /**
     * konstruktor pro třídu ProducerAnalyser
     *
     * @param encodingQueue fronta pro vkládání úloh
     * @param logLock       lock pro synchonizaci logování
     * @param inputPath     cesta k vstupní složce zadaného uživatelem
     * @param action        akce která se má provést
     * @param format        formát, na který se má video změnit
     * @param trimStart     začátek video
     * @param trimDuration  jak dlouhé má video být
     */
    public ProducerAnalyser(BlockingQueue<TaskData> encodingQueue, Lock logLock, String inputPath, OperationType action, String format, String trimStart, String trimDuration) {
        this.encodingQueue = encodingQueue;
        this.logLock = logLock;
        this.inputPath = inputPath;
        this.action = action;
        this.format = format;
        this.trimStart = trimStart;
        this.trimDuration = trimDuration;
    }

    /**
     * bezpečné vypsání do konzole
     * @param message zpráva kterou má vyspat
     */
    private void safeLog(String message) {
        logLock.lock();
        try {
            System.out.println("ProducerAnalyser: " + message);
        } finally {
            logLock.unlock();
        }
    }

    /**
     * dá příkaz do FFprobe který se následně spustí
     * @param inputPath cesta k videím
     * @return délkou videa nebo 0 při chybě
     */
    private String runFFprobe(String inputPath) {
        String ffprobePath = Main.FFPROBE_PATH_EXTERNAL;

        List<String> command = new ArrayList<>(List.of(
                ffprobePath,
                "-v", "error",
                "-show_entries", "format=duration",
                "-of", "default=noprint_wrappers=1:nokey=1",
                inputPath
        ));

        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String duration = reader.readLine();
                process.waitFor();
                return duration != null ? duration.trim() : "0";
            }
        }
        catch (Exception e) {
            safeLog("Chyba FFprobe " + new File(inputPath).getName() + ": " + e.getMessage());
            return "0";
        }
    }
    /**
     * najde soubory, analyzuje je a přidá je do fronty
     * zajišťuje odeslání poison pills(=signál k ukončení programu) při dokončení nebo chybě
     */
    @Override
    public void run() {
        File dir = new File(inputPath);

        if (!dir.exists()) {
            safeLog("Vstupní složka " + inputPath + " neexistuje");
            for (int i = 0; i < Main.NUM_ENCODER_THREADS; i++) {
                try {
                    encodingQueue.put(new TaskData());
                }
                catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            return;
        }

        File[] files = dir.listFiles((d, name) -> name.endsWith(".mp4") || name.endsWith(".mov") || name.endsWith(".avi"));

        if (files == null || files.length == 0) {
            safeLog("Upozornění: Ve složce nebyly nalezeny žádné video soubory k zpracování");
        }
        else {
            for (File file : files) {
                try {
                    String duration = runFFprobe(file.getAbsolutePath());
                    if (!duration.equals("0")) {
                        String fileName = file.getName();
                        String outputFileName = fileName.substring(0, fileName.lastIndexOf('.')) + "." + this.format;

                        TaskData task = new TaskData(this.action,file.getAbsolutePath(),Main.OUTPUT_DIR + "/" + outputFileName,this.trimStart,this.trimDuration,this.format);
                        encodingQueue.put(task);
                        safeLog("Úspěšně analyzováno: "+ file.getName());
                    }
                    else {
                        safeLog("Chyba: Video " + file.getName() + " přeskočeno kvůli chybě");
                    }

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    safeLog("Přerušení producenta");
                    break;
                }
            }
        }

        for (int i = 0; i < Main.NUM_ENCODER_THREADS; i++) {
            try {
                encodingQueue.put(new TaskData());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        safeLog("Všechny úlohy odeslány");
    }
}