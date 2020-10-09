# krdl
## Ontology Justification Explanations Using the Forgetting Operation

This project facilitates explanation for subsumption justification in ontologies using the forgetting operation.

The justifications are generated by OWL-API. For each of the justificaiton we apply forgetting using the LETHE library.

The projects experiments with various strategies for forgetting: 

1. Random 1-by-1, in which symbols are forgotten 1 by 1 in random order
2. ...


## Running the tool

The tool needs at least JRE 11 to run.

The main class to run is DLExplanantion. Use the following command line options:
1. The ontology file (required)
2. Either of the options 1, 2 or 3:
  - Option 1 - to generate all the direct and inferred subsumptions
  - Option 2 - to generate all justifications for the subsumptions generated with option 1
  - Option 3 - to generate all explanations by forgetting to all justifications generated with option 2. If option 3 is provided, then another argument needs to be provided, representing the forgettting method supported by LETHE:
    - Method 1 - AlchTBoxForgetter
    - Method 2 - ShqTBoxForgetter
    - Method 3 - AlcOntologyForgetter




