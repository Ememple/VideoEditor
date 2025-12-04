import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Scanner;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Main {
    public static final int QUEUE_CAPACITY = 10;
    public static final int NUM_ENCODER_THREADS = 5;

    public static final String OUTPUT_DIR = "output_videos";

    public static final String FFMPEG_BIN_NAME = "ffmpeg.exe";
    public static final String FFPROBE_BIN_NAME = "ffprobe.exe";
    public static final String INTERNAL_WATERMARK_NAME = "watermark.png";

    public static String FFMPEG_PATH_EXTERNAL;
    public static String FFPROBE_PATH_EXTERNAL;
    public static String EXTERNAL_WATERMARK_PATH;


    /**
     * Extrahuje FFMPEG z JARu do dočasného souboru na disku.
     * @param resourceName název zdroje v JARu
     * @param outputFile soubor, kam se má zdroj dočasně zkopírovat
     * @throws Exception pokud selhalo kopírování nebo se nenašel zdroj
     */
    private static void extractResource(String resourceName, File outputFile) throws Exception {
        InputStream inputStream = Main.class.getClassLoader().getResourceAsStream(resourceName);
        if (inputStream == null) {
            throw new Exception("Zdroj " + resourceName + " nebyl nalezen uvnitř JARu");
        }

        Files.copy(inputStream, outputFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        inputStream.close();
    }

    /**
     * Vytvoří dočasné soubory pro FFMPEG a watermark a extrahuje je
     * @throws Exception pokud selže vytvoření dočasného souboru
     */
    private static void prepareResources() throws Exception {
        File FFmpegTemp = File.createTempFile("ffmpeg", ".exe");
        FFmpegTemp.deleteOnExit();
        File fFprobeTemp = File.createTempFile("ffprobe", ".exe");
        fFprobeTemp.deleteOnExit();
        File watermarkTemp = File.createTempFile("watermark", ".png");
        watermarkTemp.deleteOnExit();

        extractResource(FFMPEG_BIN_NAME, FFmpegTemp);
        extractResource(FFPROBE_BIN_NAME, fFprobeTemp);
        extractResource(INTERNAL_WATERMARK_NAME, watermarkTemp);

        FFMPEG_PATH_EXTERNAL = FFmpegTemp.getAbsolutePath();
        FFPROBE_PATH_EXTERNAL = fFprobeTemp.getAbsolutePath();
        EXTERNAL_WATERMARK_PATH = watermarkTemp.getAbsolutePath();
    }

    public static void main(String[] args) {
        Lock logLock = new ReentrantLock();
        BlockingQueue<TaskData> encodingQueue = new ArrayBlockingQueue<>(QUEUE_CAPACITY);
        BlockingQueue<TaskData> finishedQueue = new ArrayBlockingQueue<>(QUEUE_CAPACITY);

        Scanner scanner = new Scanner(System.in);
        System.out.println("Zadejte cestu ke složce s videi:");
        String inputPath = scanner.nextLine();

        System.out.println("Vyberte akci: 1.) WATERMARK, 2.) TRIM, 3.) KONVERZE:");
        int actionChoice = scanner.nextInt();
        scanner.nextLine();

        String start= "00:00:00";
        String duration = "00:00:00";
        String format = "mp4";
        OperationType action  = null;
        ProducerAnalyser producerAnalyser = null;
        switch (actionChoice) {
            case 1:
                action = OperationType.ADD_WATERMARK;
                producerAnalyser = new ProducerAnalyser(encodingQueue, logLock,inputPath, action,format,null,null);
                break;
            case 2:
                action = OperationType.TRIM;
                System.out.println("Zadejte start formát HH:MM:SS:");
                start = scanner.nextLine();
                System.out.println("Zadejte délku formát HH:MM:SS:");
                duration = scanner.nextLine();
                producerAnalyser = new ProducerAnalyser(encodingQueue, logLock,inputPath, action,format,start,duration);
                break;
            case 3:
                action = OperationType.CONVERT_FORMAT;
                System.out.println("Zadejte formát:");
                format = scanner.nextLine();
                producerAnalyser = new ProducerAnalyser(encodingQueue, logLock,inputPath, action,format,null,null);
                break;
            default: System.err.println("Neplatná volba");
        }
        scanner.close();

        try {
            prepareResources();
        } catch (Exception e) {
            System.err.println("Chyba při připravování binárek: " + e.getMessage());
            return;
        }

        System.out.println("Start");
        long startTime = System.nanoTime();
        new File(OUTPUT_DIR).mkdirs();

        ExecutorService producerService = Executors.newSingleThreadExecutor();
        producerService.execute(producerAnalyser);

        ExecutorService consumerService = Executors.newFixedThreadPool(NUM_ENCODER_THREADS);
        for (int i = 0; i < NUM_ENCODER_THREADS; i++) {
            consumerService.execute(new ConsumerEncoder(i + 1, encodingQueue, finishedQueue, logLock));
        }

        ExecutorService reporterService = Executors.newSingleThreadExecutor();
        reporterService.execute(new Reporter(finishedQueue, logLock));

        producerService.shutdown();
        consumerService.shutdown();
        reporterService.shutdown();

        try {
            consumerService.awaitTermination(1, TimeUnit.HOURS);
            reporterService.awaitTermination(10, TimeUnit.MINUTES);
            long endTime = System.nanoTime();
            double durationSeconds = (double) (endTime - startTime) / 1_000_000_000.0;
            System.out.println("Zpracování dokončeno, videa jsou ve složce: " + OUTPUT_DIR );
            System.out.print("Celková doba zpracování: "+ durationSeconds+"s");
        }
        catch (InterruptedException e) {
            System.err.println("Proces byl přerušen: " + e.getMessage());
        }
    }
}