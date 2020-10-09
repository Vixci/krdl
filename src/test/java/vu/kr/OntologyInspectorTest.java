package vu.kr;

import com.google.common.collect.Sets;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import uk.ac.manchester.cs.owl.owlapi.OWLSubClassOfAxiomImpl;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class OntologyInspectorTest {

    private OntologyInspector ontologyInspector;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
         ontologyInspector = new OntologyInspector("dummy.owl");
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
    }


    @org.junit.jupiter.api.Test
    void exportAllJustifications() {
        ontologyInspector.calculateAllJustifications();
        try {
            ontologyInspector.exportAllJustifications();
        } catch (Exception e) {
            fail("Failed the exporting of justifications");
        }
    }

    @org.junit.jupiter.api.Test
    void calculateAllSubsumptions() {
        List<OWLSubClassOfAxiom> result = ontologyInspector.calculateAllSubsumptions();
        assertEquals(11, result.size());
        System.out.println(result);
    }

    @org.junit.jupiter.api.Test
    void getAllJustifications() {
        ontologyInspector.calculateAllJustifications();
        Map<OWLSubClassOfAxiom, Set<Set<OWLAxiom>>> result = ontologyInspector.calculateAllJustifications();
        assertEquals(11, result.size());
    }
}