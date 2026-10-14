FasalAI -- Java version
Group 8 -- AI-Based Crop Recommendation for Farmers

WHAT THIS IS
------------
A Java port of the FasalAI crop-recommendation model and app. CropModel.java
contains the exact same trained Random Forest (40 decision trees, 99.5% test
accuracy) used by the website -- the trees were exported directly from the
Python/scikit-learn model and ported here, not retrained or approximated.
FasalAIApp.java is a Swing desktop app built on top of it, with a custom
rounded-card UI (UIKit.java) and a "Why this crop?" panel that plots each
input against that crop's own typical range in the training data.

No external libraries, no Maven/Gradle, no internet connection needed. Just
a JDK (8 or newer).

HOW TO RUN THE APP
-------------------
    javac CropModel.java CropInfo.java UIKit.java FasalAIApp.java
    java FasalAIApp

A window opens with input fields for soil nutrients (N, P, K), pH, and
climate (temperature, humidity, rainfall). Pick a crop from "Load an
example" or type your own values, then click "Get Recommendation".
Click "Why this crop?" to see how your inputs compare to that crop's
typical range in the training data.

HOW TO PROVE THE MODEL IS REAL
--------------------------------
Two command-line checks are included:

    javac CropModel.java CropInfo.java TestAccuracy.java
    java TestAccuracy
        Loads test_rows.csv (440 real rows held out from training, never
        seen by the model) and reports how many the model gets right.

    javac CropModel.java CropInfo.java TestExamples.java
    java TestExamples
        Runs every crop's own typical reading back through the model and
        confirms it recovers the correct crop, with a confidence score,
        for all 22 crops.

FILES
-----
CropModel.java    The trained Random Forest + the predict() logic.
                   TREE_DATA inside it is an exported dump of the real
                   decision trees, not hand-written rules.
CropInfo.java      Display names, categories, one-line facts, each crop's
                   example reading, and its typical 10th-90th percentile
                   range per feature (for the "Why this crop?" panel).
UIKit.java         Small self-contained custom-painted Swing components
                   (rounded cards, pill buttons, gradient bars, the range
                   bar, a drawn app icon) -- purely visual, AWT/Swing only.
FasalAIApp.java    The Swing desktop app (entry point: main()).
TestAccuracy.java  Validates against 440 held-out test rows.
TestExamples.java  Round-trip check across all 22 crops.
test_rows.csv      The held-out test data used by TestAccuracy.java.
