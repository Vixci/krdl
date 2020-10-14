package vu.kr;

import org.semanticweb.owlapi.model.OWLEntity;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Metrics {
    private static String[] CSV_HEADER = {
            "ONTOLOGY_NAME",
            "FORGETTING_METHOD",
            "FORGETTING_STRATEGY",
            "FORMULA_ID",
            "JUSTIFICATION_ID",
            "FORMULA_SIZE",
            "JUSTIFICATION_SIZE", /* number of axioms in tge initial justification */
            "STRATEGY_STEP_COUNT", /* number of batches in the forgetting strategy */
            "EXPLANATION_LENGTH", /* how many steps did the explanation take */
            "JUSTIFICATION_INCREASE_AMOUNT",
            "SUCCESSFUL_EXPLANATION"
    };

    private File csvOutputFile = null;
    private PrintWriter pw = null;


    public Metrics(String directory, int forgettingStrategy) {
       csvOutputFile = new File(directory + File.separator
               + "metrics"+forgettingStrategy+".csv");
    }

    public void writeHeader() throws IOException {
        csvOutputFile.createNewFile();
        pw = new PrintWriter(csvOutputFile);
        writeRow(CSV_HEADER);
        pw.flush();
    }

    public void writeRow(String ontologyName,
                         String forgetterType,
                         int forgettingStrategy,
                         int subsumptionIndex,
                         int justificationIndex,
                         int entitiesInSubsumptionSize,
                         int justificationSize,
                         int strategyStepCount,
                         int explanationLength,
                         int justificationIncreaseAmount,
                         boolean successfulExplanation) throws IOException {
        String[] row = new String[] {
                ontologyName,
                forgetterType,
                String.valueOf(forgettingStrategy),
                String.valueOf(subsumptionIndex),
                String.valueOf(justificationIndex),
                String.valueOf(entitiesInSubsumptionSize),
                String.valueOf(justificationSize),
                String.valueOf(strategyStepCount),
                String.valueOf(explanationLength),
                String.valueOf(justificationIncreaseAmount),
                String.valueOf(successfulExplanation?1:0)
        };
        writeRow(row);
        pw.flush();
    }

    private void writeRow(String[] row) throws IOException {
        assert(pw != null);
        pw.println(convertToCSV(row));
    }

    private String convertToCSV(String[] data) {
        return Stream.of(data)
                .map(this::escapeSpecialCharacters)
                .collect(Collectors.joining(","));
    }
    private String escapeSpecialCharacters(String data) {
        String escapedData = data.replaceAll("\\R", " ");
        if (data.contains(",") || data.contains("\"") || data.contains("'")) {
            data = data.replace("\"", "\"\"");
            escapedData = "\"" + data + "\"";
        }
        return escapedData;
    }

    public void close() {
        pw.flush();
        pw.close();
    }
}
