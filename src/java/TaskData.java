/**
 * třída pro úchovu dat, které se vkládají do queue
 */
public class TaskData {
    private final OperationType operation;
    private final String inputPath;
    private final String outputPath;
    private final String trimStart;
    private final String trimDuration;
    private final String format;
    private final boolean isPoisonPill;

    /**
     * konstruktor pro TaskData
     * @param operation typ operace, která se bude provádět
     * @param inputPath cesta k souboru
     * @param outputPath cesta kam se má uložit
     * @param trimStart začátek, odkud se má video odstřihnout
     * @param trimDuration doba, jakou má video trvat
     * @param format fromát, ve kterém se má video uložit
     */
    public TaskData(OperationType operation, String inputPath, String outputPath, String trimStart, String trimDuration, String format) {
        this.operation = operation;
        this.inputPath = inputPath;
        this.outputPath = outputPath;
        this.trimStart = trimStart;
        this.trimDuration = trimDuration;
        this.format = format;
        this.isPoisonPill = false;
    }


    public TaskData() {
        this.operation = null;
        this.inputPath = null;
        this.outputPath = null;
        this.trimStart = null;
        this.trimDuration = null;
        this.format = null;
        this.isPoisonPill = true;
    }

    public OperationType getOperation() {
        return operation;
    }

    public String getInputPath() {
        return inputPath;
    }

    public String getOutputPath() {
        return outputPath;
    }

    public String getTrimStart() {
        return trimStart;
    }

    public String getTrimDuration() {
        return trimDuration;
    }

    public boolean isPoisonPill() {
        return isPoisonPill;
    }

    @Override
    public String toString() {
        return inputPath != null ? new java.io.File(inputPath).getName() : "POISON_PILL";
    }
}