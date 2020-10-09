package vu.kr;

import com.google.common.collect.Sets;
import org.semanticweb.owlapi.model.*;
import uk.ac.man.cs.lethe.forgetting.AlcOntologyForgetter;
import uk.ac.man.cs.lethe.forgetting.AlchTBoxForgetter;
import uk.ac.man.cs.lethe.forgetting.IOWLForgetter;
import uk.ac.man.cs.lethe.forgetting.ShqTBoxForgetter;
import uk.ac.man.cs.lethe.internal.tools.formatting.SimpleOWLFormatter;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ExplainByForgetProvider {
    private IOWLForgetter forgetter;
    private OntologyInspector subsumptionsInspector;
    private String justificationsDirPath;
    private int forgettingStrategy;

    public ExplainByForgetProvider(int forgettingMethod,
                                   int forgettingStrategy,
                                   OntologyInspector subsumptionsInspector,
                                   String justificationsDirPath) {
        this.subsumptionsInspector = subsumptionsInspector;
        this.justificationsDirPath = justificationsDirPath;
        this.forgettingStrategy = forgettingStrategy;

        switch (forgettingMethod) {
            case 1:
                forgetter = new AlchTBoxForgetter();
                break;
            case 2:
                forgetter = new ShqTBoxForgetter();
                break;
            case 3:
                forgetter = new AlcOntologyForgetter();
                break;
            default:
                throw new UnsupportedOperationException("Incorrect forgetting type. Allowed values: 1, 2 or 3.");
        }
    }


    private List<Set<OWLEntity>> getStrategy(Set<OWLEntity> toBeForgotten) {
        List<Set<OWLEntity>> strategy = null;
        switch (forgettingStrategy) {
            case 1:
                // This is a random order one-by-one forgetting strategy
                strategy = toBeForgotten
                        .stream()
                        .map(owlEntity -> Sets.newHashSet(owlEntity))
                        .collect(Collectors.toList());
                break;
            // TODO(Vixci): design other forgetting strategy: split the initial set into a list of sets representing the
            //  order to forget
            default:
                throw new UnsupportedOperationException("Forgetting strategy not supported");
        }
        return strategy;
    }


    /**
     * Forgets all the symbols in the justification with the aim to explain it better.
     */
    public void explainByForgetting(OWLSubClassOfAxiom subsumption, int index) throws IOException {
        List<Path> justificationFilePaths = null;
        try {
            justificationFilePaths = getJustificationFilePaths(index);
        } catch (IOException e) {
            System.err.println("Error locating justifications for subsumption #" + index + ": " + subsumption);
            return;
        }

        for(Path justificationFilePath : justificationFilePaths) {
            File explanationsFile = getExplanationsExportFile(justificationFilePath);
            OntologyInspector justificationInspector = new OntologyInspector(justificationFilePath.toString());
            // Create the explanation export file and put the initial justifications in
            exportToExplanations(explanationsFile, false, SimpleOWLFormatter.format(justificationInspector.getOntology()));
            Set<OWLEntity> toBeForgotten = justificationInspector.getAllEntities();
            toBeForgotten.removeAll(subsumption.signature().collect(Collectors.toSet()));
            System.out.println("Initial entities to forget: " + toBeForgotten);
            List<Set<OWLEntity>> strategy = getStrategy(toBeForgotten);
            for (Set<OWLEntity> batch : strategy) {
                OWLOntology newOntology = forgetter.forget(justificationInspector.getOntology(), batch);
                justificationInspector.setOntology(newOntology);
                exportToExplanations(explanationsFile, true, SimpleOWLFormatter.format(newOntology));
            }
        }
    }

    private File getExplanationsExportFile(Path justificationsFilePath) {
        String fileName = justificationsFilePath.getFileName().toString().replace("justif", "expl" + forgettingStrategy);
        return new File(justificationsDirPath + File.separator + fileName);
    }
    private void exportToExplanations(File explanationFile, boolean append, String partial) throws IOException {
        FileOutputStream fos = new FileOutputStream(explanationFile, append);
        fos.write(partial.getBytes());
        fos.write(("\n" + "-".repeat(100) + "\n").getBytes());
    }

    /**
     * Retrieve the list of paths where the justifications for the subsumption with index 'index'
     */
    public List<Path> getJustificationFilePaths(int index) throws IOException {
        PathMatcher pathMatcher = FileSystems.getDefault()
                .getPathMatcher("glob:**justif-" + index + "-*.owl");
        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(
                new File(justificationsDirPath).toPath(), pathMatcher::matches)) {
            List<Path> paths = new ArrayList<>();
            for (Path path : dirStream) {
                paths.add(path);
            }
            return paths;
        }
    }

    /**
     * Explains all the subsumptions' justifications using forgetting.
     */
    public void explainAllByForgetting() {
        List<OWLSubClassOfAxiom> subsumptions = subsumptionsInspector.getDirectSubsumptions();
        int index = 0;
        for (OWLSubClassOfAxiom subsumption : subsumptions) {
            System.out.println("-> Explaining subsumption #" + index + ": " + SimpleOWLFormatter.format(subsumption));
            try {
                explainByForgetting(subsumption, index++);
            } catch (IOException e) {
                System.err.println("Subsumption #" + (index - 1) + " explanation failed. Attemting next subsumption..." );
                e.printStackTrace();
                break;
            }
            System.out.println("---------------------------------------------------");
            // TODO(Vixci) remove this line after debugging
//            if (index > 1) break;
        }
    }
}
