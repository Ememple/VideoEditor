import java.io.File;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.locks.Lock;

/**
 * Kontroluje výsledek a generuje závěr
 */
public class Reporter implements Runnable {
    private final BlockingQueue<TaskData> finishedQueue;
    private final Lock logLock;

    /**
     * konstruktor pro třídu Reporter
     * @param finishedQueue
     * @param logLock
     */
    public Reporter(BlockingQueue<TaskData> finishedQueue, Lock logLock) {
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
            System.out.println("Reporter: "+message);
        } finally {
            logLock.unlock();
        }
    }

    /**
     * příjem hotových úloh a vypisování jejich stavu
     */
    @Override
    public void run() {
        safeLog("Spuštěn");
        int processedCount = 0;

        while (true) {
            try {
                TaskData task = finishedQueue.take();

                if (task.isPoisonPill()) {
                    int poisonPillCount = 1;
                    while (poisonPillCount < Main.NUM_ENCODER_THREADS) {
                        if (finishedQueue.take().isPoisonPill()) {
                            poisonPillCount++;
                        }
                    }
                    break;
                }

                File outputFile = new File(task.getOutputPath());
                if (outputFile.exists() && outputFile.length() > 0) {
                    safeLog("hotovo - " + outputFile.getName() + " Velikost: " + outputFile.length() / 1024 + " kb");
                    processedCount++;
                } else {
                    safeLog("chyba - soubor " + outputFile.getName() + " nebyl nalezen nebo je prázdný");
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                safeLog("Přerušení");
                break;
            }
        }
        safeLog("Ukončen");
    }
}