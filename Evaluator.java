import java.util.function.*;
import java.util.stream.*;
import java.util.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.io.*;

/**
 * Basic evaluator for automatic evaluation of programming excercise assignments.
 * This evaluator is intended to be used with Moodle and VPL (Virtual Programming Lab).
 * It provides basic capabilities to evaluate programming assignents.
 * 
 * !!! Normally there is no need to touch this file !!!
 * !!! Keep it, unless you are perfectly knowing what you are doing !!!
 * 
 * @author Nane Kratzke
 */
public class Evaluator {

    /**
     * List of Checkstyle checks that are ignored for evaluation.
     * This list is set in the configure method().
     */
    public static final List<String> IGNORE_CHECKS = new LinkedList<String>();

    /**
     * List of file names that shall be considered by checkstyle.
     * This list is set in the configure method().
     */
    public static final List<String> CHECK_FILES = new LinkedList<String>();

    /**
     * Downgrade for every found checkstyle error.
     */
    public static int CHECK_PENALTY = 5;

    class Inspector {

        private Class object;

        public Inspector(String cname) throws ClassNotFoundException { this.object = Class.forName(cname); }

        public String getName() { return object.getSimpleName(); }

        public Stream<Field> fields() { 
            return Stream.of(object.getDeclaredFields()).filter(f -> {
                int m = ((Field)f).getModifiers();
                return !(Modifier.isStatic(m) && Modifier.isFinal(m));
            });
        }
        
        public boolean hasNoFields() { return fields().count() == 0; }

        public Stream<Class> innerClasses() {
            return Stream.of(object.getDeclaredClasses());
        }

        public boolean hasNoInnerClasses() { return innerClasses().count() == 0; }

        public Stream<Field> constants() { 
            return Stream.of(object.getDeclaredFields()).filter(f -> {
                int m = ((Field)f).getModifiers();
                return Modifier.isStatic(m) && Modifier.isFinal(m);
            });
        }

        public boolean hasNo(String... keywords) {
            Path path = Paths.get(object.getSimpleName() + ".java");
            try {
                int i = 0;
                for (String line : Files.readAllLines(path)) {
                    i++;
                    for (String keyword : keywords) {
                        if (line.contains(keyword)) {
                            System.out.println(comment("Line " + i + ": " + line));
                            System.out.println(comment("Line " + i + " in file " + path + " seem to have a not allowed '" + keyword + "' phrase."));
                            
                            // No points if non allowed phrases are found in the submission.
                            // Evaluation is stopped immediately to prevent point injection attacks.
                            System.out.println("Grade :=>> 0");
                            System.exit(1);
                            return false;
                        }
                    }
                }
                return true;    
            } catch (IOException ex) {
                System.out.println(comment("Could not inspect file " + path + " due to exception " + ex.getMessage()));
                return false;
            }
        }

        public boolean hasNoLoops() { return hasNo("while", "for"); }

        public boolean hasNoConstants() { return constants().count() == 0; }

        public Stream<Method> methods() { 
            return Stream.of(object.getDeclaredMethods()).filter(m -> !((Method)m).getName().startsWith("lambda$")); 
        }

        public boolean hasNoMethods() { return methods().count() == 0; }
    }

    /**
     * The maximum points for a VPL assignment.
     */
    private static final int MAX = 100;

    /**
     * The currently reached points for a VPL assignment.
     */
    private int points = 0;

    /**
     * Test case counter.
     * Declared static to count testcases consecutively across
     * different Check classes.
     */
    private static int testcase = 0;

    /**
     * Adds points for grading if a check is passed (wishful behavior).
     * A comment is printed whether the check was successfull or not.
     */
    protected final void grading(int add, String remark, Supplier<Boolean> check) {
        testcase++;
        try {
            if (check.get()) {
                this.points += add;
                System.out.println(comment("Check " + testcase + ": [OK] " + remark + " (" + add + " points)"));
            } else System.out.println(comment("Check " + testcase + ": [FAILED] " + remark + " (0 of " + add + " points)"));
        } catch (Exception ex) {
            System.out.println(comment("Check " + testcase + ": [FAILED due to " + ex + "] " + remark + " (0 of " + add + " points)"));
        }
    }

    /**
     * Deletes points for grading if a check is not passed (unwishful behavior).
     * A comment is printed whether the check was successfull or not.
     */
    protected final void degrading(int del, String remark, Supplier<Boolean> check) {
        testcase++;
        try {
            if (check.get()) 
                System.out.println(comment("Check " + testcase + ": [OK] " + remark + " (no subtraction)"));
            else {
                this.points -= del;
                System.out.println(comment("Check " + testcase + ": [FAILED] " + remark + " (subtracted " + del + " points)"));
            }
        } catch (Exception ex) {
            this.points -= del;
            System.out.println(comment("Check " + testcase + ": [FAILED due to " + ex + "] " + remark + " (subtracted " + del + " points)"));
        }
    }

    protected final <T> boolean assure(String className, Predicate<Inspector> check) {
        try {
            return check.test(new Inspector(className));
        } catch (Exception ex) {
            System.out.println(comment("Check failed due to " + ex));
            System.out.println(comment("This might be due to a syntax error in your submission."));
            return false;
        }
    }

    /**
     * Adds a VPL comment.
     */
    protected final String comment(String c) { return "Comment :=>> " + c; }

    /**
     * This method scans and invokes all methods starting with "test" to run the grading.
     */
    protected final void evaluate() {
        for (Method test : this.getClass().getDeclaredMethods()) {
            if (!test.getName().startsWith("test")) continue;
            try {
                test.invoke(this);
            } catch (Exception ex) {
                System.out.println("Test case " + test.getName() + " failed completely." + ex);
            } finally {
                int report = points;
                report = report > MAX ? MAX : report;
                report = report < 0 ? 0 : report;
                System.out.println("Grade :=>> " + report);
            }
        }
    }

    /**
     * This method evaluates the checkstyle log file.
     */
    protected final void checkstyle() {
        try {
            Scanner in = new Scanner(new File("checkstyle.log"));
            while (in.hasNextLine()) {
                String result = in.nextLine();
                for (String file : Evaluator.CHECK_FILES) {
                    if (!result.contains(file)) continue;
                    if (Evaluator.IGNORE_CHECKS.stream().anyMatch(ignore -> result.contains(ignore))) continue;

                    String msg = result.substring(result.indexOf(file));
                    System.out.println(comment("[CHECKSTYLE]: " + msg));
                    this.points -= Evaluator.CHECK_PENALTY;
                }
            }
            in.close();
            System.out.println(comment("[CHECKSTYLE] All violations: " + this.points + " points"));
        } catch (Exception ex) {
            System.out.println(comment("You are so lucky! We had problems processing the checkstyle.log."));
            System.out.println(comment("This was due to: " + ex));
        }
    }

    /**
     * This method is a hook for the Checks class to configure the evaluation.
     */
    protected void configure() {
        Evaluator.IGNORE_CHECKS.addAll(Arrays.asList(
            "[NewlineAtEndOfFile]", "[HideUtilityClassConstructor]", "[FinalParameters]",
            "[JavadocPackage]", "[AvoidInlineConditionals]"
        ));

        Evaluator.CHECK_FILES.add("Main.java");

        Evaluator.CHECK_PENALTY = 5;
    }

    /**
     * The main method calls the evaluation.
     */
    public static final void main(String[] args) {
        Checks checks = new Checks();
        checks.configure();
        checks.checkstyle();
        checks.evaluate();
        checks.comment("Finished");
    }
}