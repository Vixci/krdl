package vu.kr;

import com.google.common.collect.Comparators;
import openllet.owlapi.OpenlletReasoner;
import openllet.owlapi.OpenlletReasonerFactory;
import openllet.owlapi.explanation.PelletExplanation;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.reasoner.NodeSet;
import uk.ac.manchester.cs.owl.owlapi.OWLClassExpressionImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLSubClassOfAxiomImpl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Loads an ontology from a file and provides utility methods to deal with the subsumptions and justifications.
 */
public class OntologyInspector {
    private String ontologyExportPath;
    private OWLOntology ontology;
//    private Map<OWLSubClassOfAxiom, Set<Set<OWLAxiom>>> allJustifications;

    public OntologyInspector(String ontologyFile) {
        File file = new File(ontologyFile);
        ontologyExportPath = calculateExportPath(
                file.getAbsolutePath().substring(0, file.getAbsolutePath().lastIndexOf(File.separator)),
                file.getName().split("\\.")[0]);
        setOntology(loadOntologyFromPath(ontologyFile));
    }
    public OntologyInspector(OWLOntology ontology) {
        setOntology(ontology);
    }

    public OntologyInspector(Set<OWLAxiom> axioms) {
        OWLOntologyManager ontManager = OWLManager.createOWLOntologyManager();
        try {
            ontology = ontManager.createOntology(axioms);
        } catch (OWLOntologyCreationException e) {
            System.out.println("ERROR creating ontology from axioms ");
            e.printStackTrace();
        }
    }

    private OWLOntology loadOntologyFromPath(String ontologyPath) {
        try {
            OWLOntologyManager ontManager = OWLManager.createOWLOntologyManager();
            System.out.println("Loading ontology located at: " + ontologyPath + "...");
            File ontologyDocument = new File(ontologyPath);
            OWLOntology ontology = ontManager.loadOntologyFromOntologyDocument(ontologyDocument);
            System.out.println("DONE - Loaded ontology located at: " + ontologyPath);
            System.out.println("Format : " + ontManager.getOntologyFormat(ontology));
            System.out.println("--------");
            System.out.println("\n");
            
            return ontology;
        } catch (OWLOntologyCreationException e) {
            System.out.println("ERROR loading ontology located at: " + ontologyPath);
            System.out.println("--------");
            System.out.println("\n");
            e.printStackTrace();
            return null;
        }
    }

    public Set<OWLEntity> getAllEntities() {
        return ontology.signature().collect(Collectors.toSet());
    }

    /**
     * Calculate direct and implicit subsumptions from the ontology.
     *
     * @return A set of axioms representing the direct and indirect subsumptions
     */
    public List<OWLSubClassOfAxiom> calculateAllSubsumptions() {
        List<OWLSubClassOfAxiom> directSubsumptions = getDirectSubsumptions();
        List<OWLSubClassOfAxiom> allSubsumptions = new ArrayList<>();
        for (OWLSubClassOfAxiom axiom: directSubsumptions) {
            allSubsumptions.addAll(getInferredSubsumptions(axiom));
        }
        return allSubsumptions.stream().distinct().sorted().collect(Collectors.toList());
    }

    public List<OWLSubClassOfAxiom> getDirectSubsumptions() {
        return ontology.tboxAxioms(Imports.EXCLUDED)
                .filter(axiom -> axiom.isOfType(AxiomType.SUBCLASS_OF))
                .map(axiom -> (OWLSubClassOfAxiom) axiom)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    private List<OWLSubClassOfAxiom> getInferredSubsumptions(OWLSubClassOfAxiom subsumption) {
        OpenlletReasoner reasoner = OpenlletReasonerFactory.getInstance().createReasoner(ontology);
        NodeSet<OWLClass> superClasses = reasoner.getSuperClasses(subsumption.getSubClass(), false);
        return superClasses.entities()
                .filter(s -> !s.isOWLThing() && s instanceof OWLClass)
                .map(s -> new OWLSubClassOfAxiomImpl(subsumption.getSubClass(), s, /* annotations= */ Collections.EMPTY_LIST))
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * Calculates all justifications for all direct and implicit subsumptions from the ontology.
     * The justifications are stored in a map keyed by the sybsumption.
     * For each subsumption we associate a set of justifications.
     */
    public Map<OWLSubClassOfAxiom, Set<Set<OWLAxiom>>> calculateAllJustifications() {
        List<OWLSubClassOfAxiom> allSubsumptions = calculateAllSubsumptions();
        // Only call this method for small ontologies, for big ones we cannot load everything in memory.
        assert(allSubsumptions.size() < 20);
        // Starting up the Pellet Explanation module.
        PelletExplanation.setup();

        OpenlletReasoner reasoner = OpenlletReasonerFactory.getInstance().createReasoner(ontology);

        // Create an Explanation reasoner with the Pellet Explanation and the Openllet Reasoner modules.
        PelletExplanation explanationsGenerator = new PelletExplanation(reasoner);

        Map<OWLSubClassOfAxiom, Set<Set<OWLAxiom>>> allJustifications = new HashMap<>();

        // For each subClassOf statement get all its explanations from the ontology
        for (final OWLSubClassOfAxiom subClassStatement : allSubsumptions) {
            Set<Set<OWLAxiom>> explanations = explanationsGenerator.getSubClassExplanations(
                    subClassStatement.getSubClass(), subClassStatement.getSuperClass());
            allJustifications.put(subClassStatement, explanations);
        }
        return allJustifications;
    }



    /**
     * Exports all direct and implicit subsumptions in the ontology to a file.
     *
     * @throws IOException if the file cannot be written
     */
    public void exportAllSubsumptions() throws IOException {
        createExportDir();
        List<OWLSubClassOfAxiom> allSubsumptions = calculateAllSubsumptions();

        File subClassFile = new File(ontologyExportPath + File.separator + "subClasses.nt");
        subClassFile.createNewFile();
        System.out.println("Saving all subClassOf statements to following file: " + subClassFile.getAbsolutePath());
        System.out.println("\n");
        FileOutputStream fos = new FileOutputStream(subClassFile, false);
        for (final OWLSubClassOfAxiom subClassStatement : allSubsumptions) {
                String triple = subClassStatement.getSubClass() + " <http://www.w3.org/2000/01/rdf-schema#subClassOf>  "
                        + subClassStatement.getSuperClass() + " .";
                fos.write(triple.getBytes());
                fos.write(System.lineSeparator().getBytes());
        }
        System.out.println("DONE - Saved all subClassOf statements to following file: " + subClassFile.getAbsolutePath());
        System.out.println("--------");
        System.out.println("\n");
        fos.close();
    }

    /**
     * Exports the justifications for one given subsumption. Each justification is exported in a separate owl file,
     * named exp-<subsumptionId>-<justificationId>.owl
     *
     * @param subsumption the subsumption to be justified
     * @param subsumptionId the ID of the subsumption, which is used to identify the explanation in the filename.
     *
     */
    private void exportJustifications(Set<Set<OWLAxiom>> justifications, OWLSubClassOfAxiom subsumption,  int subsumptionId) {
        System.out.println("\nExporting explanation for: " + subsumption.getSubClass() + " rdfs:subClassOf " + subsumption.getSuperClass());

        int counter = 0;
        for(Set<OWLAxiom> justification: justifications) {
            System.out.println("-> Subsumption #" + subsumptionId + ": " + subsumption);
            System.out.println("-> #" + subsumptionId + " Justification #" + counter);
            String fileName = "justif-" + subsumptionId + "-" + (counter++) + ".owl";
            exportOneJustification(justification, fileName);
        }
    }

    private void exportOneJustification(Set<OWLAxiom> justification, String fileName) {
        File justificationsFile = new File(ontologyExportPath + File.separator + fileName);
        OWLOntologyManager ontManager = OWLManager.createOWLOntologyManager();
        try {
            OWLOntology justificationOntology = ontManager.createOntology(justification);
            //OWLDocumentFormat format = ontManager.getOntologyFormat(justificationOntology);
            ontManager.saveOntology(justificationOntology, IRI.create(justificationsFile.toURI()));
        } catch (OWLOntologyStorageException e) {
            e.printStackTrace();
        } catch (OWLOntologyCreationException e) {
            e.printStackTrace();
        }
    }

    /**
     * Exports justifications for all the subsumptions in the ontology. Each set of justifications is exported in a
     * separate exp-<SubsumptionId>-</><justificationId></>.owl file, e.g exp-0-0.owl, exp-0-1.owl etc
     *
     * @throws IOException
     */
    public void exportAllJustifications() throws IOException {
        createExportDir();
        OWLOntology subsumptionsOntology = loadOntologyFromPath(ontologyExportPath + File.separator + "subclasses.nt");

        List<OWLSubClassOfAxiom> subsumptions = subsumptionsOntology
                .tboxAxioms(Imports.EXCLUDED)
                .map(a -> (OWLSubClassOfAxiom) a)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        exportAllJustifications(subsumptions);
    }

    public void exportAllJustifications(List<OWLSubClassOfAxiom> subsumptions) throws IOException {
        int counter = 0;
        PelletExplanation.setup();

        OpenlletReasoner reasoner = OpenlletReasonerFactory.getInstance().createReasoner(ontology);

        // Create an Explanation reasoner with the Pellet Explanation and the Openllet Reasoner modules.
        PelletExplanation explanationsGenerator = new PelletExplanation(reasoner);

        // For each subClassOf statement get all its explanations from the ontology
        for (final OWLSubClassOfAxiom subsumption : subsumptions) {
            exportJustifications(
                    explanationsGenerator.getSubClassExplanations(
                            subsumption.getSubClass(), subsumption.getSuperClass()),
                    subsumption,
                    counter++);
        }
    }

    private String calculateExportPath(String ontologyPath, String ontologyName) {
        return ontologyPath + File.separator + ontologyName + "_exports";
    }

    private void createExportDir() {
        File dir = new File(ontologyExportPath);
        dir.mkdir();
    }

    public OWLOntology getOntology() {
        return ontology;
    }

    public void setOntology(OWLOntology ontology) {
        this.ontology = ontology;
    }

    public String getOntologyExportPath() {
        return ontologyExportPath;
    }
}
