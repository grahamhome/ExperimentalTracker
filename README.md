# Multiple Identity Tracker

This Java program is intended for use in Multiple Identity Tracking (MIT) experiments.
An experimenter can fill out a configuration file which specifies many variables of the experiment
including the appearance of the background image, the number, size, color, shape, speed and paths of the 
moving objects, the position of any static objects, and the timing and content of screen masks and pop-up
queries. When a participant completes the experiment, a results file is written which contains information on 
their interactions with the experiment. A sample configuration called Demo Config is included in this repo, with 
comments on each line to explain the configuration file schema.

This program was initially created for Dr. Esa Rantanen by Graham Home at Rochester Institute of Technology.

## Running the Program

To run the program, simply download and double-click the included "Tracker.jar" file.

## Modifying the Experiment Parameters

If you are a researcher who wishes to create your own experiment, it is recommended to first make a copy of 
the included "Demo Config" folder. Then, you can overwrite the lines in the demo config file with your own 
configuration lines, specifying whatever parameters you choose. The comments in the config file should explain 
what each parameter does.

## Modifying the Program Itself

If you are a developer who needs to add new features to this program, follow the guide below.

### Development Environment

#### Java 8

First, you will need to have Java 8 installed on your local machine. This program requires Java 8 because it
makes use of the JavaFX library, which was introduced in Java 8, to display content on the screen.

#### Eclipse / IntelliJ / Other Java IDE

Next, you will need to have a Java IDE such as Eclipse or IntelliJ installed on your machine. Make sure it is configured
to use the Java 8 runtime, especially if you have any other Java versions installed on your computer!

### Now the Fun Part

At this point, you are ready to begin modifying the program itself. This is a fairly complex program, and a lot of experimentation
and testing has been performed to bring it to its current state. Therefore, it is highly reccomended that you follow the process below
before making any changes:

#### Read the Comments

Open up each file in the project with your IDE. Look at the top-level comments so you understand what each class does.

#### Understand the New Requirements

What changes are you making? It's probably a good idea to write them down. Make sure you understand the full scope and ramifications of these
changes before contining. Ask as many questions of the stakeholder (most likely Dr. Rantanen or a psych student) as you need to ensure you fully 
understand their needs.

#### Understand the Impact

What parts of the current program will need to be changed in order to add your new feature? There may be more than you think.
For example, if you have been asked to add a new type of query, that will require changes to the following classes:

1. ConfigImporter will need new logic to allow it to read and validate the configuration lines for the new query type.

2. ExperimentModel will need a new sub-class to represent the new query type.

3. A new GraphicalQuery* class will need to be written to represent the new query type visually and handle the user's interaction
with it.

4. ReportWriter will need a new method to log the new query type's appearance, interaction and disappearance in the report file.

Make sure you understand how your new feature will affect each of the classes in the project before you begin to modify any of them.

#### Follow Existing Patterns

It is generally a good idea to write new code which looks like the existing code in each of the classes you are modifying. Therefore, 
it is recommended that you identify a similar, existing feature to the one you are tasked with implementing, and find the code which 
belongs to that feature in each class. Then, you can write your new feature in the same way as the old feature, taking advantage of 
existing data structures and class hierarchies wherever possible. This will significantly reduce the amount of work you have to do and 
produce less code duplication.

#### Commit Early, Commit Often

Frequent commits with descriptive messages will help you manage your changes and revert any changes which cause unwanted behavior in the program.

#### Export A JAR

When you've written your changes and thoroughly tested the program with a couple of configuration files, don't forget to export it as a runnable JAR 
so your stakeholders can use it.

### Final Tips

#### GitHub

It is a good idea to go ahead and fork this project into your own GitHub repository before beginning to work on it. That way, the original repo 
will still be here should you need to revert back to the original version for any reason.

#### Debugging

In order to run this program in debug mode, you will need to temporarily disable fullscreen mode. At least with Eclipse, debugging in fullscreen mode 
does not work. To do this, simply comment out Line 58 of the TrackingActivity class. Don't forget to un-comment it when you're done!

#### Phone A Friend

I am more than happy to answer any questions you may have about this codebase and provide advice on how to proceed with any changes. Please don't 
hesitate to contact me via email at gmh5970(at)g.rit.edu. Just put "MIT Program" somewhere in the subject line and my email filter will make sure I 
see it. I'll do my best to respond to you within a few days. 

Good luck, and happy coding! 

Sincerely,
Graham Home