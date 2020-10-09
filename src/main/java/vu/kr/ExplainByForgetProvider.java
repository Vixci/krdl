package vu.kr;

import com.google.common.collect.Sets;
import org.semanticweb.owlapi.model.*;
import uk.ac.man.cs.lethe.forgetting.AlcOntologyForgetter;
import uk.ac.man.cs.lethe.forgetting.AlchTBoxForgetter;
import uk.ac.man.cs.lethe.forgetting.IOWLForgetter;
import uk.ac.man.cs.lethe.forgetting.ShqTBoxForgetter;

import java.awt.desktop.SystemEventListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ExplainByForgetProvider {
    private IOWLForgetter forgetter;
    private OntologyInspector subsumptionsInspector;
    private String justificationsPath;

    public ExplainByForgetProvider(int forgettingMethod, OntologyInspector subsumptionsInspector, String justificationsPath) {
        this.subsumptionsInspector = subsumptionsInspector;
        this.justificationsPath = justificationsPath;

        switch (forgettingMethod) {
            case 1:
                forgetter = new ShqTBoxForgetter();
                break;
            case 2:
                forgetter = new AlchTBoxForgetter();
                break;
            case 3:
                forgetter = new AlcOntologyForgetter();
                break;
            default:
                throw new UnsupportedOperationException("Incorrect forgetting type. Allowed values: 1, 2 or 3.");
        }
    }


    private List<Set<OWLEntity>> getStrategy(Set<OWLEntity> toBeForgotten) {
        // TODO(Vixci): design forgetting strategy: split the initial set into a list of sets representing the order
        // to forget

        // This is a random order one-by-one forgetting strategy
        List<Set<OWLEntity>> strategy = toBeForgotten
                .stream()
                .map(owlEntity -> Sets.newHashSet(owlEntity))
                .collect(Collectors.toList());
        return strategy;
    }


    /**
     * Forgets all the symbols in the justification with the aim to explain it better.
     */
    public void explainByForgetting(OWLSubClassOfAxiom subsumption, int index) {
        List<Path> justificationFilePaths = getJustificationFilePaths(index);
        System.out.println(subsumption);
        for(Path justificationFilePath : justificationFilePaths) {
            System.out.println(justificationFilePath);
            OntologyInspector justificationInspector = new OntologyInspector(justificationFilePath.toString());
            Set<OWLEntity> toBeForgotten = justificationInspector.getAllEntities();
            System.out.println(toBeForgotten);
            toBeForgotten.removeAll(subsumption.signature().collect(Collectors.toSet()));
            List<Set<OWLEntity>> strategy = getStrategy(toBeForgotten);
            for (Set<OWLEntity> batch : strategy) {
                OWLOntology newOntology = forgetter.forget(justificationInspector.getOntology(), batch);
                justificationInspector.setOntology(newOntology);
                // TODO(Vixci): Replace the ontology to forget from with the new one.
                // TODO(Vixci): Output the new ontology
                System.out.println(newOntology);
            }
        }

    }

    /**
     * Retrieve the list of paths where the justifications for the subsumption with index 'index'
     */
    public List<Path> getJustificationFilePaths(int index) {
        PathMatcher pathMatcher = FileSystems.getDefault()
                .getPathMatcher("glob:**justif-"+index+"-*.owl");
        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(
                new File(justificationsPath).toPath(), pathMatcher::matches)) {
            List<Path> paths = new ArrayList<>();
            for (Path path : dirStream) {
                paths.add(path);
            }
            return paths;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Explains all the subsumptions' justifications using forgetting.
     */
    public void explainAllByForgetting () {
        List<OWLSubClassOfAxiom> subsumptions = subsumptionsInspector.getDirectSubsumptions();
        int index = 0;
        for (OWLSubClassOfAxiom subsumption : subsumptions) {
            System.out.println("-> Explaining subsumption #" + index + ": " + subsumption);
            explainByForgetting(subsumption, index++);
            System.out.println("---------------------------------------------------");
            // TODO(Vixci) remove this line after debugging
            if (index > 1) break;
        }
    }
}
