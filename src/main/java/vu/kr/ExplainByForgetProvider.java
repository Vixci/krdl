package vu.kr;

import com.google.common.collect.Sets;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.Imports;
import uk.ac.man.cs.lethe.forgetting.AlcOntologyForgetter;
import uk.ac.man.cs.lethe.forgetting.AlchTBoxForgetter;
import uk.ac.man.cs.lethe.forgetting.IOWLForgetter;
import uk.ac.man.cs.lethe.forgetting.ShqTBoxForgetter;
import uk.ac.man.cs.lethe.internal.tools.formatting.SimpleOWLFormatter;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;

public class ExplainByForgetProvider {
    private IOWLForgetter forgetter;
    private OntologyInspector subsumptionsInspector;
    private String justificationsDirPath;
    private String explanationsDirPath;
    private int forgettingStrategy;
    private String ontologyName;
    private Metrics metrics;

    public ExplainByForgetProvider(int forgettingMethod,
                                   int forgettingStrategy,
                                   OntologyInspector subsumptionsInspector,
                                   String justificationsDirPath,
                                   String ontologyName) {
        this.subsumptionsInspector = subsumptionsInspector;
        this.forgettingStrategy = forgettingStrategy;
        this.justificationsDirPath = justificationsDirPath;
        this.ontologyName = ontologyName;

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
        this.explanationsDirPath = justificationsDirPath
                + File.separator
                + forgetter.getClass().getSimpleName();
        this.metrics = new Metrics(explanationsDirPath, forgettingStrategy);
    }


    private List<Set<OWLEntity>> getStrategy(Set<OWLEntity> toBeForgotten, OWLOntology justification) {
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
            case 2:


                Map<OWLEntity, Integer> occ = new HashMap<>();

                for (OWLLogicalAxiom axiom: justification.logicalAxioms().collect(Collectors.toList())) {
                    for (OWLEntity entity: toBeForgotten) {
                        if (axiom.containsEntityInSignature(entity)) {
                            if (occ.containsKey(entity)) {
                                occ.put(entity, occ.get(entity)+1);
                            }
                            else {
                                occ.put(entity, 1);
                            }
                        }
                    }
                }

                // Create a list from elements of HashMap
                List<Map.Entry<OWLEntity, Integer> > list =
                        new LinkedList<>(occ.entrySet());

                // Sort the list
                Collections.sort(list, new Comparator<Map.Entry<OWLEntity, Integer> >() {
                    public int compare(Map.Entry<OWLEntity, Integer> o1,
                                       Map.Entry<OWLEntity, Integer> o2)
                    {
                        return (o1.getValue()).compareTo(o2.getValue());
                    }
                });

                strategy = list.stream()
                        .map(entry -> Sets.newHashSet(entry.getKey()))
                        .collect(Collectors.toList());

                break;
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
        Set<OWLEntity> entitiesInSubsumption = subsumption.signature().collect(Collectors.toSet());

        for(Path justificationFilePath : justificationFilePaths) {
            File explanationsFile = getExplanationsExportFile(justificationFilePath);
            OntologyInspector justificationInspector = new OntologyInspector(justificationFilePath.toString());
            // Create the explanation export file and put the subsumption whose justification is explained
            exportToExplanations(explanationsFile, false, "EXPLAINING SUBSUMPTION: " + SimpleOWLFormatter.format(subsumption));
            // Export the initial justifications in the file
            exportToExplanations(explanationsFile, true, SimpleOWLFormatter.format(justificationInspector.getOntology()));
            Set<OWLEntity> toBeForgotten = justificationInspector.getAllEntities();
            int initialJustificationAxiomCount = justificationInspector.getOntology().getLogicalAxiomCount(Imports.EXCLUDED);
            toBeForgotten.removeAll(entitiesInSubsumption);
            System.out.println("Initial entities to forget: " + toBeForgotten);
            List<Set<OWLEntity>> strategy = getStrategy(toBeForgotten, justificationInspector.getOntology());
            boolean succesfulExplanation = false;
            int actualForgettingStepsDelta =  strategy.size();

            // If the justification is the conclusion itself, consider it a success.
            if (justificationInspector.getOntology().getLogicalAxiomCount(Imports.EXCLUDED) == 1
                    && justificationInspector.getOntology().containsAxiom(subsumption)) {
                succesfulExplanation = true;
            }

            int justificationIncreaseAmount = 0;
            int justificationSizePrev = initialJustificationAxiomCount;

            for (Set<OWLEntity> batch : strategy) {

                OWLOntology newOntology = forgetter.forget(justificationInspector.getOntology(), batch);

                if (newOntology.getLogicalAxiomCount() > justificationSizePrev) {
                    justificationIncreaseAmount += newOntology.getLogicalAxiomCount()  - justificationSizePrev;
                }
                justificationSizePrev = newOntology.getLogicalAxiomCount();

                Optional<String> batchString = batch.stream()
                        .map(e -> e.toString())
                        .map(e -> e.lastIndexOf("#") == -1? e: e.substring(e.lastIndexOf('#')+1, e.length() - 1) + ", ")
                        .reduce(String::concat);
                if (batchString.isPresent()) {
                    exportToExplanations(explanationsFile, true, "FORGETTING: " + batchString.orElse("Nothing to forget"));
                }

                justificationInspector.setOntology(newOntology);
                exportToExplanations(explanationsFile, true, SimpleOWLFormatter.format(newOntology));

                actualForgettingStepsDelta --;

                if (justificationInspector.getOntology().getLogicalAxiomCount(Imports.EXCLUDED) == 1
                        && justificationInspector.getOntology().containsAxiom(subsumption)) {
                    succesfulExplanation = true;
                    break;
                }
            }
            metrics.writeRow(ontologyName,
                    forgetter.getClass().getSimpleName(),
                    forgettingStrategy,
                    index,
                    entitiesInSubsumption.size(),
                    initialJustificationAxiomCount,
                    strategy.size(),
                    actualForgettingStepsDelta,
                    justificationIncreaseAmount,
                    succesfulExplanation);
        }
    }

    private File getExplanationsExportFile(Path justificationsFilePath) {
        String fileName = justificationsFilePath.getFileName().toString().replace("justif", "expl" + forgettingStrategy);
        return new File(explanationsDirPath + File.separator + fileName);
    }

    private void exportToExplanations(File explanationFile, boolean append, String partial) throws IOException {
        FileOutputStream fos = new FileOutputStream(explanationFile, append);
        fos.write(partial.getBytes());
        fos.write(("\n" + (append?"-":"*").repeat(100) + "\n").getBytes());
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
        createExportDir();
        try {
            metrics.writeHeader();
        } catch (IOException e) {
            System.err.println("Error exporting metrics header.");
            e.printStackTrace();
        }
        List<OWLSubClassOfAxiom> subsumptions = subsumptionsInspector.getDirectSubsumptions();
        int index = 0;
        for (OWLSubClassOfAxiom subsumption : subsumptions) {
            System.out.println("-> Explaining subsumption #" + index + ": " + SimpleOWLFormatter.format(subsumption));
            try {
                explainByForgetting(subsumption, index++);
            } catch (IOException e) {
                System.err.println("Subsumption #" + (index - 1) + " explanation failed. Attempting next subsumption..." );
                e.printStackTrace();
                break;
            }
            System.out.println("---------------------------------------------------");
            // TODO(Vixci) remove this line after debugging
            //if (index > 3) break;
        }
        metrics.close();
    }

    private void createExportDir() {
        File dir = new File(explanationsDirPath);
        dir.mkdir();
    }
}
