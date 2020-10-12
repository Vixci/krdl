package vu.kr;

import com.google.common.collect.Sets;
import openllet.owlapi.OpenlletReasoner;
import openllet.owlapi.OpenlletReasonerFactory;
import openllet.owlapi.explanation.PelletExplanation;
import org.semanticweb.owlapi.model.*;

import java.io.File;
import java.util.Set;

public class DLExplanation {
    public static void main(String[] args) {
        String ontologyPath = args[0];
        OntologyInspector ontologyInspector = new OntologyInspector(ontologyPath);

        int option = Integer.parseInt(args[1]);
        switch (option) {
            case 1:
                try {
                    ontologyInspector.exportAllSubsumptions();
                } catch (Exception e) {
                    System.err.println("Error occurred while exporting subsumptions");
                }
                break;
            case 2:
                try {
                    ontologyInspector.exportAllJustifications();
                } catch (Exception e) {
                    System.err.println("Error occurred while exporting justifications");
                }
                break;
            case 3:
                int forgettingMethod = Integer.parseInt(args[2]);
                OntologyInspector subsumptionsInspector = new OntologyInspector(
                        ontologyInspector.getOntologyExportPath()
                        + File.separator + "/subclasses.nt");
                int forgettingStrategy = 2;
                ExplainByForgetProvider forgetProvider = new ExplainByForgetProvider(
                        forgettingMethod, forgettingStrategy, subsumptionsInspector,
                        ontologyInspector.getOntologyExportPath(), ontologyInspector.getOntologyName());
                forgetProvider.explainAllByForgetting();
                break;
        }
    }
}
