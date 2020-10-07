package vu.kr;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import uk.ac.man.cs.lethe.forgetting.AlcOntologyForgetter;
import uk.ac.man.cs.lethe.forgetting.AlchTBoxForgetter;
import uk.ac.man.cs.lethe.forgetting.IOWLForgetter;
import uk.ac.man.cs.lethe.forgetting.ShqTBoxForgetter;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ExplainByForgetProvider {
    private IOWLForgetter forgetter;
    private OntologyInspector ontologyInspector;

    public ExplainByForgetProvider(int forgettingMethod, OntologyInspector ontologyInspector) {
        this.ontologyInspector = ontologyInspector;

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
        // TODO(Vixci): design strategy for forgetting: split the initial set into a list of sets representing the order of
        // forgetting
        // This is a random one-by-one strategy
        List<Set<OWLEntity>> strategy = toBeForgotten
                .stream()
                .map(owlEntity -> Sets.newHashSet(owlEntity))
                .collect(Collectors.toList());
        return strategy;
    }


    public void explainByForgetting(OWLSubClassOfAxiom subsumption) {
        Set<Set<OWLAxiom>> justifications = ontologyInspector.getAllJustifications().get(subsumption);
        for(Set<OWLAxiom> justification : justifications) {
            OntologyInspector justificationInspector = new OntologyInspector(justification);
            Set<OWLEntity> toBeForgotten = justificationInspector.getAllEntities();
            toBeForgotten.removeAll(subsumption.signature().collect(Collectors.toSet()));
            List<Set<OWLEntity>> strategy = getStrategy(toBeForgotten);
            for (Set<OWLEntity> batch : strategy) {
                OWLOntology newOntology = forgetter.forget(justificationInspector.getOntology(), batch);
                // TODO(vixci): output the new ontology
                System.out.println(newOntology);
            }
        }

    }

    public void explainAllByForgetting () {
        ontologyInspector.calculateAllJustifications();
        for (OWLSubClassOfAxiom subsumption : ontologyInspector.getAllSubsumptions()) {
            explainByForgetting(subsumption);
        };
    }
}
