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
            "FORGETTING_STRATEGY",
            "FORMULA_ID",
            "FORMULA_SIZE",
            "JUSTIFICATION_SIZE",
            "STRATEGY_STEP_COUNT",
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
                         int entitiesInSubsumptionSize,
                         int justificationSize,
                         int strategyStepCount,
                         boolean successfulExplanation) throws IOException {
        String[] row = new String[] {
                ontologyName,
                forgetterType,
                String.valueOf(forgettingStrategy),
                String.valueOf(subsumptionIndex),
                String.valueOf(entitiesInSubsumptionSize),
                String.valueOf(justificationSize),
                String.valueOf(strategyStepCount),
                String.valueOf(successfulExplanation)
        };
        writeRow(row);
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
