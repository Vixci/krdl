package vu.kr;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExplainByForgetProviderTest {
    private OntologyInspector ontologyInspector;
    private ExplainByForgetProvider forgetProvider;

    @BeforeEach
    void setUp() {
        ontologyInspector = new OntologyInspector("pizza.owl");
        forgetProvider = new ExplainByForgetProvider(2, 1, ontologyInspector, "");
    }

    @Test
    void explainByForgetting() {
    }

    @Test
    void explainAllByForgetting() {
        forgetProvider.explainAllByForgetting();
    }
}