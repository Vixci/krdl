package vu.kr;

import openllet.owlapi.OpenlletReasoner;
import openllet.owlapi.OpenlletReasonerFactory;
import openllet.owlapi.explanation.PelletExplanation;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.NodeSet;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Set;

public class App {

    public static OWLOntologyManager ontManager;

    public static void main1(String[] args) throws OWLException, InstantiationException, IllegalAccessException,
            ClassNotFoundException, IOException {

        // We first need to obtain a copy of an OWLOntologyManager, which, as the name suggests, manages a set of ontologies.
        ontManager = OWLManager.createOWLOntologyManager();

        String inputFunction = args[0].toLowerCase();
        String ontologyPath = args[1];
        String subsumptionPath = "";

        System.out.println("Starting Program...");
        System.out.println("--------------------");


        switch (inputFunction) {
            case "printallsubclasses":
                printAllSubClasses(ontologyPath);
                break;
            case "saveallsubclasses":
                saveAllSubClasses(ontologyPath);
                break;
            case "printallexplanations":
                subsumptionPath = args[2];
                getAllSubsumptionExplanations(ontologyPath, subsumptionPath, false);
                break;
            case "saveallexplanations":
                subsumptionPath = args[2];
                getAllSubsumptionExplanations(ontologyPath, subsumptionPath, true);
                break;
            default:
                System.out.println("Please choose one of the following functions as first input of the program: "
                        + "[printAllSubClasses, saveAllSubClasses, printAllExplanations, saveAllExplanations]" );
        }

    }

    public static OWLOntology loadOntologyFromPath(String ontologyPath) {
        try {
            System.out.println("Loading ontology located at: " + ontologyPath);
            File ontologyDocument = new File(ontologyPath);
            OWLOntology ontology = ontManager.loadOntologyFromOntologyDocument(ontologyDocument);
            System.out.println("DONE! Loading ontology located at: " + ontologyPath);
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


    @SuppressWarnings("deprecation")
    public static void printAllSubClasses(String ontologyPath) throws OWLOntologyCreationException {
        OWLOntology ontology = loadOntologyFromPath(ontologyPath);
        OpenlletReasoner reasoner = OpenlletReasonerFactory.getInstance().createReasoner(ontology);

        System.out.println("Printing all subClassOf statements to console");
        System.out.println("\n");
        for (final OWLSubClassOfAxiom subClassStatement : ontology.getAxioms(AxiomType.SUBCLASS_OF)){
            if (subClassStatement.getSuperClass() instanceof OWLClass){
                NodeSet<OWLClass> superClasses = reasoner.getSuperClasses(subClassStatement.getSubClass(), false);
                for (OWLClass superClass : superClasses.getFlattened()) {
                    if(!superClass.isOWLThing() &&  superClass instanceof OWLClass) {
                        System.out.println(subClassStatement.getSubClass() + " rdfs:subClassOf " + superClass);
                    }
                }
            }
        }
        System.out.println("\n");
        System.out.println("DONE! Printing all subClassOf statements to console");
        System.out.println("--------");
        System.out.println("\n");
    }


    @SuppressWarnings("deprecation")
    public static void saveAllSubClasses(String ontologyPath) throws IOException, OWLOntologyCreationException {
        String parentDir = new File(ontologyPath).getParent(); // parent directory of the ontology location
        File subClassFile = new File(parentDir+"/subClasses.nt");
        subClassFile.createNewFile();
        OWLOntology ontology = loadOntologyFromPath(ontologyPath);
        OpenlletReasoner reasoner = OpenlletReasonerFactory.getInstance().createReasoner(ontology);
        System.out.println("Saving all subClassOf statements to following file: " + subClassFile.getAbsolutePath());
        System.out.println("\n");
        FileOutputStream fos = new FileOutputStream(subClassFile, false);
        for (final OWLSubClassOfAxiom subClassStatement : ontology.getAxioms(AxiomType.SUBCLASS_OF)){
            if (subClassStatement.getSuperClass() instanceof OWLClass){
                NodeSet<OWLClass> superClasses = reasoner.getSuperClasses(subClassStatement.getSubClass(), false);
                for (OWLClass superClass : superClasses.getFlattened()) {
                    if(!superClass.isOWLThing() &&  superClass instanceof OWLClass) {
                        String triple = subClassStatement.getSubClass() + " <http://www.w3.org/2000/01/rdf-schema#subClassOf>  " + superClass + " .";
                        fos.write(triple.getBytes());
                        fos.write(System.lineSeparator().getBytes());
                    }
                }
            }
        }
        System.out.println("DONE! Saving all subClassOf statements to following file: " + subClassFile.getAbsolutePath());
        System.out.println("--------");
        System.out.println("\n");
        fos.close();
    }


    @SuppressWarnings("deprecation")
    public static void getAllSubsumptionExplanations(String ontologyPath, String subsumptionPath, Boolean save) throws IOException {
        String parentDir = new File(ontologyPath).getParent(); // parent directory of the ontology location

        OWLOntology ontology = loadOntologyFromPath(ontologyPath);
        OWLOntology subsumptions = loadOntologyFromPath(subsumptionPath);

        String messageAction = "Printing";
        if(save == true) {
            messageAction = "Saving";
        }
        System.out.println(messageAction + " all explanations for the subsumptions located at: " + subsumptionPath);
        System.out.println("\n");

        // Starting up the Pellet Explanation module.
        PelletExplanation.setup();

        // Create the reasoner and load the ontology with the open pellet reasoner.
        OpenlletReasoner reasoner = OpenlletReasonerFactory.getInstance().createReasoner(ontology);

        // Create an Explanation reasoner with the Pellet Explanation and the Openllet Reasoner modules.
        PelletExplanation explanationsGenerator = new PelletExplanation(reasoner);

        int counter = 0;
        // For each subClassOf statement get all its explanations from the ontology
        for (final OWLSubClassOfAxiom subClassStatement : subsumptions.getAxioms(AxiomType.SUBCLASS_OF)){
            counter ++ ;
            if(save == true) {
                saveExplanationsOfAxiom(explanationsGenerator, subClassStatement.getSubClass(), subClassStatement.getSuperClass(), counter, parentDir);
            } else {
                printExplanationsOfAxiom(explanationsGenerator, subClassStatement.getSubClass(), subClassStatement.getSuperClass(), counter);
            }
        }
        System.out.println("\n");
        System.out.println("DONE! " + messageAction + " all explanations for subsumptions located at: " + subsumptionPath);
        System.out.println("--------");
        System.out.println("\n");
    }


    public static void printExplanationsOfAxiom(PelletExplanation explanationsGenerator, OWLClassExpression subClass, OWLClassExpression superClass, int explanationID){
        System.out.println("\nComputing explanation for: " + subClass + " rdfs:subClassOf " + superClass);
        Set<Set<OWLAxiom>> explanations = explanationsGenerator.getSubClassExplanations(subClass, superClass);
        for(Set<OWLAxiom> Explanation: explanations) {
            int counterExp = 0;
            System.out.println("-> Explanation: #" + explanationID );
            for (OWLAxiom rule : Explanation) {
                counterExp++;
                System.out.println("\t Axiom " + counterExp + ". " + rule.toString());
            }
        }
    }



    public static void saveExplanationsOfAxiom(PelletExplanation explanationsGenerator, OWLClassExpression subClass, OWLClassExpression superClass, int explanationID, String dirPath) throws IOException{
        System.out.println("\nComputing explanation for: " + subClass + " rdfs:subClassOf " + superClass);
        Set<Set<OWLAxiom>> explanations = explanationsGenerator.getSubClassExplanations(subClass, superClass);
        for(Set<OWLAxiom> Explanation: explanations) {
            int counterRule = 0;
            System.out.println("-> Explanation #" + explanationID );
            String fileName = "exp-" + explanationID + ".owl";
            File explanationFile = new File(dirPath+"/" + fileName);
            explanationFile.createNewFile();
            FileOutputStream fos = new FileOutputStream(explanationFile, false);
            for (OWLAxiom rule : Explanation) {
                counterRule++;
                System.out.println("\t Axiom " + counterRule + ". " + rule.toString());
                fos.write(rule.toString().getBytes());
                fos.write(System.lineSeparator().getBytes());
            }
            fos.close();
        }
    }


}

