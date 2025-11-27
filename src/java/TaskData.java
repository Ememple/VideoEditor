/**
 * třída pro úchovu dat, které se vkládají do queue
 */
public class TaskData {
    private final String inputPath;
    private final String outputPath;
    private final boolean isPoisonPill;

    /**
     * konstruktor pro třídu TaskData
     * @param inputPath cesta k vstupní složce
     * @param outputPath cesta k výstupní složce
     */
    public TaskData(String inputPath, String outputPath) {
        this.inputPath = inputPath;
        this.outputPath = outputPath;
        this.isPoisonPill = false;
    }

    /**
     * konstruktor pro třídu TaskData
     */
    public TaskData() {
        this.inputPath = null;
        this.outputPath = null;
        this.isPoisonPill = true;
    }

    public String getInputPath() {
        return inputPath;
    }

    public String getOutputPath() {
        return outputPath;
    }

    public boolean isPoisonPill() {
        return isPoisonPill;
    }

    @Override
    public String toString() {
        return inputPath != null ? new java.io.File(inputPath).getName() : "POISON_PILL";
    }
}