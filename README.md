# krdl
## Ontology Justification Explanations Using the Forgetting Operation

This project facilitates explanation for subsumption justification in ontologies using the forgetting operation.

The justifications are generated by OWL-API. For each of the justificaiton we apply forgetting using the LETHE library, with the aim to make the justifications more understandable.

All the results are exported in a directory called `<ontology_name>_exports` where `<ontology_name>` is the initial ontology.

The projects experiments with various heuristics for forgetting: 

1. Random 1-by-1, in which symbols are forgotten 1 by 1 in random order
2. 1-by-1 in increasing order of frequency of occurence in the axioms in the initial justification (less frequent first).
3. 1-by-1 in decreasing order of frequency of occurence in the axioms in the initial justification (more frequent first).
4. In batches of the same frequency at once, in increasing order of frequency of occurence in the axioms in the initial justification (less frequent first)
5. In batches by class expression type, i.e. the symbols occurring only in simple axioms without logical operations or property restrictions first, then symbols occuring in logical operations, then symbols occuring only in more complex axioms with property restrictions.

### Running the tool

The tool needs at least JRE 11 to run.

The main class to run is DLExplanation. 

To run the program, use the following command line options:

1. The ontology file (required)
2. Either of the options 1, 2 or 3:
  - Option 1 - to generate all the direct and inferred subsumptions. 
  The results are exported in a file called `subclasses.nt` in `<ontology_name>_exports` directory.
  - Option 2 - to generate all justifications for the subsumptions generated with option 1
  For each of the subsumptions, there can be more justifications. All the justifications are
  exported in OWL format in files named `<ontology_name>_exports/justif-<subsumption_id>-<justification_id>.owl`
  - Option 3 - to generate all explanations by forgetting to all justifications generated with option 2. 
  If option 3 is provided, then another 2 arguments need to be provided, representing:
    - The forgetting method supported by LETHE:
         - Method 1 - `AlchTBoxForgetter`
         - Method 2 - `ShqTBoxForgetter`
         - Method 3 - `AlcOntologyForgetter`
    - The forgetting strategy: 
        - 1 forgetting entities 1-by-1 in random order
        - 2 1-by-1 in increasing order of frequency of occurence in the axioms in the initial justification (less frequent first).
        - 3 1-by-1 in decreasing order of frequency of occurence in the axioms in the initial justification (more frequent first).
        - 4 In batches of the same frequency at once, in increasing order of frequency of occurrence in the axioms in the initial justification (less frequent first)
        - 5 In batches by class expression type, i.e. the symbols occurring only in simple axioms without logical operations or property restrictions first, then symbols occuring in logical operations, then symbols occuring only in more complex axioms with property restrictions.
 The explanations by forgetting are exported in human readable format in files named
 `<ontology_name>_exports/<forgetting_method>/expl<strategy_id>-<subsumption_id>-<justification_id>.owl`  
For example `pizza_exports/AlchForgetter/expl3-1-7.owl` contains explanations using strategy 3 for justification #7 of subsumption #1.

Example usage:

    > java -jar krdl.jar my_path/pizza_super_simple.owl 1
    > java -jar krdl.jar my_path/pizza_super_simple.owl 2
    > java -jar krdl.jar my_path/pizza_super_simple.owl  3 1 2


